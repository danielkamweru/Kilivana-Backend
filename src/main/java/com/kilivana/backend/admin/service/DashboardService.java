package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.DashboardStatsResponse;
import com.kilivana.backend.admin.dto.DashboardStatsResponse.CategorySlice;
import com.kilivana.backend.admin.dto.DashboardStatsResponse.OrderTrendPoint;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    /**
     * Orders that represent money the platform actually handled. Revenue and averages are
     * computed over these, so a cancelled or failed order never counts as turnover.
     */
    private static final List<OrderStatus> SETTLED_STATUSES =
            List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    /** Everything the panel has not closed, disputes included: a disputed
     *  order is still open, because its money is held rather than gone. */
    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.IN_TRANSIT,
            OrderStatus.DELIVERED, OrderStatus.DISPUTED);

    private static final List<DisputeStatus> OPEN_DISPUTE_STATUSES =
            List.of(DisputeStatus.OPEN, DisputeStatus.IN_PROGRESS);

    private static final int TREND_DAYS = 14;
    private static final int TOP_CATEGORY_LIMIT = 6;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime trendStart = LocalDate.now().minusDays(TREND_DAYS - 1L).atStartOfDay();

        return DashboardStatsResponse.builder()
                .totalFarmers(userRepository.countByRole(UserRole.FARMER))
                .totalBuyers(userRepository.countByRole(UserRole.BUYER))
                .activeOrders(orderRepository.countByStatusIn(ACTIVE_STATUSES))
                .monthlyRevenue(orderRepository.sumTotalByStatusInSince(SETTLED_STATUSES, monthStart))
                .monthlyOrders(orderRepository.countByStatusInSince(SETTLED_STATUSES, monthStart))
                .pendingVerifications(userRepository.countByVerificationStatus(VerificationStatus.PENDING))
                .openDisputes(disputeRepository.countByStatusIn(OPEN_DISPUTE_STATUSES))
                .averageOrderValue(orderRepository.averageTotalByStatusIn(SETTLED_STATUSES))
                .updatedAt(LocalDateTime.now())
                .orderTrend(buildTrend(trendStart))
                .topCategories(buildTopCategories())
                .build();
    }

    /**
     * Dense series: every day in the window appears exactly once, with zero on days that had
     * no orders, so the admin app can plot a straight line without gap-filling.
     */
    private List<OrderTrendPoint> buildTrend(LocalDateTime since) {
        Map<LocalDate, Object[]> byDay = new LinkedHashMap<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            byDay.put(since.toLocalDate().plusDays(i), new Object[]{0L, BigDecimal.ZERO});
        }

        for (Object[] row : orderRepository.dailyTotalsByStatusInSince(SETTLED_STATUSES, since)) {
            Object[] bucket = byDay.get(((java.sql.Date) row[0]).toLocalDate());
            if (bucket != null) {
                bucket[0] = ((Number) row[1]).longValue();
                bucket[1] = (BigDecimal) row[2];
            }
        }

        List<OrderTrendPoint> points = new ArrayList<>(byDay.size());
        byDay.forEach((day, values) -> points.add(OrderTrendPoint.builder()
                .date(day.atStartOfDay())
                .orders((Long) values[0])
                .revenue((BigDecimal) values[1])
                .build()));
        return points;
    }

    private List<CategorySlice> buildTopCategories() {
        return orderItemRepository.countByCategoryForStatuses(SETTLED_STATUSES).stream()
                .limit(TOP_CATEGORY_LIMIT)
                .map(row -> CategorySlice.builder()
                        .category((String) row[0])
                        .orders(((Number) row[1]).longValue())
                        .build())
                .toList();
    }
}
