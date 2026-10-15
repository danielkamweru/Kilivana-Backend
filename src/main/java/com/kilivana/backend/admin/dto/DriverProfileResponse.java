package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a driver profile with vehicle, KYC, and image details.
 */
@Data
@Builder
@Schema(description = "Driver profile response with vehicle, KYC, and image details")
public class DriverProfileResponse {

    @Schema(description = "Unique identifier of the driver profile", example = "8")
    private Long id;

    @Schema(description = "ID of the user this profile belongs to", example = "20")
    private Long userId;

    @Schema(description = "Where the driver lives and collects the vehicle", example = "45 Thika Road, Kiambu")
    private String address;

    @Schema(description = "Driving license number", example = "DL1234567")
    private String licenseNumber;

    @Schema(description = "Type of vehicle", example = "PICKUP")
    private VehicleType vehicleType;

    @Schema(description = "Registration plate", example = "KCA 123A")
    private String vehicleNumber;

    @Schema(description = "Additional vehicle details", example = "Refrigerated container")
    private String vehicleDetails;

    @Schema(description = "Vehicle make/model", example = "Toyota Hilux")
    private String vehicleMake;

    @Schema(description = "Payload capacity in kilograms, for arithmetic", example = "5000")
    private Integer vehicleCapacityKg;

    @Schema(description = "The capacity as recorded, such as \"5T\". Lets a client show the label it was given.", example = "5T")
    private String vehicleCapacity;

    @Schema(description = "Driving license expiry date", example = "2027-12-31")
    private LocalDate licenseExpiryDate;

    @Schema(description = "Type of identification document", example = "National ID")
    private String idType;

    @Schema(description = "Identification number", example = "12345678")
    private String idNumber;

    @Schema(description = "KYC verification status", example = "VERIFIED")
    private KycStatus kycStatus;

    @Schema(description = "Driver availability status", example = "AVAILABLE")
    private DriverStatus availabilityStatus;

    @Schema(description = "Reason if driver is suspended", example = "License expired")
    private String suspensionReason;

    @Schema(description = "Timestamp when the profile was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the profile was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Associated images (license, vehicle, etc.)")
    private List<ImageResponse> images;

    public static DriverProfileResponse fromEntity(DriverProfile profile) {
        return fromEntity(profile, null);
    }

    public static DriverProfileResponse fromEntity(DriverProfile profile, List<ImageResponse> images) {
        return DriverProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .address(profile.getAddress())
                .licenseNumber(profile.getLicenseNumber())
                .vehicleType(profile.getVehicleType())
                .vehicleNumber(profile.getVehicleNumber())
                .vehicleDetails(profile.getVehicleDetails())
                .vehicleMake(profile.getVehicleMake())
                .vehicleCapacityKg(profile.getVehicleCapacityKg())
                .vehicleCapacity(DriverCapacityFormatter.format(profile.getVehicleCapacityKg()))
                .licenseExpiryDate(profile.getLicenseExpiryDate())
                .idType(profile.getIdType())
                .idNumber(profile.getIdNumber())
                .kycStatus(profile.getKycStatus())
                .availabilityStatus(profile.getAvailabilityStatus())
                .suspensionReason(profile.getSuspensionReason())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .images(images)
                .build();
    }
}