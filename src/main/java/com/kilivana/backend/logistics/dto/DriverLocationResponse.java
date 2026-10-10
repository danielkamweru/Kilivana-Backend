package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * The public view of a driver's last known position. Delivered both over REST and over
 * WebSocket, so the two transports can never drift apart: this is the one shape a client
 * needs to know.
 *
 * <p>Latitude and longitude are present for every fix; speed, bearing and accuracy are
 * optional because not every device reports them.
 */
@Schema(description = "A driver's last known position for a delivery.")
@Data
public class DriverLocationResponse {

    private Long tripId;
    private Long orderId;
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double bearing;
    private Double accuracyMetres;
    private String status;
    private String clientTimestamp;
    private String serverTimestamp;
}