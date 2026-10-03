package com.kilivana.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectorProfileRequest {

    private String inspectorDetails;

    /** What the inspector is qualified to check, e.g. "Tea & Coffee". */
    private String specialization;

    @NotBlank
    private String assignedArea;

    @NotBlank
    private String status;
}