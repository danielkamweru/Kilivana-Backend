package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.DashboardStatsResponse;
import com.kilivana.backend.admin.service.DashboardService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.ecommerce.service.ProductService;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOperationsController {

    private final ProductService productService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final LogisticsService logisticsService;
    private final DashboardService dashboardService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final LogisticsJobRepository logisticsJobRepository;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Dashboard totals, order trend and category breakdown")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> dashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Long>>> dashboard() {
        // Counted in the database. The previous version materialised every order and every
        // payment to call size() on the result.
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("orders", orderRepository.count());
        metrics.put("payments", paymentRepository.count());
        metrics.put("logisticsJobs", logisticsJobRepository.count());
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateProductStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.updateProductStatus(id, ProductStatus.from(status))));
    }

    /**
     * The order pipeline, filterable by buyer and status, so the
     * panel can show its tabs: placed, confirmed (processing),
     * in_transit (shipped), delivered, completed, disputed, cancelled.
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> orders(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<OrderResponse> orders = orderService.searchOrders(buyerId, status, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Dispatches an order: the pending delivery job is reused or
     * created (pickup defaults to the first seller's region, the
     * destination to the buyer's delivery address), and the driver
     * is assigned. The order moves to confirmed with the assignment.
     */
    @PostMapping("/orders/{orderId}/assign")
    public ResponseEntity<ApiResponse<LogisticsJob>> assignOrderToDriver(
            @PathVariable Long orderId,
            @RequestParam Long driverId,
            @RequestParam(required = false) String pickupAddress,
            @RequestParam(required = false) String destinationAddress) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.assignOrderToDriver(orderId, driverId, pickupAddress, destinationAddress)));
    }

    @GetMapping("/logistics/jobs")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> logisticsJobs() {
        return ResponseEntity.ok(ApiResponse.success(logisticsService.getAllJobs()));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> payments() {
        List<PaymentResponse> payments = paymentService.getAllPayments().stream()
                .map(paymentService::mapToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Map<String, Long>>> reports() {
        Map<String, Long> report = new LinkedHashMap<>();
        report.put("orders", orderService.searchOrders(null, null, Pageable.unpaged()).getTotalElements());
        report.put("logisticsJobs", (long) logisticsService.getAllJobs().size());
        report.put("payments", (long) paymentService.getAllPayments().size());
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}