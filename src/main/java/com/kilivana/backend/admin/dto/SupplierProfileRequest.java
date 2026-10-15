package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a supplier profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a supplier profile")
public class SupplierProfileRequest {

    @NotBlank
    @Schema(description = "Registered business name", example = "Green Valley Farms Ltd")
    private String businessName;

    @Schema(description = "Additional details about the business operations", example = "Wholesale vegetable supplier, cold storage facility on-site")
    private String businessDetails;

    @NotBlank
    @Schema(description = "Physical location of the business", example = "Plot 45, Nakuru-Eldoret Road")
    private String location;

    @Schema(description = "Verification information or documents reference", example = "Business License #BL-2026-045")
    private String verificationInfo;
}