package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to save a pickup station to a user's profile.
 */
@Schema(description = "Save pickup station request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePickupStationRequest {

    @Schema(description = "ID of the pickup station to save", example = "5")
    @NotNull(message = "Pickup station ID is required")
    private Long pickupStationId;

    @Schema(description = "Optional custom label for the saved station", example = "My usual station")
    private String label;

    @Schema(description = "Whether to set this as the default pickup station", example = "false")
    private Boolean isDefault;
}
