package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.OrderRequest;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/ecommerce/orders", "/api/v1/orders"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request.getBuyerId(), request.getSubtotal(), request.getDeliveryFee(), request.getTotal(), request.getAddressId());
        OrderResponse response = new OrderResponse(order.getId(), order.getBuyerId(), order.getStatus(), order.getSubtotal(), order.getDeliveryFee(), order.getTotal(), order.getPaymentStatus(), order.getAddressId(), order.getCancellationReason(), order.getCreatedAt(), order.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        OrderResponse response = mapToResponse(order);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<OrderResponse> orders = orderService.searchOrders(buyerId, status, pageable)
                .map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByBuyer(@PathVariable Long buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(ApiResponse.success(orders.stream().map(this::mapToResponse).toList()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrders(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<Order> orders = orderService.searchOrders(buyerId, status, pageable);
        Page<OrderResponse> responses = orders.map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(order)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        Order order = orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(order)));
    }

    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmReceipt(@PathVariable Long id) {
        Order order = orderService.updateOrderStatus(id, OrderStatus.COMPLETED);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(order)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Order deleted successfully"));
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(order.getId(), order.getBuyerId(), order.getStatus(), order.getSubtotal(), order.getDeliveryFee(), order.getTotal(), order.getPaymentStatus(), order.getAddressId(), order.getCancellationReason(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
