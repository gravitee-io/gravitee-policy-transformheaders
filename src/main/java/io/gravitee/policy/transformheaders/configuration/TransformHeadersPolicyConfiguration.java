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
package io.gravitee.policy.transformheaders.configuration;

import io.gravitee.policy.api.PolicyConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TransformHeadersPolicyConfiguration implements PolicyConfiguration {

    private List<String> removeHeaders = new ArrayList<>();

    private List<HttpHeader> addHeaders = new ArrayList<>();

    public List<String> getRemoveHeaders() {
        return removeHeaders;
    }

    public void setRemoveHeaders(List<String> removeHeaders) {
        this.removeHeaders = removeHeaders;
    }

    public List<HttpHeader> getAddHeaders() {
        return addHeaders;
    }

    public void setAddHeaders(List<HttpHeader> addHeaders) {
        this.addHeaders = addHeaders;
    }
}