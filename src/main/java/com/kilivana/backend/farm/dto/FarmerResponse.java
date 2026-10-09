package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a farmer and their associated farms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO representing a farmer with their associated farms")
public class FarmerResponse {
    @Schema(description = "Unique identifier of the farmer", example = "12")
    private Long id;

    @Schema(description = "Reference code assigned to the farmer", example = "FARM-0012")
    private String referenceCode;

    @Schema(description = "Full name of the farmer", example = "John Mutua")
    private String name;

    @Schema(description = "Email address of the farmer", example = "john.mutua@example.com")
    private String email;

    @Schema(description = "Phone number of the farmer", example = "+254700123456")
    private String phone;

    @Schema(description = "Username for the farmer account", example = "johnmutua")
    private String username;

    @Schema(description = "County where the farmer is located", example = "Nairobi")
    private String county;

    @Schema(description = "Verification status of the farmer", example = "VERIFIED")
    private VerificationStatus verificationStatus;

    @Schema(description = "Current account status of the farmer", example = "ACTIVE")
    private UserStatus accountStatus;

    @Schema(description = "Timestamp when the farmer record was created", example = "2024-01-20T09:15:00")
    private LocalDateTime createdAt;

    @Schema(description = "List of farms owned by the farmer")
    private List<FarmResponse> farms;
}
