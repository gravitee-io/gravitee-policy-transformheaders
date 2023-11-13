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

import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.transformheaders.configuration.HttpHeader;
import io.gravitee.policy.transformheaders.configuration.PolicyScope;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

    private HttpHeaders requestHttpHeaders = HttpHeaders.create(), responseHtpHeaders = HttpHeaders.create();

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Arrays.asList(new HttpHeader("X-Gravitee-Header1", "Header1"), new HttpHeader("X-Gravitee-Header2", "Header2")));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", null)));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnRequest_addHeader_nullName() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(new HttpHeader(null, "Value")));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(new HttpHeader(null, "Value")));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

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
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

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
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-Test"));

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
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(responseHtpHeaders.getFirst("X-Gravitee-Test"));
    }

    @Test
    public void testOnResponse_removeHeaderNull() {
        // Prepare
        responseHtpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(null));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertEquals(responseHtpHeaders.getFirst("X-Gravitee-Test"), "Initial");
    }

    @Test
    public void testOnResponse_removeHeaderAndWhiteList() {
        // Prepare
        responseHtpHeaders.set("x-gravitee-toremove", "Initial");
        responseHtpHeaders.set("x-gravitee-white", "Initial");
        responseHtpHeaders.set("x-gravitee-black", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-ToRemove"));
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-Gravitee-White"));

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
        responseHtpHeaders.set("x-walter", "Initial");
        responseHtpHeaders.set("x-white", "Initial");
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
        requestHttpHeaders.set("x-walter", "Initial");
        requestHttpHeaders.set("x-white", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertNull(requestHttpHeaders.getFirst("X-Walter"));
        assertNotNull(requestHttpHeaders.getFirst("X-White"));
    }

    @Test
    public void testOnRequestContent_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Product-Id", "{#jsonPath(#request.content, '$.product.id')}")));

        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST_CONTENT);
        when(executionContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        when(executionContext.request()).thenReturn(request);

        // Run
        new TransformHeadersPolicy(transformHeadersPolicyConfiguration)
            .onRequestContent(executionContext)
            .write(Buffer.buffer("{\n" + "  \"product\": {\n" + "    \"id\": \"1234\"\n" + "  }\n" + "}"))
            .end();

        // Verify
        assertNotNull(requestHttpHeaders.getFirst("X-Product-Id"));
        assertEquals("1234", requestHttpHeaders.getFirst("X-Product-Id"));
    }

    @Test
    public void testOnResponseContent_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Product-Id", "{#jsonPath(#response.content, '$.product.id')}")));

        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE_CONTENT);
        when(executionContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        when(executionContext.response()).thenReturn(response);

        // Run
        new TransformHeadersPolicy(transformHeadersPolicyConfiguration)
            .onResponseContent(executionContext)
            .write(Buffer.buffer("{\n" + "  \"product\": {\n" + "    \"id\": \"1234\"\n" + "  }\n" + "}"))
            .end();

        // Verify
        assertNotNull(responseHtpHeaders.getFirst("X-Product-Id"));
        assertEquals("1234", responseHtpHeaders.getFirst("X-Product-Id"));
    }
}
