package com.kilivana.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
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
 * Everything the admin panel's driver form collects, in the one flat object it submits.
 *
 * <p>Registering a driver needs two rows — a user account and a driver profile — and the form
 * collects the fields of both at once. Requiring the caller to split the payload across
 * {@code POST /admin/users} and then {@code POST /profiles/drivers/{userId}} meant the account
 * could be created with its profile step never taken, leaving a driver the roster shows as
 * blank.
 *
 * <p>The {@link JsonAlias} names are the spellings the panel uses ({@code licenceNumber},
 * {@code licenceExpiry}, {@code plateNumber}, {@code fullName}). The canonical property names
 * stay as they are so existing clients are unaffected.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @JsonAlias("name")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 4, message = "Username must be at least 4 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may contain letters, numbers, dot, underscore and dash")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\s-]{9,15}$", message = "Phone number format is invalid")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Region is required")
    private String region;

    private String address;

    // KYC
    @NotBlank(message = "ID type is required")
    private String idType;

    @NotBlank(message = "ID number is required")
    private String idNumber;

    @NotBlank(message = "Licence number is required")
    @JsonAlias("licenseNumber")
    private String licenceNumber;

    @NotNull(message = "Licence expiry is required")
    @JsonAlias("licenseExpiryDate")
    private LocalDate licenceExpiry;

    private KycStatus kycStatus;

    // Vehicle
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle capacity is required")
    @JsonAlias({"capacity"})
    private String vehicleCapacity;

    @NotBlank(message = "Plate number is required")
    @JsonAlias({"vehicleNumber"})
    private String plateNumber;

    private String vehicleMake;

    /**
     * A new driver starts offline: they go online from their own app once they accept work.
     * Accepting it here would let an administrator put a driver on the road who has never
     * signed in.
     */
    private com.kilivana.backend.common.enums.DriverStatus availabilityStatus;
}