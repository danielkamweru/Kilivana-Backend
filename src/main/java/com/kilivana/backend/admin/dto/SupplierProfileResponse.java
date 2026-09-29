package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SupplierProfileResponse {

    private Long id;
    private Long userId;
    private String businessName;
    private String businessDetails;
    private String location;
    private String verificationInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImageResponse> images;

    public static SupplierProfileResponse fromEntity(SupplierProfile profile) {
        return SupplierProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .businessName(profile.getBusinessName())
                .businessDetails(profile.getBusinessDetails())
                .location(profile.getLocation())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public static SupplierProfileResponse fromEntity(SupplierProfile profile, List<ImageResponse> images) {
        return SupplierProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .businessName(profile.getBusinessName())
                .businessDetails(profile.getBusinessDetails())
                .location(profile.getLocation())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .images(images)
                .build();
    }
}