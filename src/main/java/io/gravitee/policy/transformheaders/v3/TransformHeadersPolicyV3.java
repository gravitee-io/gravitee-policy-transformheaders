/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.transformheaders.v3;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_API;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.api.el.EvaluableResponse;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.policy.transformheaders.configuration.PolicyScope;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class TransformHeadersPolicyV3 {

    protected static final String ERROR_MESSAGE_FORMAT = "[api-id:%s] [request-id:%s] [request-path:%s] %s";
    private static final String REQUEST_TEMPLATE_VARIABLE = "request";
    private static final String RESPONSE_TEMPLATE_VARIABLE = "response";

    /**
     * Transform headers configuration
     */
    protected final TransformHeadersPolicyConfiguration configuration;

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (configuration.getScope() == null || configuration.getScope() == PolicyScope.REQUEST) {
            // Do transform
            transform(request.headers(), executionContext);
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (configuration.getScope() == PolicyScope.RESPONSE) {
            // Do transform
            transform(response.headers(), executionContext);
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }

    @OnRequestContent
    public ReadWriteStream<Buffer> onRequestContent(ExecutionContext executionContext) {
        if (configuration.getScope() == PolicyScope.REQUEST_CONTENT) {
            return createStream(PolicyScope.REQUEST_CONTENT, executionContext);
        }

        return null;
    }

    @OnResponseContent
    public ReadWriteStream<Buffer> onResponseContent(ExecutionContext executionContext) {
        if (configuration.getScope() == PolicyScope.RESPONSE_CONTENT) {
            return createStream(PolicyScope.RESPONSE_CONTENT, executionContext);
        }

        return null;
    }

    private ReadWriteStream<Buffer> createStream(PolicyScope scope, ExecutionContext context) {
        return new BufferedReadWriteStream() {
            Buffer buffer = Buffer.buffer();

            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                buffer.appendBuffer(content);
                return this;
            }

            @Override
            public void end() {
                initRequestResponseProperties(
                    context,
                    (scope == PolicyScope.REQUEST_CONTENT) ? buffer.toString() : null,
                    (scope == PolicyScope.RESPONSE_CONTENT) ? buffer.toString() : null
                );

                if (scope == PolicyScope.REQUEST_CONTENT) {
                    transform(context.request().headers(), context);
                } else {
                    transform(context.response().headers(), context);
                }

                if (buffer.length() > 0) {
                    super.write(buffer);
                }
                super.end();
            }
        };
    }

    private void initRequestResponseProperties(ExecutionContext context, String requestContent, String responseContent) {
        context
            .getTemplateEngine()
            .getTemplateContext()
            .setVariable(REQUEST_TEMPLATE_VARIABLE, new EvaluableRequest(context.request(), requestContent));

        context
            .getTemplateEngine()
            .getTemplateContext()
            .setVariable(RESPONSE_TEMPLATE_VARIABLE, new EvaluableResponse(context.response(), responseContent));
    }

    void transform(HttpHeaders httpHeaders, ExecutionContext executionContext) {
        // Add or update response headers
        if (configuration.getAddHeaders() != null) {
            configuration
                .getAddHeaders()
                .forEach(header -> {
                    if (header.getName() != null && !header.getName().trim().isEmpty()) {
                        try {
                            String extValue = (header.getValue() != null)
                                ? executionContext.getTemplateEngine().convert(header.getValue())
                                : null;
                            if (extValue != null) {
                                httpHeaders.set(header.getName(), extValue);
                            }
                        } catch (Exception ex) {
                            MDC.put("api", String.valueOf(executionContext.getAttribute(ATTR_API)));
                            log.error(
                                String.format(
                                    ERROR_MESSAGE_FORMAT,
                                    executionContext.getAttribute(ATTR_API),
                                    executionContext.request().id(),
                                    executionContext.request().path(),
                                    ex.getMessage()
                                ),
                                ex.getCause()
                            );
                            MDC.remove("api");
                        }
                    }
                });
        }

        // verify the whitelist
        List<String> headersToRemove = configuration.getRemoveHeaders() == null
            ? new ArrayList<>()
            : new ArrayList<>(configuration.getRemoveHeaders());

        if (httpHeaders != null && configuration.getWhitelistHeaders() != null && !configuration.getWhitelistHeaders().isEmpty()) {
            httpHeaders
                .names()
                .forEach(headerName -> {
                    if (configuration.getWhitelistHeaders().stream().noneMatch(headerName::equalsIgnoreCase)) {
                        headersToRemove.add(headerName);
                    }
                });
        }

        // Remove request headers
        headersToRemove.forEach(headerName -> {
            if (headerName != null && !headerName.trim().isEmpty()) {
                httpHeaders.remove(headerName);
            }
        });
    }
}
