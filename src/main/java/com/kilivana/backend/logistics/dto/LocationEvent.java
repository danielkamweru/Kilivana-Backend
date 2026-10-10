package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * The payload pushed to every subscriber of a delivery's tracking topic. Carries enough
 * context to redraw a marker without a second REST call: the trip id, the coordinates,
 * the optional telemetry, the delivery status and the instant the fix was recorded.
 */
@Schema(description = "A live location update pushed to subscribers of a delivery.")
@Data
public class LocationEvent {

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