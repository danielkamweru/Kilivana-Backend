package com.kilivana.backend.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/v1/auth/login}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginRequest {

    /** Account email; must be a valid address and is matched case-insensitively. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** Important: The plain-text password should never be stored in the database. Only its BCrypt hash should be stored.. */
    @NotBlank(message = "Password is required")
    private String password;
}
