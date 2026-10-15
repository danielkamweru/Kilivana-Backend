package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Administrator submission for a new supplier or an edit to an existing supplier.
 *
 * <p>{@code password} is required on POST, optional on PUT &mdash; a blank value means
 * the password is left as-is.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to register or update a supplier account and profile")
public class SupplierRegistrationRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    @Schema(description = "Registered company name", example = "Green Valley Farms Ltd")
    private String companyName;

    @NotBlank(message = "Contact person is required")
    @Schema(description = "Primary contact person name", example = "Mary Wanjiku")
    private String contactPerson;

    @NotBlank(message = "Username is required")
    @Size(min = 4, message = "Username must be at least 4 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, dots, underscores and hyphens")
    @Schema(description = "Unique username for login", example = "greenvalley")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Contact email address", example = "info@greenvalley.co.ke")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\s-]{9,15}$", message = "Phone must be 9–15 digits, spaces and +/- allowed")
    @Schema(description = "Contact phone number", example = "+254722000003")
    private String phone;

    @NotBlank(message = "Region is required")
    @Schema(description = "Region or county where the supplier operates", example = "Nakuru")
    private String region;

    @Schema(description = "Physical address", example = "Plot 45, Nakuru-Eldoret Road")
    private String address;

    @NotBlank(message = "Category is required")
    @Schema(description = "Primary product category", example = "Vegetables")
    private String category;

    @Schema(description = "Contract end date", example = "2026-12-31")
    private LocalDate contractEndDate;

    @Pattern(regexp = "^(active|pending|suspended)$", message = "Status must be active, pending or suspended")
    @Schema(description = "Account status for admin panel", example = "active")
    private String status;

    /** Required on POST, optional on PUT. */
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Account password (required on create, optional on update)", example = "Suppl!erPass123")
    private String password;
}
