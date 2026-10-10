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

    private Long tripId;
    private Double latitude;
    private Double longitude;
    private String serverTimestamp;
}