package com.kilivana.backend.logistics.config;

import com.kilivana.backend.security.JwtHandshakeInterceptor;
import com.kilivana.backend.logistics.service.TrackingChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Enables STOMP-over-WebSocket for live delivery tracking.
 *
 * <p>Clients connect to {@code /ws} with a bearer token in the query string and then:
 * <pre>
 *   SUBSCRIBE  /user/{userId}/tracking
 *   SUBSCRIBE  /topic/tracking/{jobId}
 *   SEND       /app/tracking/{jobId}      (driver only)
 * </pre> The {@code /user/} prefix is the Spring broker's per-user queue: a message sent to
 * {@code /user/7/tracking} is delivered only to the session that authenticated as user 7,
 * which is what keeps one buyer from receiving another buyer's tracking.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String WS_ENDPOINT = "/ws";
    static final String USER_DESTINATION_PREFIX = "/user";
    static final String TOPIC_PREFIX = "/topic";
    static final String APP_PREFIX = "/app";

    private final JwtHandshakeInterceptor handshakeInterceptor;
    private final TrackingChannelInterceptor trackingInterceptor;

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint(WS_ENDPOINT)
                .setAllowedOrigins("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker(TOPIC_PREFIX, USER_DESTINATION_PREFIX);
        config.setApplicationDestinationPrefixes(APP_PREFIX);
        config.setUserDestinationPrefix(USER_DESTINATION_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(trackingInterceptor);
    }
}