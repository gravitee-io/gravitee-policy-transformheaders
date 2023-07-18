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
package io.gravitee.policy.transformheaders.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformHeadersPolicyConfigurationTest {

    @Test
    public void test_transformHeaders01() throws IOException {
        TransformHeadersPolicyConfiguration configuration = load(
            "/io/gravitee/policy/transformheaders/configuration/transformheaders01.json",
            TransformHeadersPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.REQUEST);
        assertThat(configuration.getAddHeaders()).isNotNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNull();

        assertThat(configuration.getAddHeaders()).hasSize(1);
        assertThat(configuration.getRemoveHeaders()).hasSize(1);
    }

    @Test
    public void test_transformHeaders02() throws IOException {
        TransformHeadersPolicyConfiguration configuration = load(
            "/io/gravitee/policy/transformheaders/configuration/transformheaders02.json",
            TransformHeadersPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.RESPONSE);
        assertThat(configuration.getAddHeaders()).isNotNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNull();

        assertThat(configuration.getAddHeaders()).hasSize(1);
        assertThat(configuration.getRemoveHeaders()).hasSize(1);
    }

    @Test
    public void test_transformHeaders03() throws IOException {
        TransformHeadersPolicyConfiguration configuration = load(
            "/io/gravitee/policy/transformheaders/configuration/transformheaders03.json",
            TransformHeadersPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.RESPONSE);
        assertThat(configuration.getAddHeaders()).isNotNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNull();

        assertThat(configuration.getAddHeaders()).hasSize(2);
        assertThat(configuration.getRemoveHeaders()).hasSize(1);
    }

    @Test
    public void test_transformHeaders04() throws IOException {
        TransformHeadersPolicyConfiguration configuration = load(
            "/io/gravitee/policy/transformheaders/configuration/transformheaders04.json",
            TransformHeadersPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.RESPONSE);
        assertThat(configuration.getAddHeaders()).isNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNotNull();

        assertThat(configuration.getRemoveHeaders()).hasSize(1);
        assertThat(configuration.getWhitelistHeaders()).hasSize(2);
    }

    private <T> T load(String resource, Class<T> type) throws IOException {
        URL jsonFile = this.getClass().getResource(resource);
        return new ObjectMapper().readValue(jsonFile, type);
    }
}
