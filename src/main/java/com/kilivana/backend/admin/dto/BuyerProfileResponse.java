package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.BuyerProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BuyerProfileResponse {

    private Long id;
    private Long userId;
    private String contactDetails;
    private String savedAddresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BuyerProfileResponse fromEntity(BuyerProfile profile) {
        return BuyerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .contactDetails(profile.getContactDetails())
                .savedAddresses(profile.getSavedAddresses())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}