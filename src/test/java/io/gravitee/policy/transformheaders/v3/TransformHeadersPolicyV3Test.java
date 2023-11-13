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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.transformheaders.configuration.HttpHeader;
import io.gravitee.policy.transformheaders.configuration.PolicyScope;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TransformHeadersPolicyV3Test {

    private TransformHeadersPolicyV3 transformHeadersPolicy;

    @Mock
    private TransformHeadersPolicyConfiguration transformHeadersPolicyConfiguration;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Request request;

    private final HttpHeaders requestHttpHeaders = HttpHeaders.create();
    private HttpHeaders responseHttpHeaders = HttpHeaders.create();

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @BeforeEach
    public void init() {
        transformHeadersPolicy = new TransformHeadersPolicyV3(transformHeadersPolicyConfiguration);
        lenient().when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        lenient().when(request.headers()).thenReturn(requestHttpHeaders);
        lenient().when(response.headers()).thenReturn(responseHttpHeaders);
        lenient().when(templateEngine.convert(any(String.class))).thenAnswer(returnsFirstArg());
    }

    @Test
    void test_OnRequest_noTransformation() {
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnResponse_noTransformation() {
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnRequest_invalidScope() {
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        verify(transformHeadersPolicyConfiguration, never()).getAddHeaders();
        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnResponse_invalidScope() {
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        verify(transformHeadersPolicyConfiguration, never()).getAddHeaders();
        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnRequest_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnResponse_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnResponse_addMultipleHeaders() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Arrays.asList(new HttpHeader("X-Gravitee-Header1", "Header1"), new HttpHeader("X-Gravitee-Header2", "Header2")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Header1")).isEqualTo("Header1");
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Header2")).isEqualTo("Header2");
    }

    @Test
    void test_OnRequest_addHeader_nullValue() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_addHeader_nullValue() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnRequest_addHeader_nullName() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(new HttpHeader(null, "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_addHeader_nullName() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(new HttpHeader(null, "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnRequest_updateHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnResponse_updateHeader() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnRequest_removeHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_removeHeader() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_removeHeaderNull() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(null));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Initial");
    }

    @Test
    void test_OnResponse_removeHeaderAndWhiteList() {
        // Prepare
        responseHttpHeaders.set("x-gravitee-toremove", "Initial");
        responseHttpHeaders.set("x-gravitee-white", "Initial");
        responseHttpHeaders.set("x-gravitee-black", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(transformHeadersPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-ToRemove"));
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-Gravitee-White"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-ToRemove")).isNull();
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Black")).isNull();
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-White")).isNotNull();
    }

    @Test
    void test_OnResponse_doNothing() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isNotNull();
    }

    @Test
    void test_OnRequest_doNothing() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNotNull();
    }

    @Test
    void test_OnResponse_whitelistHeader() {
        // Prepare
        responseHttpHeaders.set("x-walter", "Initial");
        responseHttpHeaders.set("x-white", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Walter")).isNull();
        assertThat(responseHttpHeaders.getFirst("X-White")).isNotNull();
    }

    @Test
    void test_OnRequest_whitelistHeader() {
        // Prepare
        requestHttpHeaders.set("x-walter", "Initial");
        requestHttpHeaders.set("x-white", "Initial");
        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(transformHeadersPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Walter")).isNull();
        assertThat(requestHttpHeaders.getFirst("X-White")).isNotNull();
    }

    @Test
    void test_OnRequestContent_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Product-Id", "{#jsonPath(#request.content, '$.product.id')}")));

        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST_CONTENT);
        when(executionContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        when(executionContext.request()).thenReturn(request);

        // Run
        new TransformHeadersPolicyV3(transformHeadersPolicyConfiguration)
            .onRequestContent(executionContext)
            .write(Buffer.buffer("{\n" + "  \"product\": {\n" + "    \"id\": \"1234\"\n" + "  }\n" + "}"))
            .end();

        // Verify
        assertThat(requestHttpHeaders.getFirst("X-Product-Id")).isNotNull().isEqualTo("1234");
    }

    @Test
    void test_OnResponseContent_addHeader() {
        // Prepare
        when(transformHeadersPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Product-Id", "{#jsonPath(#response.content, '$.product.id')}")));

        when(transformHeadersPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE_CONTENT);
        when(executionContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        when(executionContext.response()).thenReturn(response);

        // Run
        new TransformHeadersPolicyV3(transformHeadersPolicyConfiguration)
            .onResponseContent(executionContext)
            .write(Buffer.buffer("{\n" + "  \"product\": {\n" + "    \"id\": \"1234\"\n" + "  }\n" + "}"))
            .end();

        // Verify
        assertThat(responseHttpHeaders.getFirst("X-Product-Id")).isNotNull().isEqualTo("1234");
    }
}
