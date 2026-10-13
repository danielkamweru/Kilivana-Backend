package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Lightweight acknowledgment of an accepted location update. Returning the trip id and
 * the server timestamp lets the driver app reconcile its own clock against the server's
 * without a follow-up GET.
 */
@Schema(description = "Acknowledgment of an accepted location update.")
@Data
public class LocationUpdateResponse {

    /** ID of the delivery job the fix was recorded for. */
    private Long tripId;
    /** Latitude of the accepted fix, in decimal degrees, WGS84. */
    private Double latitude;
    /** Longitude of the accepted fix, in decimal degrees, WGS84. */
    private Double longitude;
    /** The instant the server accepted the fix, ISO-8601; lets the client reconcile its clock. */
    private String serverTimestamp;

    // Explicit getters and setters for Lombok compatibility
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(String serverTimestamp) { this.serverTimestamp = serverTimestamp; }
}