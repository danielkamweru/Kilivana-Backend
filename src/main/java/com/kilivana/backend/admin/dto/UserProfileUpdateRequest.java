package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating a user's basic profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a user's basic profile details")
public class UserProfileUpdateRequest {

    @NotBlank
    @Schema(description = "Full name of the user", example = "John Kamau")
    private String name;

    @NotBlank
    @Email
    @Schema(description = "Email address", example = "john.kamau@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Phone number", example = "+254700000001")
    private String phone;

    @Schema(description = "Region or county", example = "Nairobi")
    private String region;
}