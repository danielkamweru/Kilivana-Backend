package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.DriverProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DriverProfileResponse {

    private Long id;
    private Long userId;
    private String licenseNumber;
    private String vehicleType;
    private String vehicleNumber;
    private String vehicleDetails;
    private String availabilityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DriverProfileResponse fromEntity(DriverProfile profile) {
        return DriverProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .licenseNumber(profile.getLicenseNumber())
                .vehicleType(profile.getVehicleType())
                .vehicleNumber(profile.getVehicleNumber())
                .vehicleDetails(profile.getVehicleDetails())
                .availabilityStatus(profile.getAvailabilityStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}