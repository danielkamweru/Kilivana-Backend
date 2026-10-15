package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for creating or updating a driver profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a driver profile")
public class DriverProfileRequest {

    @Schema(description = "Where the driver lives and collects the vehicle", example = "45 Thika Road, Kiambu")
    private String address;

    @NotBlank
    @Schema(description = "Driving license number", example = "DL1234567")
    private String licenseNumber;

    @NotNull
    @Schema(description = "Type of vehicle", example = "PICKUP")
    private VehicleType vehicleType;

    @NotBlank
    @Schema(description = "Registration plate", example = "KCA 123A")
    private String vehicleNumber;

    @Schema(description = "Additional vehicle details", example = "Refrigerated container")
    private String vehicleDetails;

    @Schema(description = "Vehicle make/model", example = "Toyota Hilux")
    private String vehicleMake;

    @Schema(description = "Payload capacity as written by an administrator, such as \"5T\" or \"200kg\"", example = "5T")
    private String vehicleCapacity;

    @Schema(description = "Payload capacity in kilograms, for arithmetic", example = "5000")
    private Integer vehicleCapacityKg;

    @Schema(description = "Driving license expiry date", example = "2027-12-31")
    private LocalDate licenseExpiryDate;

    @Schema(description = "Type of identification document", example = "National ID")
    private String idType;

    @Schema(description = "Identification number", example = "12345678")
    private String idNumber;

    @Schema(description = "Whether an administrator has checked the licence and national ID", example = "VERIFIED")
    private KycStatus kycStatus;

    @NotNull
    @Schema(description = "Driver availability status", example = "AVAILABLE")
    private DriverStatus availabilityStatus;

    @Schema(description = "Required when availabilityStatus is SUSPENDED", example = "License expired")
    private String suspensionReason;
}