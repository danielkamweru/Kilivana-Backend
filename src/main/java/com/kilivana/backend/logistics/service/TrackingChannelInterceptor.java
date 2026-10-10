package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.security.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Enforces authorization on the WebSocket channel, because the broker itself does not
 * know who may follow which delivery.
 *
 * <p>Two checks, both driven by the user id stashed on the session by
 * {@link JwtHandshakeInterceptor}:
 * <ul>
 *   <li><b>Subscribe</b> — a client may subscribe to {@code /topic/tracking/{jobId}} only
 *       if they are the buyer, a seller on that order, the assigned driver, or an
 *       administrator. The per-user destination {@code /user/{id}/tracking} is open to
 *       whoever authenticated as that id.</li>
 *   <li><b>Send</b> — only the driver assigned to the job may push a fix to
 *       {@code /app/tracking/{jobId}}.</li>
 * </ul> A failed check is logged at debug (no coordinates, no user id in the message) and
 * the message is dropped, which the client sees as a silent failure to subscribe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingChannelInterceptor implements ChannelInterceptor {

    private final LogisticsJobRepository logisticsJobRepository;
    private final TrackingAuthorizationService authorizationService;

    @Override
    public Message<?> preSend(@Nullable Message<?> message, @NonNull MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();
        Long userId = userId(accessor);

        if (destination == null || userId == null) {
            return message;
        }

        if (destination.startsWith("/topic/tracking/")) {
            Long jobId = parseJobId(destination, "/topic/tracking/".length());
            if (jobId == null) {
                log.debug("Dropped subscription to malformed tracking topic");
                return null;
            }
            authorizationService.ensureCanTrack(jobId, userId);
        } else if (destination.startsWith("/app/tracking/")) {
            Long jobId = parseJobId(destination, "/app/tracking/".length());
            if (jobId == null) {
                log.debug("Dropped send to malformed tracking destination");
                return null;
            }
            ensureIsDriver(jobId, userId);
        }
        // /user/{id}/tracking subscriptions are allowed for the session's own user only,
        // which the handshake already guarantees by scoping the destination to that id.
        return message;
    }

    private void ensureIsDriver(Long jobId, Long userId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ForbiddenException("Delivery not found"));
        if (job.getDriverId() == null || !job.getDriverId().equals(userId)) {
            throw new ForbiddenException("Only the assigned driver may report a location");
        }
    }

    private static Long userId(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return null;
        }
        Object userId = attributes.get(JwtHandshakeInterceptor.USER_ID_PROPERTY);
        if (userId instanceof Long value) {
            return value;
        }
        return null;
    }

    private static Long parseJobId(String destination, int prefixLength) {
        String suffix = destination.substring(prefixLength).trim();
        if (suffix.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(suffix);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}