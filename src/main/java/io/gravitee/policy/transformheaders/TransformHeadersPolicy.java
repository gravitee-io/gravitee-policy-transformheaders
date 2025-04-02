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

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.gravitee.policy.transformheaders.v3.TransformHeadersPolicyV3;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Guillaume Lamirand (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformHeadersPolicy extends TransformHeadersPolicyV3 implements Policy {

    private static final String TRANSFORM_HEADERS_FAILURE = "TRANSFORM_HEADERS_FAILURE";

    public TransformHeadersPolicy(final TransformHeadersPolicyConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String id() {
        return "transform-headers";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.defer(() -> transform(ctx, ctx.request().headers()));
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return Completable.defer(() -> transform(ctx, ctx.response().headers()));
    }

    private Completable transform(final HttpExecutionContext ctx, final HttpHeaders httpHeaders) {
        return transformHeaders(ctx.getTemplateEngine(), httpHeaders)
            .onErrorResumeWith(
                ctx.interruptWith(
                    new ExecutionFailure(500).key(TRANSFORM_HEADERS_FAILURE).message("Unable to apply headers transformation")
                )
            );
    }

    @Override
    public Completable onMessageRequest(MessageExecutionContext ctx) {
        return ctx.request().onMessage(message -> transformMessageHeaders(ctx, message));
    }

    @Override
    public Completable onMessageResponse(MessageExecutionContext ctx) {
        return ctx.response().onMessage(message -> transformMessageHeaders(ctx, message));
    }

    private Maybe<Message> transformMessageHeaders(final MessageExecutionContext ctx, final Message message) {
        return transformHeaders(ctx.getTemplateEngine(message), message.headers())
            .andThen(Maybe.just(message))
            .onErrorResumeWith(
                ctx.interruptMessageWith(
                    new ExecutionFailure(500).key(TRANSFORM_HEADERS_FAILURE).message("Unable to apply headers transformation on message")
                )
            );
    }

    private Completable transformHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return setHeaders(templateEngine, httpHeaders)
            .andThen(appendHeaders(templateEngine, httpHeaders))
            .andThen(Completable.fromRunnable(() -> removeHeaders(httpHeaders)));
    }

    private Completable setHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return Maybe
            .fromCallable(configuration::getAddHeaders)
            .flatMapPublisher(Flowable::fromIterable)
            .filter(httpHeader -> httpHeader.getName() != null && !httpHeader.getName().trim().isEmpty() && httpHeader.getValue() != null)
            .flatMapCompletable(httpHeader ->
                templateEngine
                    .eval(httpHeader.getValue(), String.class)
                    .doOnSuccess(newValue -> httpHeaders.set(httpHeader.getName(), newValue))
                    .ignoreElement()
            );
    }

    private Completable appendHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return Maybe
            .fromCallable(configuration::getAppendHeaders)
            .flatMapPublisher(Flowable::fromIterable)
            .filter(httpHeader -> httpHeader.getName() != null && !httpHeader.getName().trim().isEmpty() && httpHeader.getValue() != null)
            .flatMapCompletable(httpHeader ->
                templateEngine
                    .eval(httpHeader.getValue(), String.class)
                    .doOnSuccess(newValue -> httpHeaders.add(httpHeader.getName(), newValue))
                    .ignoreElement()
            );
    }
}
