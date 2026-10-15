package com.kilivana.backend.dto;

import com.kilivana.backend.common.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Public-facing farmer profile information exposed to buyers.
 * Contains only non-sensitive data suitable for public display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public farmer profile information")
public class PublicFarmerProfileResponse {

    @Schema(description = "Unique identifier of the farmer", example = "23")
    private Long id;

    @Schema(description = "Full name of the farmer", example = "Jane Farmer")
    private String name;

    @Schema(description = "County where the farmer is located", example = "Nakuru County")
    private String region;

    @Schema(description = "Verification status of the farmer", example = "VERIFIED")
    private VerificationStatus verificationStatus;

    @Schema(description = "Timestamp when the farmer record was created", example = "2026-10-07T10:00:00")
    private LocalDateTime createdAt;

    public static PublicFarmerProfileResponse of(Long id, String name, String region,
                                                  VerificationStatus verificationStatus, LocalDateTime createdAt) {
        PublicFarmerProfileResponse response = new PublicFarmerProfileResponse();
        response.id = id;
        response.name = name;
        response.region = region;
        response.verificationStatus = verificationStatus;
        response.createdAt = createdAt;
        return response;
    }
}
