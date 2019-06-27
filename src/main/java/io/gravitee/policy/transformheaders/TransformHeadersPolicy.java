/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.transformheaders;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.transformheaders.configuration.PolicyScope;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformHeadersPolicy {

    /**
     * Transform headers configuration
     */
    private final TransformHeadersPolicyConfiguration transformHeadersPolicyConfiguration;

    public TransformHeadersPolicy(final TransformHeadersPolicyConfiguration transformHeadersPolicyConfiguration) {
        this.transformHeadersPolicyConfiguration = transformHeadersPolicyConfiguration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (transformHeadersPolicyConfiguration.getScope() == null || transformHeadersPolicyConfiguration.getScope() == PolicyScope.REQUEST) {
            // Do transform
            transform(request.headers(), executionContext);
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (transformHeadersPolicyConfiguration.getScope() == PolicyScope.RESPONSE) {
            // Do transform
            transform(response.headers(), executionContext);
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }

    void transform(HttpHeaders httpHeaders, ExecutionContext executionContext) {
        // Add or update response headers
        if (transformHeadersPolicyConfiguration.getAddHeaders() != null) {
            transformHeadersPolicyConfiguration.getAddHeaders().forEach(
                    header -> {
                        if (header.getName() != null && !header.getName().trim().isEmpty()) {
                            try {
                                String extValue = (header.getValue() != null) ?
                                        executionContext.getTemplateEngine().convert(header.getValue()) : null;
                                if (extValue != null) {
                                    httpHeaders.set(header.getName(), extValue);
                                }
                            } catch (Exception ex) {
                                // Do nothing
                                ex.printStackTrace();
                            }
                        }
                    });
        }

        // verify the whitelist
        List<String> headersToRemove = transformHeadersPolicyConfiguration.getRemoveHeaders() == null
                ? new ArrayList<>()
                : new ArrayList<>(transformHeadersPolicyConfiguration.getRemoveHeaders());

        if (httpHeaders != null && transformHeadersPolicyConfiguration.getWhitelistHeaders() != null
                && !transformHeadersPolicyConfiguration.getWhitelistHeaders().isEmpty()) {
            httpHeaders.keySet().forEach(headerName -> {
                if (!transformHeadersPolicyConfiguration.getWhitelistHeaders().contains(headerName)) {
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
