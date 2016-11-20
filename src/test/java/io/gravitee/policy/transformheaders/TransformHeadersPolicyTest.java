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
import io.gravitee.gateway.api.expression.TemplateEngine;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.transformheaders.configuration.HttpHeader;
import io.gravitee.policy.transformheaders.configuration.PolicyScope;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.gravitee.reporter.api.http.RequestMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TransformHeadersPolicyTest {

    private TransformHeadersPolicy transformHeadersPolicy;

    @Mock
    private TransformHeadersPolicyConfiguration transformHeadersPolicyConfiguration;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Request request;

    private HttpHeaders requestHttpHeaders = new HttpHeaders(),
            responseHtpHeaders = new HttpHeaders();

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @Before
    public void init() {
        initMocks(this);

        transformHeadersPolicy = new TransformHeadersPolicy(transformHeadersPolicyConfiguration);
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(request.metrics()).thenReturn(RequestMetrics.on(System.currentTimeMillis()).build());
        when(request.headers()).thenReturn(requestHttpHeaders);
        when(response.headers()).thenReturn(responseHtpHeaders);
        when(templateEngine.convert(any(String.class))).thenAnswer(returnsFirstArg());
    }

    @Test
    public void testOnRequest_noTransformation() {
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnResponse_noTransformation() {
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_invalidScope() {
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        verify(transformHeadersPolicyConfiguration, never()).getAddHeaders();
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnResponse_invalidScope() {
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        verify(transformHeadersPolicyConfiguration, never()).getAddHeaders();
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals("Value", requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals("Value", responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnRequest_addHeader_nullValue() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(null, requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_addHeader_nullValue() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(null, responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnRequest_addHeader_nullName() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader(null, "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(null, requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_addHeader_nullName() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader(null, "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(null, requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnRequest_updateHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals("Value", requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_updateHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals("Value", responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnRequest_removeHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(
                "X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(null, requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_removeHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(
                "X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(null, responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }
}
