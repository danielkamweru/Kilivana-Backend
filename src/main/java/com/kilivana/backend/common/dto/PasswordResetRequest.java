package com.kilivana.backend.common.dto;

import com.kilivana.backend.common.service.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/v1/auth/reset-password}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    /** Raw single-use token supplied in the recovery email. */
    @NotBlank(message = "Reset token is required")
    private String token;
    /**
     * Replacement password. Recovery applies the same rules as registration, so a
     * reset cannot be used to install a password registration would have refused.
     */
    @NotBlank(message = "New password is required")
    @Size(min = PasswordPolicy.MIN_LENGTH, message = "Password must be at least " + PasswordPolicy.MIN_LENGTH + " characters")
    private String newPassword;
}
