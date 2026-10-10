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

    /** ID of the delivery job this position belongs to. */
    private Long tripId;
    /** ID of the order this delivery job is fulfilling. */
    private Long orderId;
    /** ID of the driver who reported the fix. */
    private Long driverId;
    /** Latitude in decimal degrees, WGS84. */
    private Double latitude;
    /** Longitude in decimal degrees, WGS84. */
    private Double longitude;
    /** Speed over ground in km/h, when the device reported it; null otherwise. */
    private Double speedKmh;
    /** Direction of travel in degrees (0 = north, clockwise); null when unknown. */
    private Double bearing;
    /** Accuracy radius in metres, when the device reported it; null otherwise. */
    private Double accuracyMetres;
    /** Current delivery status, e.g. "in_transit". */
    private String status;
    /** The instant the fix was taken on the driver's device, ISO-8601; null when not supplied. */
    private String clientTimestamp;
    /** The instant the server accepted the fix, ISO-8601. */
    private String serverTimestamp;
}