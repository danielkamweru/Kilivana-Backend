package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * A station returned by the nearest-station search, carrying the great-circle
 * distance from the caller's coordinates so the client can render pins ordered
 * by proximity without recomputing it.
 */
@Schema(description = "Nearest pickup station with computed distance")
@Data
@Builder
public class NearestPickupStationResponse {

    @Schema(description = "Unique identifier of the pickup station", example = "1")
    private Long id;

    @Schema(description = "Name of the pickup station", example = "Kilivana Hub - Westlands")
    private String name;

    @Schema(description = "Full address of the station", example = "123 Mombasa Road, Nairobi")
    private String address;

    @Schema(description = "Latitude coordinate", example = "-1.2921")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "36.8219")
    private Double longitude;

    @Schema(description = "Region/county for filtering", example = "Nairobi")
    private String region;

    @Schema(description = "Whether the station is currently accepting pickups", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Operating hours description", example = "8:00 AM - 8:00 PM, Mon-Sat")
    private String openingHours;

    @Schema(description = "Contact phone number for the station", example = "+254712345678")
    private String contactPhone;

    @Schema(description = "Additional instructions for pickup", example = "Bring your order confirmation")
    private String pickupInstructions;

    @Schema(description = "Great-circle distance from the search point, in kilometres", example = "3.4")
    private Double distanceKm;
}