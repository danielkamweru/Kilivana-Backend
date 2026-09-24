package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.InspectorProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InspectorProfileResponse {

    private Long id;
    private Long userId;
    private String inspectorDetails;
    private String assignedArea;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InspectorProfileResponse fromEntity(InspectorProfile profile) {
        return InspectorProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .inspectorDetails(profile.getInspectorDetails())
                .assignedArea(profile.getAssignedArea())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}