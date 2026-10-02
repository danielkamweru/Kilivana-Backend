package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DriverProfileResponse {

    private Long id;
    private Long userId;
    private String address;
    private String licenseNumber;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private String vehicleDetails;
    private String vehicleMake;
    private Integer vehicleCapacityKg;
    /** The capacity as recorded, such as "5T". Lets a client show the label it was given. */
    private String vehicleCapacity;
    private LocalDate licenseExpiryDate;
    private String idType;
    private String idNumber;
    private KycStatus kycStatus;
    private DriverStatus availabilityStatus;
    private String suspensionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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