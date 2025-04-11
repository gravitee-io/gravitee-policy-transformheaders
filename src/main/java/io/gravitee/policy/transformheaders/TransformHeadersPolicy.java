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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.gravitee.policy.transformheaders.configuration.HttpHeader;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.gravitee.policy.transformheaders.v3.TransformHeadersPolicyV3;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import org.apache.kafka.common.protocol.Errors;

/**
 * @author Guillaume Lamirand (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformHeadersPolicy extends TransformHeadersPolicyV3 implements HttpPolicy, KafkaPolicy {

    private static final String TRANSFORM_HEADERS_FAILURE = "TRANSFORM_HEADERS_FAILURE";

    public TransformHeadersPolicy(final TransformHeadersPolicyConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String id() {
        return "transform-headers";
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.defer(() -> transform(ctx, ctx.request().headers()));
    }

    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return Completable.defer(() -> transform(ctx, ctx.response().headers()));
    }

    private Completable transform(final HttpPlainExecutionContext ctx, final HttpHeaders httpHeaders) {
        return transformHeaders(ctx.getTemplateEngine(), httpHeaders)
            .onErrorResumeWith(
                ctx.interruptWith(
                    new ExecutionFailure(500).key(TRANSFORM_HEADERS_FAILURE).message("Unable to apply headers transformation")
                )
            );
    }

    @Override
    public Completable onMessageRequest(HttpMessageExecutionContext ctx) {
        return ctx.request().onMessage(message -> transformMessageHeaders(ctx, message));
    }

    @Override
    public Completable onMessageResponse(HttpMessageExecutionContext ctx) {
        return ctx.response().onMessage(message -> transformMessageHeaders(ctx, message));
    }

    private Maybe<Message> transformMessageHeaders(final HttpMessageExecutionContext ctx, final Message message) {
        return transformHeaders(ctx.getTemplateEngine(message), message.headers())
            .andThen(Maybe.just(message))
            .onErrorResumeWith(
                ctx.interruptMessageWith(
                    new ExecutionFailure(500).key(TRANSFORM_HEADERS_FAILURE).message("Unable to apply headers transformation on message")
                )
            );
    }

    private Completable transformHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return addHeaders(templateEngine, httpHeaders)
            .andThen(appendHeaders(templateEngine, httpHeaders))
            .andThen(Completable.fromRunnable(() -> removeHeaders(httpHeaders)));
    }

    private Completable addHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return updateHeaders(
            configuration::getAddHeaders,
            templateEngine,
            (key, value) -> Optional.ofNullable(httpHeaders).map(h -> h.set(key, value)).orElse(null)
        );
    }

    private Completable appendHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return updateHeaders(
            configuration::getAppendHeaders,
            templateEngine,
            (key, value) -> Optional.ofNullable(httpHeaders).map(h -> h.add(key, value)).orElse(null)
        );
    }

    @Override
    public Completable onMessageRequest(KafkaMessageExecutionContext ctx) {
        return ctx.request().onMessage(message -> transformKafkaMessageHeaders(ctx.executionContext(), message));
    }

    @Override
    public Completable onMessageResponse(KafkaMessageExecutionContext ctx) {
        return ctx.response().onMessage(message -> transformKafkaMessageHeaders(ctx.executionContext(), message));
    }

    private Maybe<KafkaMessage> transformKafkaMessageHeaders(KafkaExecutionContext ctx, KafkaMessage kafkaMessage) {
        return transformHeaders(ctx.getTemplateEngine(), kafkaMessage)
            .onErrorResumeWith(ctx.interruptWith(Errors.INVALID_RECORD))
            .andThen(Maybe.just(kafkaMessage));
    }

    private Completable transformHeaders(final TemplateEngine templateEngine, final KafkaMessage message) {
        return addHeaders(templateEngine, message).andThen(Completable.fromRunnable(() -> removeHeaders(message)));
    }

    private Completable addHeaders(final TemplateEngine templateEngine, final KafkaMessage message) {
        return updateHeaders(
            configuration::getAddHeaders,
            templateEngine,
            (key, value) -> message.putRecordHeader(key, Buffer.buffer(value))
        );
    }

    private Completable updateHeaders(
        Callable<List<HttpHeader>> configurationHeaders,
        final TemplateEngine templateEngine,
        BiFunction<String, String, ?> updateHeaders
    ) {
        return Maybe
            .fromCallable(configurationHeaders)
            .flatMapPublisher(Flowable::fromIterable)
            .filter(httpHeader -> httpHeader.getName() != null && !httpHeader.getName().trim().isEmpty() && httpHeader.getValue() != null)
            .flatMapCompletable(httpHeader ->
                templateEngine
                    .eval(httpHeader.getValue(), String.class)
                    .doOnSuccess(newValue -> updateHeaders.apply(httpHeader.getName(), newValue))
                    .ignoreElement()
            );
    }
}
