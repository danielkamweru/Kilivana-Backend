package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to register or update a platform-managed pickup station.
 *
 * <p>Operators enter verified coordinates manually; no third-party geocoding is
 * required. Latitude and longitude are validated so a swapped or out-of-range
 * pair cannot be persisted as a real pin on the map.
 */
@Schema(description = "Create or update a pickup station request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePickupStationRequest {

    @Schema(description = "Station name", example = "Kilivana Hub - Westlands")
    @NotBlank(message = "Station name is required")
    @Size(max = 255, message = "Station name must be at most 255 characters")
    private String name;

    @Schema(description = "Full physical address", example = "123 Mombasa Road, Nairobi")
    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Schema(description = "Latitude in decimal degrees, WGS84", example = "-1.2921", required = true,
            minimum = "-90", maximum = "90")
    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "latitude must be at least -90")
    @Max(value = 90, message = "latitude must be at most 90")
    private Double latitude;

    @Schema(description = "Longitude in decimal degrees, WGS84", example = "36.8219", required = true,
            minimum = "-180", maximum = "180")
    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "longitude must be at most 180")
    @Max(value = 180, message = "longitude must be at most 180")
    private Double longitude;

    @Schema(description = "Region/county used for filtering", example = "Nairobi")
    @NotBlank(message = "Region is required")
    @Size(max = 100, message = "Region must be at most 100 characters")
    private String region;

    @Schema(description = "Whether the station is currently accepting pickups", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Operating hours description", example = "8:00 AM - 8:00 PM, Mon-Sat")
    @Size(max = 255, message = "Opening hours must be at most 255 characters")
    private String openingHours;

    @Schema(description = "Contact phone number for the station", example = "+254712345678")
    @Size(max = 20, message = "Contact phone must be at most 20 characters")
    private String contactPhone;

    @Schema(description = "Additional instructions for pickup", example = "Bring your order confirmation")
    private String pickupInstructions;
}