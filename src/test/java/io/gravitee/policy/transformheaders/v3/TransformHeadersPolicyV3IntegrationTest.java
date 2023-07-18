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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest(v2ExecutionMode = ExecutionMode.V3)
@DeployApi("/apis/add-update-whitelist-remove-headers.json")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransformHeadersPolicyV3IntegrationTest extends AbstractPolicyTest<TransformHeadersPolicyV3, TransformHeadersPolicyConfiguration> {

    @Test
    @DisplayName("Should add, update, whitelist and remove headers")
    void shouldAddUpdateAndRemoveHeaders(HttpClient client) throws InterruptedException {
        wiremock.stubFor(
            get("/endpoint")
                .willReturn(
                    ok()
                        .withHeader("toUpdateKeyResponse", "responseToUpdate")
                        .withHeader("toRemoveKeyResponse", "willBeRemoved")
                        .withHeader("whitelistedKeyResponse", "whitelisted")
                        .withHeader("notInWhitelistKeyResponse1", "excluded")
                        .withHeader("notInWhitelistKeyResponse2", "excluded")
                )
        );

        final TestObserver<HttpClientResponse> obs = client
            .request(HttpMethod.GET, "/test")
            .flatMap(request ->
                request
                    .putHeader("toUpdateKey", "firstValue")
                    .putHeader("toRemoveKey", "willBeRemoved")
                    .putHeader("whitelistedKey", "whitelisted")
                    .putHeader("notInWhitelistKey1", "excluded")
                    .putHeader("notInWhitelistKey2", "excluded")
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
                .withHeader("headerKey", equalTo("headerValue"))
                .withHeader("toUpdateKey", equalTo("updatedValue"))
                .withHeader("whitelistedKey", equalTo("whitelisted"))
                .withoutHeader("toRemoveKey")
                .withoutHeader("notInWhitelistKey1")
                .withoutHeader("notInWhitelistKey2")
        );
    }
}
