package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated figures for the admin dashboard home screen. Every count is computed in the
 * database; the trend series is dense (every day in the window appears exactly once, with zero
 * on empty days) so the app can plot a straight line without gap-filling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated figures for the admin dashboard home screen")
public class DashboardStatsResponse {

    @Schema(description = "Registered farmers", example = "128")
    private Long totalFarmers;

    @Schema(description = "Registered buyers", example = "76")
    private Long totalBuyers;

    @Schema(description = "Orders that are neither cancelled nor failed", example = "23")
    private Long activeOrders;

    @Schema(description = "Sum of settled order totals for the current calendar month", example = "1250000.00")
    private BigDecimal monthlyRevenue;

    @Schema(description = "Users still awaiting verification, any role", example = "12")
    private Long pendingVerifications;

    @Schema(description = "Disputes in OPEN or IN_PROGRESS", example = "5")
    private Long openDisputes;

    @Schema(description = "Mean order total across settled orders", example = "18500.50")
    private BigDecimal averageOrderValue;

    @Schema(description = "Settled orders placed in the current calendar month", example = "68")
    private Long monthlyOrders;

    @Schema(description = "When these figures were computed", example = "2026-09-30T23:59:59")
    private LocalDateTime updatedAt;

    @Schema(description = "Settled orders per day for the last 14 days, oldest first")
    private List<OrderTrendPoint> orderTrend;

    @Schema(description = "Most-ordered product categories, highest count first")
    private List<CategorySlice> topCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "One day of the order trend")
    public static class OrderTrendPoint {
        @Schema(example = "2026-09-30")
        private LocalDateTime date;
        @Schema(example = "6")
        private Long orders;
        @Schema(example = "45000.00")
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "One category in the category breakdown")
    public static class CategorySlice {
        @Schema(example = "Vegetables")
        private String category;
        @Schema(example = "18")
        private Long orders;
    }
}
