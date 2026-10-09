package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating an inspector profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update an inspector profile")
public class InspectorProfileRequest {

    @Schema(description = "Details about the inspector's background and experience", example = "Former KEBS officer with 10 years produce inspection experience")
    private String inspectorDetails;

    @Schema(description = "What the inspector is qualified to check", example = "Tea & Coffee")
    private String specialization;

    @NotBlank
    @Schema(description = "Geographic area assigned to the inspector", example = "Central Kenya Region")
    private String assignedArea;

    @NotBlank
    @Schema(description = "Current status of the inspector", example = "ACTIVE")
    private String status;
}