package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Dispute report response")
public class DisputeReportResponse {
    private long total;
    private long open;
    private long resolved;
    private double avgResolutionHours;
    private Map<String, Long> outcomes;
}
