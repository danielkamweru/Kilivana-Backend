package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionResponse {

    private Long id;
    private Long inspectorId;
    private String targetType;
    private Long targetId;
    private InspectionStatus status;
    private InspectionResult result;
    private String notes;
    private String evidenceUrls;
    private LocalDateTime inspectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InspectionResponse fromEntity(com.kilivana.backend.admin.entity.Inspection inspection) {
        return InspectionResponse.builder()
                .id(inspection.getId())
                .inspectorId(inspection.getInspectorId())
                .targetType(inspection.getTargetType())
                .targetId(inspection.getTargetId())
                .status(inspection.getStatus())
                .result(inspection.getResult())
                .notes(inspection.getNotes())
                .evidenceUrls(inspection.getEvidenceUrls())
                .inspectedAt(inspection.getInspectedAt())
                .createdAt(inspection.getCreatedAt())
                .updatedAt(inspection.getUpdatedAt())
                .build();
    }
}
