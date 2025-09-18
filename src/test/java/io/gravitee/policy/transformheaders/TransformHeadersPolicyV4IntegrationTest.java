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
package io.gravitee.policy.transformheaders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.vertx.core.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.gravitee.policy.transformheaders.v3.TransformHeadersPolicyV3;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransformHeadersPolicyV4IntegrationTest extends AbstractPolicyTest<TransformHeadersPolicyV3, TransformHeadersPolicyConfiguration> {

    private MessageStorage messageStorage;

    @BeforeEach
    void setUp() {
        messageStorage = getBean(MessageStorage.class);
    }

    @AfterEach
    void tearDown() {
        messageStorage.reset();
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/add-update-whitelist-remove-headers-v4-proxy.json")
    void should_add_update_and_remove_headers_with_proxy_api(HttpClient client) throws InterruptedException {
        wiremock.stubFor(
            get("/endpoint").willReturn(
                ok()
                    .withHeader("toupdatekeyresponse", "responseToUpdate")
                    .withHeader("toremovekeyresponse", "willBeRemoved")
                    .withHeader("whitelistedkeyresponse", "whitelisted")
                    .withHeader("notinwhitelistkeyresponse1", "excluded")
                    .withHeader("notinwhitelistkeyresponse2", "excluded")
            )
        );

        final TestObserver<HttpClientResponse> obs = client
            .request(HttpMethod.GET, "/test")
            .flatMap(request ->
                request
                    .putHeader("toupdatekey", "firstValue")
                    .putHeader("toremovekey", "willBeRemoved")
                    .putHeader("whitelistedkey", "whitelisted")
                    .putHeader("notinwhitelistkey1", "excluded")
                    .putHeader("notinwhitelistkey2", "excluded")
                    .rxSend()
            )
            .test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("headerKeyResponse")).isEqualTo("headerValue");
                assertThat(response.headers().get("toUpdateKeyResponse")).isEqualTo("updatedValue");
                assertThat(response.headers().get("whitelistedKeyResponse")).isEqualTo("whitelisted");
                assertThat(response.headers().contains("toRemoveKeyResponse")).isFalse();
                assertThat(response.headers().contains("notInWhitelistKeyResponse1")).isFalse();
                assertThat(response.headers().contains("notInWhitelistKeyResponse2")).isFalse();
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint"))
                .withHeader("headerkey", equalTo("headerValue"))
                .withHeader("toupdatekey", equalTo("updatedValue"))
                .withHeader("whitelistedkey", equalTo("whitelisted"))
                .withoutHeader("toremovekey")
                .withoutHeader("notinwhitelistkey1")
                .withoutHeader("notinwhitelistkey2")
        );
    }

    @Test
    @DeployApi("/apis/append-headers-v4-proxy.json")
    void should_append_headers_with_proxy_api(HttpClient client) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok().withHeader("headerKeyResponse", "headerValue0")));

        final TestObserver<HttpClientResponse> obs = client.request(HttpMethod.GET, "/test").flatMap(HttpClientRequest::rxSend).test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().getAll("headerKeyResponse")).contains("headerValue0", "headerValue1", "headerValue2");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint"))
                .withHeader("headerkey", containing("headerValue1"))
                .withHeader("headerkey", containing("headerValue2"))
        );
    }

    @Test
    @DeployApi("/apis/add-update-whitelist-remove-headers-v4-message-publish.json")
    void should_add_update_and_remove_headers_on_request_message(HttpClient httpClient) throws InterruptedException {
        httpClient
            .rxRequest(POST, "/test")
            .flatMap(request ->
                request
                    .putHeader("toupdatekey", "firstValue")
                    .putHeader("toremovekey", "willBeRemoved")
                    .putHeader("whitelistedkey", "whitelisted")
                    .putHeader("notinwhitelistkey1", "excluded")
                    .putHeader("notinwhitelistkey2", "excluded")
                    .rxSend("message")
            )
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS);

        messageStorage
            .subject()
            .test()
            .assertValue(message -> {
                assertThat(message.headers().get("headerKey")).isEqualTo("headerValue");
                assertThat(message.headers().get("toUpdateKey")).isEqualTo("updatedValue");
                assertThat(message.headers().get("whitelistedKey")).isEqualTo("whitelisted");
                assertThat(message.headers().contains("toRemoveKey")).isFalse();
                assertThat(message.headers().contains("notInWhitelistKey1")).isFalse();
                assertThat(message.headers().contains("notInWhitelistKey2")).isFalse();
                return true;
            })
            .dispose();
    }

    @Test
    @DeployApi("/apis/append-headers-v4-message-publish.json")
    void should_append_headers_on_request_message(HttpClient httpClient) throws InterruptedException {
        httpClient
            .rxRequest(POST, "/test")
            .flatMap(request -> request.putHeader("headerKey", "headerValue0").rxSend("message"))
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS);

        messageStorage
            .subject()
            .test()
            .assertValue(message -> {
                assertThat(message.headers().getAll("headerKey")).contains("headerValue0", "headerValue1", "headerValue2");
                return true;
            })
            .dispose();
    }

    @Test
    @DeployApi("/apis/add-update-whitelist-remove-headers-v4-message-subscribe.json")
    void should_add_update_and_remove_headers_on_response_message(HttpClient httpClient) throws InterruptedException {
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                return request.rxSend();
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .filter(buffer -> !buffer.toString().startsWith("retry:") && !buffer.toString().startsWith(":"))
            .test()
            .awaitCount(1)
            .assertValueAt(0, chunk -> {
                final String[] splitMessage = chunk.toString().split("\n");
                assertThat(splitMessage).hasSize(6);
                assertThat(splitMessage[0]).isEqualTo("id: 0");
                assertThat(splitMessage[1]).isEqualTo("event: message");
                assertThat(splitMessage[2]).isEqualTo("data: { \"message\": \"hello\" }");
                assertThat(splitMessage[3]).isEqualTo(":whitelistedKeyResponse: whitelisted");
                assertThat(splitMessage[4]).isEqualTo(":headerKeyResponse: headerValue");
                assertThat(splitMessage[5]).isEqualTo(":toUpdateKeyResponse: updatedValue");
                return true;
            });
    }

    @Test
    @DeployApi("/apis/append-headers-v4-message-subscribe.json")
    void should_append_headers_on_response_message(HttpClient httpClient) throws InterruptedException {
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                return request.rxSend();
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .filter(buffer -> !buffer.toString().startsWith("retry:") && !buffer.toString().startsWith(":"))
            .test()
            .awaitCount(1)
            .assertValueAt(0, chunk -> {
                final String[] splitMessage = chunk.toString().split("\n");
                assertThat(splitMessage).hasSize(4);
                assertThat(splitMessage[0]).isEqualTo("id: 0");
                assertThat(splitMessage[1]).isEqualTo("event: message");
                assertThat(splitMessage[2]).isEqualTo("data: { \"message\": \"hello\" }");
                assertThat(splitMessage[3]).isEqualTo(":headerKeyResponse: headerValue0,headerValue1,headerValue2");
                return true;
            });
    }
}
