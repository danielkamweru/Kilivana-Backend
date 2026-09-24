package com.kilivana.backend.admin.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.ecommerce.service.ProductService;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOperationsController {

    private final ProductService productService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final LogisticsService logisticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Long>>> dashboard() {
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("orders", orderService.searchOrders(null, null, Pageable.unpaged()).getTotalElements());
        metrics.put("payments", (long) paymentService.getAllPayments().size());
        metrics.put("logisticsJobs", (long) logisticsService.getAllJobs().size());
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateProductStatus(@PathVariable Long id, @RequestParam ProductStatus status) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProductStatus(id, status)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> orders(Pageable pageable) {
        Page<OrderResponse> orders = orderService.searchOrders(null, null, pageable).map(this::mapOrder);
        return ResponseEntity.ok(ApiResponse.success(orders));
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

    private OrderResponse mapOrder(Order order) {
        return new OrderResponse(order.getId(), order.getBuyerId(), order.getStatus(), order.getSubtotal(),
                order.getDeliveryFee(), order.getTotal(), order.getPaymentStatus(), order.getAddressId(),
                order.getCancellationReason(), order.getCreatedAt(), order.getUpdatedAt());
    }
}