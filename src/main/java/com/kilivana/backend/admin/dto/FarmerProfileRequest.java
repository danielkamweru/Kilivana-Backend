package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.OwnershipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a farmer profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a farmer profile")
public class FarmerProfileRequest {

    @NotBlank
    @Schema(description = "Name of the farm", example = "Green Acres Farm")
    private String farmName;

    @NotBlank
    @Schema(description = "Physical location of the farm", example = "Kiambu County, near Thika")
    private String location;

    @Schema(description = "Additional details about the farm operations", example = "Organic vegetable farm, 50 acres, drip irrigation")
    private String farmDetails;

    @Schema(description = "Verification information or documents reference", example = "Certificate #ORG-2026-001")
    private String verificationInfo;

    /**
     * Required when this request creates the profile's linked farm. It remains
     * optional for later profile-only edits, because the value is owned by the
     * farm record once it has been created.
     */
    @Positive
    @Schema(description = "Farm size in acres", example = "2.5")
    private Double sizeAcres;

    /** Required when this request creates the profile's linked farm. */
    @Schema(description = "How the farmer holds the land", example = "OWNED")
    private OwnershipType ownershipType;
}
