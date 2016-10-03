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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
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
        // Add or update request headers
        if (transformHeadersPolicyConfiguration.getAddHeaders() != null) {
            transformHeadersPolicyConfiguration.getAddHeaders().forEach(
                    header -> {
                        if (header.getName() != null && ! header.getName().trim().isEmpty()) {
                            try {
                                String extValue = (header.getValue() != null) ?
                                        executionContext.getTemplateEngine().convert(header.getValue()) : null;
                                request.headers().set(header.getName(), extValue);
                            } catch (Exception ex) {
                                // Do nothing
                            }
                        }
                    });
        }

        // Remove request headers
        if (transformHeadersPolicyConfiguration.getRemoveHeaders() != null) {
            transformHeadersPolicyConfiguration.getRemoveHeaders()
                    .forEach(headerName -> {
                        if (headerName != null && ! headerName.trim().isEmpty()) {
                            request.headers().remove(headerName);
                        }
                    });
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }
}
