package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRequest {

    @NotNull(message = "Inspector ID is required")
    private Long inspectorId;

    @NotNull(message = "Target type is required")
    private String targetType;

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotNull(message = "Status is required")
    private InspectionStatus status;

    private InspectionResult result;

    private String notes;

    private String evidenceUrls;
}
