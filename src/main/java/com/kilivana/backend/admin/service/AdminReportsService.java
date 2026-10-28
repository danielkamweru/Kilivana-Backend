package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.DisputeReportResponse;
import com.kilivana.backend.admin.dto.ReportOverviewResponse;
import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
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
public class AdminReportsService {

    private static final java.util.List<OrderStatus> SETTLED =
            java.util.List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public ReportOverviewResponse overview(LocalDate from, LocalDate to) {
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(days);
        LocalDate prevTo = from.minusDays(1);

        ReportOverviewResponse.Kpis kpis = buildKpis(from, to);
        kpis.setPrevious(buildKpis(prevFrom, prevTo));

        List<ReportOverviewResponse.TrendPoint> salesTrend = buildTrend(from, to);
        List<ReportOverviewResponse.TrendPoint> orders = buildOrders(from, to);

        return ReportOverviewResponse.builder()
                .kpis(kpis)
                .salesTrend(salesTrend)
                .orders(orders)
                .build();
    }

    private ReportOverviewResponse.Kpis buildKpis(LocalDate from, LocalDate to) {
        LocalDateTime since = from.atStartOfDay();
        BigDecimal revenue = orderRepository.sumTotalByStatusInSince(SETTLED, since);
        if (revenue == null) revenue = BigDecimal.ZERO;
        // subtract out-of-range days by filtering daily totals
        BigDecimal rangeRevenue = BigDecimal.ZERO;
        long rangeOrders = 0;
        for (Object[] row : orderRepository.dailyTotalsByStatusInSince(SETTLED, since)) {
            LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
            if (!d.isBefore(from) && !d.isAfter(to)) {
                rangeRevenue = rangeRevenue.add((BigDecimal) row[2]);
                rangeOrders += ((Number) row[1]).longValue();
            }
        }
        BigDecimal aov = rangeOrders > 0 ? rangeRevenue.divide(BigDecimal.valueOf(rangeOrders), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        long disputes = disputeRepository.countByStatusIn(java.util.List.of(com.kilivana.backend.common.enums.DisputeStatus.OPEN,
                com.kilivana.backend.common.enums.DisputeStatus.IN_PROGRESS));
        double disputeRate = rangeOrders > 0 ? (disputes * 100.0 / rangeOrders) : 0.0;
        return ReportOverviewResponse.Kpis.builder()
                .revenue(rangeRevenue).orders(rangeOrders).averageOrderValue(aov).disputeRate(disputeRate).build();
    }

    private List<ReportOverviewResponse.TrendPoint> buildTrend(LocalDate from, LocalDate to) {
        List<ReportOverviewResponse.TrendPoint> pts = new ArrayList<>();
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) byDay.put(d, BigDecimal.ZERO);
        for (Object[] row : orderRepository.dailyTotalsByStatusInSince(SETTLED, from.atStartOfDay())) {
            LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
            if (byDay.containsKey(d)) byDay.put(d, (BigDecimal) row[2]);
        }
        byDay.forEach((d, v) -> pts.add(ReportOverviewResponse.TrendPoint.builder().date(d).value(v).build()));
        return pts;
    }

    private List<ReportOverviewResponse.TrendPoint> buildOrders(LocalDate from, LocalDate to) {
        List<ReportOverviewResponse.TrendPoint> pts = new ArrayList<>();
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) byDay.put(d, 0L);
        for (Object[] row : orderRepository.dailyTotalsByStatusInSince(SETTLED, from.atStartOfDay())) {
            LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
            if (byDay.containsKey(d)) byDay.put(d, ((Number) row[1]).longValue());
        }
        byDay.forEach((d, v) -> pts.add(ReportOverviewResponse.TrendPoint.builder().date(d).value(BigDecimal.valueOf(v)).build()));
        return pts;
    }

    @Transactional(readOnly = true)
    public DisputeReportResponse disputes(LocalDate from, LocalDate to) {
        long total = disputeRepository.count();
        long open = disputeRepository.countByStatusIn(java.util.List.of(com.kilivana.backend.common.enums.DisputeStatus.OPEN,
                com.kilivana.backend.common.enums.DisputeStatus.IN_PROGRESS));
        long resolved = disputeRepository.countByStatusIn(java.util.List.of(com.kilivana.backend.common.enums.DisputeStatus.RESOLVED,
                com.kilivana.backend.common.enums.DisputeStatus.CLOSED));
        return DisputeReportResponse.builder()
                .total(total).open(open).resolved(resolved).avgResolutionHours(0.0)
                .outcomes(java.util.Map.of("BUYER", 0L, "FARMER", 0L)).build();
    }
}
