package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of the administrator's driver list: the user account, the driver profile and the
 * delivery history, in a single read.
 *
 * <p>The admin panel showed all of these together. Fetching them separately meant the list had
 * to be stitched together client-side from a user list, a profile per user and a job list,
 * which meant a driver without a profile either vanished or threw.
 */
@Data
@Builder
@Schema(description = "Admin view of a driver with profile, vehicle, KYC and delivery summary")
public class AdminDriverResponse {

    @Schema(description = "User account ID", example = "20")
    private Long userId;

    @Schema(description = "Driver profile ID", example = "8")
    private Long profileId;

    @Schema(description = "Driver code such as DA-005", example = "DA-005")
    private String code;

    @Schema(description = "Full name of the driver", example = "Peter Otieno")
    private String fullName;

    @Schema(description = "Username", example = "peter.otieno")
    private String username;

    @Schema(description = "Email address", example = "peter.otieno@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+254711000002")
    private String phone;

    @Schema(description = "Region or county", example = "Kiambu")
    private String region;

    @Schema(description = "Physical address", example = "45 Thika Road, Kiambu")
    private String address;

    @Schema(description = "Driver availability status", example = "AVAILABLE")
    private DriverStatus status;

    @Schema(description = "Reason if driver is suspended", example = "License expired")
    private String suspensionReason;

    @Schema(description = "Type of identification document", example = "National ID")
    private String idType;

    @Schema(description = "Identification number", example = "12345678")
    private String idNumber;

    @Schema(description = "Driving license number", example = "DL1234567")
    private String licenseNumber;

    @Schema(description = "Driving license expiry date", example = "2027-12-31")
    private LocalDate licenseExpiryDate;

    @Schema(description = "KYC verification status", example = "VERIFIED")
    private KycStatus kycStatus;

    @Schema(description = "Type of vehicle", example = "PICKUP")
    private VehicleType vehicleType;

    @Schema(description = "Vehicle capacity as displayed (e.g., 5T)", example = "5T")
    private String vehicleCapacity;

    @Schema(description = "Vehicle registration plate", example = "KCA 123A")
    private String plateNumber;

    @Schema(description = "Vehicle make/model", example = "Toyota Hilux")
    private String vehicleMake;

    @Schema(description = "Order ID of the delivery in progress, or null when the driver has none", example = "105")
    private Long activeOrderId;

    @Schema(description = "Total number of completed deliveries", example = "42")
    private long totalDeliveries;

    @Schema(description = "Driver rating (always null - not yet implemented)", example = "4.5")
    private Double rating;

    @Schema(description = "Timestamp when the driver account was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;
}