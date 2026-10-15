package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for registering a new user account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a new user account")
public class UserRegistrationRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Full name of the user", example = "John Kamau")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "john.kamau@example.com")
    private String email;

    @NotBlank(message = "Phone is required")
    @Schema(description = "Phone number", example = "+254700000001")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Account password", example = "UserPass123")
    private String password;

    @NotNull(message = "Role is required")
    @Schema(description = "User role in the system", example = "BUYER")
    private UserRole role;

    @Schema(description = "Optional username", example = "john.kamau")
    private String username;

    @Schema(description = "Region or county", example = "Nairobi")
    private String region;
}
