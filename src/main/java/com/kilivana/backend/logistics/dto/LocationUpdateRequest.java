package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A single GPS fix reported by the driver's app while they are on an active delivery.
 *
 * <p>The caller's identity comes from the JWT, never from this body: {@code driverId} is
 * deliberately absent so a client cannot impersonate another driver. The server resolves the
 * driver from the token and checks that they are the one assigned to {@code tripId}.
 *
 * <p>{@code clientTimestamp} is the instant the fix was taken on the device, which can lag
 * behind server time on a poor connection. It is accepted up to {@code TRACKING_MAX_CLOCK_SKEW}
 * in the past or future; anything older is rejected as stale rather than silently stored.
 */
@Schema(description = "A GPS fix reported by the driver for an active delivery.")
@Data
public class LocationUpdateRequest {

    /** The delivery job the driver is currently running. */
    @Schema(description = "ID of the delivery job the driver is running.",
            example = "17", required = true, minimum = "1")
    @NotNull(message = "tripId is required")
    @Min(value = 1, message = "tripId must be a positive number")
    private Long tripId;

    /** Latitude in decimal degrees, WGS84. */
    @Schema(description = "Latitude in decimal degrees, WGS84.", example = "-1.2921", required = true,
            minimum = "-90", maximum = "90")
    @NotNull(message = "latitude is required")
    @Min(value = -90, message = "latitude must be at least -90")
    @Max(value = 90, message = "latitude must be at most 90")
    private Double latitude;

    /** Longitude in decimal degrees, WGS84. */
    @Schema(description = "Longitude in decimal degrees, WGS84.", example = "36.8219", required = true,
            minimum = "-180", maximum = "180")
    @NotNull(message = "longitude is required")
    @Min(value = -180, message = "longitude must be at least -180")
    @Max(value = 180, message = "longitude must be at most 180")
    private Double longitude;

    /** Speed over ground in km/h, when the device can report it. Null is legitimate. */
    @Schema(description = "Speed over ground in km/h, when the device can report it. Null is legitimate.",
            example = "24.5", minimum = "0", maximum = "300")
    @Min(value = 0, message = "speed must be at least 0")
    @Max(value = 300, message = "speed must be at most 300")
    private Double speedKmh;

    /** Direction of travel in degrees, 0 = north, clockwise. */
    @Schema(description = "Direction of travel in degrees, 0 = north, clockwise.", example = "135",
            minimum = "0", maximum = "360")
    @Min(value = 0, message = "bearing must be at least 0")
    @Max(value = 360, message = "bearing must be at most 360")
    private Double bearing;

    /** The instant the fix was taken on the device, ISO-8601. */
    @Schema(description = "The instant the fix was taken on the device, ISO-8601.",
            example = "2026-10-09T12:00:00Z")
    private String clientTimestamp;

    /** Accuracy radius in metres, when the device reports it. */
    @Schema(description = "Accuracy radius in metres, when the device reports it.", example = "6.2")
    @Min(value = 0, message = "accuracyMetres must be at least 0")
    @Max(value = 500, message = "accuracyMetres must be at most 500")
    private Double accuracyMetres;
}