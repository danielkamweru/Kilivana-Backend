package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.logistics.dto.LocationUpdateRequest;
import com.kilivana.backend.logistics.dto.LocationUpdateResponse;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * WebSocket side of live tracking. Clients SEND to {@code /app/tracking/{jobId}} with a
 * {@link LocationUpdateRequest}; the broker pushes the resulting {@code LocationEvent} to
 * every subscriber of that job's topic.
 *
 * <p>Authorization is enforced by {@link com.kilivana.backend.logistics.service.TrackingChannelInterceptor},
 * which refuses sends from anyone who is not the assigned driver, so this controller
 * trusts the message it receives and only does the work.
 */
@Controller
@RequiredArgsConstructor
public class TrackingWebSocketController {

    private final DriverLocationService driverLocationService;

    @MessageMapping("/tracking/{jobId}")
    public void reportLocation(@DestinationVariable Long jobId,
                              @Payload LocationUpdateRequest request) {
        if (request == null) {
            return;
        }
        request.setTripId(jobId);
        driverLocationService.submitLocation(CurrentUser.id(), request);
    }
}