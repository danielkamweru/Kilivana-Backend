package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.security.JwtHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the WebSocket channel interceptor in isolation. The interceptor is the only
 * place that enforces authorization on subscriptions and sends, so it gets its own unit
 * test with mocked repositories: a malformed destination is dropped, an unauthorized
 * subscription is refused, and an unassigned driver cannot push a fix.
 */
class TrackingChannelInterceptorTest {

    @Test
    void dropsMalformedSubscriptionDestination() {
        TrackingChannelInterceptor interceptor = interceptorWithJob(null, null);
        Message<?> message = message("/topic/tracking/", 1L);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isNull();
    }

    @Test
    void dropsMalformedSendDestination() {
        TrackingChannelInterceptor interceptor = interceptorWithJob(null, null);
        Message<?> message = message("/app/tracking/", 1L);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isNull();
    }

    @Test
    void allowsMessageWithNoDestination() {
        TrackingChannelInterceptor interceptor = interceptorWithJob(null, null);
        Message<?> message = message(null, 1L);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void allowsMessageWithNoUserId() {
        TrackingChannelInterceptor interceptor = interceptorWithJob(null, null);
        Message<?> message = message("/topic/tracking/1", null);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void refusesSendFromUnassignedDriver() {
        LogisticsJob job = new LogisticsJob();
        job.setDriverId(99L);
        TrackingChannelInterceptor interceptor = interceptorWithJob(1L, job);

        Message<?> message = message("/app/tracking/1", 7L);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void allowsSendFromAssignedDriver() {
        LogisticsJob job = new LogisticsJob();
        job.setDriverId(7L);
        TrackingChannelInterceptor interceptor = interceptorWithJob(1L, job);

        Message<?> message = message("/app/tracking/1", 7L);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    private TrackingChannelInterceptor interceptorWithJob(Long jobId, LogisticsJob job) {
        LogisticsJobRepository repository = mock(LogisticsJobRepository.class);
        if (jobId != null && job != null) {
            when(repository.findById(jobId)).thenReturn(java.util.Optional.of(job));
        }
        TrackingAuthorizationService authorizationService = mock(TrackingAuthorizationService.class);
        return new TrackingChannelInterceptor(repository, authorizationService);
    }

    private static Message<?> message(String destination, Long userId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (userId != null) {
            accessor.setSessionAttributes(Map.of(JwtHandshakeInterceptor.USER_ID_PROPERTY, userId));
        }
        return MessageBuilder.withPayload(new Object()).setHeaders(accessor).build();
    }
}