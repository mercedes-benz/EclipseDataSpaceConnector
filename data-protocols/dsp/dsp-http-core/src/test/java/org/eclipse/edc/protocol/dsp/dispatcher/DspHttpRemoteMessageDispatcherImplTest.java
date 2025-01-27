/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.dispatcher;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRequestFactory;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.response.DspHttpResponseBodyExtractor;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspHttpRemoteMessageDispatcherImplTest {

    private static final String SCOPE_CLAIM = "scope";
    private static final String AUDIENCE_CLAIM = "aud";
    private final EdcHttpClient httpClient = mock();
    private final IdentityService identityService = mock();
    private final PolicyEngine policyEngine = mock();
    private final TokenDecorator tokenDecorator = mock();
    private final DspHttpRequestFactory<TestMessage> requestFactory = mock();
    private final Duration timeout = Duration.of(5, SECONDS);

    private final DspHttpRemoteMessageDispatcher dispatcher =
            new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService, tokenDecorator, policyEngine);

    private static okhttp3.Response dummyResponse(int code) {
        return dummyResponseBuilder(code)
                .build();
    }

    @NotNull
    private static okhttp3.Response.Builder dummyResponseBuilder(int code) {
        return new okhttp3.Response.Builder()
                .code(code)
                .message("any")
                .body(ResponseBody.create("", MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://any").build());
    }

    @BeforeEach
    void setUp() {
        when(tokenDecorator.decorate(any())).thenAnswer(a -> a.getArgument(0));
    }

    @Test
    void protocol_returnDsp() {
        assertThat(dispatcher.protocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    @Test
    void dispatch_ensureTokenDecoratorScope() {
        var authToken = "token";
        Map<String, Object> additional = Map.of("foo", "bar");
        when(tokenDecorator.decorate(any())).thenAnswer(a -> a.getArgument(0, TokenParameters.Builder.class).claims(additional).claims(SCOPE_CLAIM, "test-scope"));
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(httpClient.executeAsync(any(), isA(List.class))).thenReturn(completedFuture(dummyResponse(200)));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(authToken).build()));

        dispatcher.registerMessage(TestMessage.class, requestFactory, mock());

        var message = new TestMessage();
        var result = dispatcher.dispatch(String.class, message);

        assertThat(result).succeedsWithin(timeout);

        var captor = ArgumentCaptor.forClass(TokenParameters.class);
        verify(identityService).obtainClientCredentials(captor.capture());
        verify(httpClient).executeAsync(argThat(r -> authToken.equals(r.headers().get("Authorization"))), isA(List.class));
        verify(requestFactory).createRequest(message);
        assertThat(captor.getValue()).satisfies(tr -> {
            assertThat(tr.getStringClaim(SCOPE_CLAIM)).isEqualTo("test-scope");
            assertThat(tr.getStringClaim(AUDIENCE_CLAIM)).isEqualTo(message.getCounterPartyAddress());
            assertThat(tr.getClaims()).containsAllEntriesOf(additional);
        });

    }

    @Test
    void dispatch_messageNotRegistered_throwException() {
        assertThat(dispatcher.dispatch(String.class, new TestMessage())).failsWithin(timeout)
                .withThrowableThat().withCauseInstanceOf(EdcException.class).withMessageContaining("found");

        verifyNoInteractions(httpClient);
    }

    @Test
    void dispatch_failedToObtainToken_throwException() {
        dispatcher.registerMessage(TestMessage.class, requestFactory, mock());
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));

        assertThat(dispatcher.dispatch(String.class, new TestMessage())).failsWithin(timeout)
                .withThrowableThat().withCauseInstanceOf(EdcException.class).withMessageContaining("credentials");

        verifyNoInteractions(httpClient);
    }

    @Test
    void dispatch_shouldNotEvaluatePolicy_whenItIsNotRegistered() {
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(httpClient.executeAsync(any(), isA(List.class))).thenReturn(completedFuture(dummyResponse(200)));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("any").build()));
        dispatcher.registerMessage(TestMessage.class, requestFactory, mock());

        var result = dispatcher.dispatch(String.class, new TestMessage());

        assertThat(result).succeedsWithin(timeout);
        verifyNoInteractions(policyEngine);
    }

    @Test
    void dispatch_shouldEvaluatePolicy() {
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(httpClient.executeAsync(any(), isA(List.class))).thenReturn(completedFuture(dummyResponse(200)));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("any").build()));
        dispatcher.registerMessage(TestMessage.class, requestFactory, mock());
        var policy = Policy.Builder.newInstance().build();
        dispatcher.registerPolicyScope(TestMessage.class, "test.message", m -> policy);

        var result = dispatcher.dispatch(String.class, new TestMessage());

        assertThat(result).succeedsWithin(timeout);
        verify(policyEngine).evaluate(eq("test.message"), eq(policy), and(isA(PolicyContext.class), argThat(c -> c.getContextData(TokenParameters.Builder.class) != null)));
    }

    static class TestMessage implements RemoteMessage {
        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getCounterPartyAddress() {
            return "http://connector";
        }
    }

    @Nested
    class Response {

        private final DspHttpResponseBodyExtractor<Object> bodyExtractor = mock();

        @Test
        void shouldShouldReturnSuccess_whenResponseIsSuccessful() {
            respondWith(dummyResponse(200), bodyExtractor);
            when(bodyExtractor.extractBody(any())).thenReturn("response");

            var future = dispatcher.dispatch(String.class, new TestMessage());

            assertThat(future).succeedsWithin(timeout).satisfies(result -> {
                assertThat(result).isSucceeded().isEqualTo("response");
            });
        }

        @Test
        void shouldReturnFatalError_whenResponseIsClientError() {
            var responseBody = ResponseBody.create("expectedValue", MediaType.get("application/json"));
            respondWith(dummyResponseBuilder(400).body(responseBody).build(), bodyExtractor);

            var future = dispatcher.dispatch(String.class, new TestMessage());

            assertThat(future).succeedsWithin(timeout).satisfies(result -> {
                assertThat(result).isFailed().satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(FATAL_ERROR);
                    assertThat(failure.getMessages()).containsOnly("expectedValue");
                });
            });
            verify(bodyExtractor, never()).extractBody(any());
        }

        @Test
        void shouldReturnFatalError_whenResponseIsClientErrorAndBodyIsNull() {
            respondWith(dummyResponseBuilder(400).body(null).build(), bodyExtractor);

            var future = dispatcher.dispatch(String.class, new TestMessage());

            assertThat(future).succeedsWithin(timeout).satisfies(result -> {
                assertThat(result).isFailed().satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(FATAL_ERROR);
                    assertThat(failure.getMessages()).allMatch(it -> it.contains("is null"));
                });
            });
            verify(bodyExtractor, never()).extractBody(any());
        }

        @Test
        void shouldReturnRetryError_whenResponseIsServerError() {
            respondWith(dummyResponse(500), bodyExtractor);

            var future = dispatcher.dispatch(String.class, new TestMessage());

            assertThat(future).succeedsWithin(timeout).satisfies(result -> {
                assertThat(result).isFailed().satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(ERROR_RETRY);
                });
            });
            verify(bodyExtractor, never()).extractBody(any());
        }

        private void respondWith(okhttp3.Response response, DspHttpResponseBodyExtractor<Object> bodyExtractor) {
            when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
            when(httpClient.executeAsync(any(), isA(List.class))).thenReturn(completedFuture(response));
            when(identityService.obtainClientCredentials(any()))
                    .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
            dispatcher.registerMessage(TestMessage.class, requestFactory, bodyExtractor);
        }
    }
}
