package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
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
public class SupplierRegistrationRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    @NotBlank(message = "Username is required")
    @Size(min = 4, message = "Username must be at least 4 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\s-]{9,15}$", message = "Phone must be 9–15 digits, spaces and +/- allowed")
    private String phone;

    @NotBlank(message = "Region is required")
    private String region;

    private String address;

    @NotBlank(message = "Category is required")
    private String category;

    private LocalDate contractEndDate;

    @Pattern(regexp = "^(active|pending|suspended)$", message = "Status must be active, pending or suspended")
    private String status;

    /** Required on POST, optional on PUT. */
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
