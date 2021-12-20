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

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.transformheaders.configuration.HttpHeader;
import io.gravitee.policy.transformheaders.configuration.PolicyScope;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
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

    private HttpHeaders requestHttpHeaders = HttpHeaders.create(),
            responseHtpHeaders = HttpHeaders.create();

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @Before
    public void init() {
        initMocks(this);

        transformHeadersPolicy = new TransformHeadersPolicy(transformHeadersPolicyConfiguration);
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
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
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(
                new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals("Value", responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_addMultipleHeaders() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Arrays.asList(
                new HttpHeader("X-Gravitee-Header1", "Header1"), new HttpHeader("X-Gravitee-Header2", "Header2")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals("Header1", responseHtpHeaders.getFirst("X-Gravitee-Header1"));
        assertEquals("Header2", responseHtpHeaders.getFirst("X-Gravitee-Header2"));
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
        assertNull(requestHttpHeaders.getFirst("X-Gravitee-Test"));
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
        assertNull(responseHtpHeaders.getFirst("X-Gravitee-Test"));
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
        assertNull(requestHttpHeaders.getFirst("X-Gravitee-Test"));
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
        assertNull(requestHttpHeaders.getFirst("X-Gravitee-Test"));
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
        responseHtpHeaders.set("X-Gravitee-Test", "Initial");
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
        assertNull(requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_removeHeader() {
        // Prepare
        responseHtpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(
                "X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_removeHeaderAndWhiteList() {
        // Prepare
        responseHtpHeaders.set("X-Gravitee-ToRemove", "Initial");
        responseHtpHeaders.set("X-Gravitee-White", "Initial");
        responseHtpHeaders.set("X-Gravitee-Black", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(
                "X-Gravitee-ToRemove"));
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList(
                "X-Gravitee-White"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(responseHtpHeaders.getFirst("X-Gravitee-ToRemove"));
        assertNull(responseHtpHeaders.getFirst("X-Gravitee-Black"));
        assertNotNull(responseHtpHeaders.getFirst("X-Gravitee-White"));
    }

    @Test
    public void testOnResponse_doNothing() {
        // Prepare
        responseHtpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNotNull(responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnRequest_doNothing() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNotNull(requestHttpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_whitelistHeader() {
        // Prepare
        responseHtpHeaders.set("X-Walter", "Initial");
        responseHtpHeaders.set("X-White", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(responseHtpHeaders.getFirst("X-Walter"));
        assertNotNull(responseHtpHeaders.getFirst("X-White"));
    }

    @Test
    public void testOnRequest_whitelistHeader() {
        // Prepare
        requestHttpHeaders.set("X-Walter", "Initial");
        requestHttpHeaders.set("X-White", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(requestHttpHeaders.getFirst("X-Walter"));
        assertNotNull(requestHttpHeaders.getFirst("X-White"));
    }
}
