package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<ImageResponse> images;

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

    public static DriverProfileResponse fromEntity(DriverProfile profile, List<ImageResponse> images) {
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
                .images(images)
                .build();
    }
}