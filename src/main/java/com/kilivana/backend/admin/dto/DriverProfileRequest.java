package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileRequest {

    /** Where the driver lives and collects the vehicle. Recorded by an administrator. */
    private String address;

    @NotBlank
    private String licenseNumber;

    @NotNull
    private VehicleType vehicleType;

    /** Registration plate. */
    @NotBlank
    private String vehicleNumber;

    private String vehicleDetails;

    private String vehicleMake;

    /**
     * Payload capacity as written by an administrator, such as {@code "5T"} or {@code "200kg"}.
     * Accepted alongside {@link #vehicleCapacityKg}; when both are sent the kilogram figure
     * wins, because that is the one used for arithmetic.
     */
    private String vehicleCapacity;

    /** Payload capacity in kilograms, for arithmetic. Derived from {@link #vehicleCapacity} if absent. */
    private Integer vehicleCapacityKg;

    private LocalDate licenseExpiryDate;

    private String idType;

    private String idNumber;

    /** Whether an administrator has checked the licence and national ID. */
    private KycStatus kycStatus;

    @NotNull
    private DriverStatus availabilityStatus;

    /** Required when {@link #availabilityStatus} is {@link DriverStatus#SUSPENDED}. */
    private String suspensionReason;
}