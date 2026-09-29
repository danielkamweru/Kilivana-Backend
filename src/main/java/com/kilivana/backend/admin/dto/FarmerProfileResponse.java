package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.FarmerProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FarmerProfileResponse {

    private Long id;
    private Long userId;
    private String farmName;
    private String location;
    private String farmDetails;
    private String verificationInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImageResponse> images;

    public static FarmerProfileResponse fromEntity(FarmerProfile profile) {
        return FarmerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .farmName(profile.getFarmName())
                .location(profile.getLocation())
                .farmDetails(profile.getFarmDetails())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public static FarmerProfileResponse fromEntity(FarmerProfile profile, List<ImageResponse> images) {
        return FarmerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .farmName(profile.getFarmName())
                .location(profile.getLocation())
                .farmDetails(profile.getFarmDetails())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .images(images)
                .build();
    }
}