package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Report overview response")
public class ReportOverviewResponse {
    @Schema(description = "KPIs") private Kpis kpis;
    @Schema(description = "Sales trend") private List<TrendPoint> salesTrend;
    @Schema(description = "Order counts") private List<TrendPoint> orders;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Kpis {
        private BigDecimal revenue;
        private long orders;
        private BigDecimal averageOrderValue;
        private double disputeRate;
        private Kpis previous;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal value;
    }
}
