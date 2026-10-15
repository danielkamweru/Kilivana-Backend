package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user account with role and verification details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User account response with role and verification status")
public class UserResponse {

    @Schema(description = "Unique identifier of the user", example = "10")
    private Long id;

    @Schema(description = "Full name of the user", example = "John Kamau")
    private String name;

    @Schema(description = "Email address", example = "john.kamau@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+254700000001")
    private String phone;

    @Schema(description = "Username", example = "john.kamau")
    private String username;

    @Schema(description = "Reference code (e.g., B-005, DA-005, S-001)", example = "B-005")
    private String referenceCode;

    @Schema(description = "Region or county", example = "Nairobi")
    private String region;

    @Schema(description = "User role in the system", example = "BUYER")
    private UserRole role;

    @Schema(description = "Account status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Verification status", example = "VERIFIED")
    private VerificationStatus verificationStatus;

    @Schema(description = "Timestamp when the account was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;
}
