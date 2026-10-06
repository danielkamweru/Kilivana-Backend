package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.OrderRequest;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "E-Commerce · Orders", description = "Order placement, status transitions, timeline and disputes")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request.getBuyerId(), request.getAddressId(),
                request.getDeliveryFee(), request.getItems(),
                request.getSubtotal(), request.getTotal());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderService.toResponse(order)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<OrderResponse> orders = orderService.searchOrders(buyerId, status, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByBuyer(@PathVariable Long buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(ApiResponse.success(orders.stream().map(orderService::toResponse).toList()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrders(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<OrderResponse> responses = orderService.searchOrders(buyerId, status, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id, @RequestParam String status) {
        Order order = orderService.updateOrderStatus(id, OrderStatus.from(status));
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Order order = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmReceipt(@PathVariable Long id) {
        Order order = orderService.confirmReceipt(id);
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> postOrderStatus(
            @PathVariable Long id, @RequestParam String status) {
        return updateOrderStatus(id, status);
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<List<OrderEvent>>> getOrderTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderTimeline(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Order deleted successfully"));
    }
}
