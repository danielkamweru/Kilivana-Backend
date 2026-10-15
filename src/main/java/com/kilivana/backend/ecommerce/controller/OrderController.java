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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing purchase orders.
 * Supports placement, retrieval, status transitions, timeline, cancellation and deletion.
 * Orders contain line items with product details at the time of purchase.
 */
@Tag(name = "E-Commerce · Orders", description = "Order placement, status transitions, timeline and disputes")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new purchase order from the supplied request payload.
     * The subtotal and total are computed server-side from line items; the
     * values in the request are used only for reconciliation.
     *
     * @param request the order details (buyer, address, items, delivery fee, reconciliation totals)
     * @return the created order
     */
    @Operation(summary = "Create order", description = "Creates a new purchase order from the supplied request payload and returns the created order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Parameter(description = "Order creation details") @Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request.getBuyerId(), request.getAddressId(),
                request.getDeliveryFee(), request.getItems(),
                request.getSubtotal(), request.getTotal());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderService.toResponse(order)));
    }

    /**
     * Retrieves a single order by its identifier.
     *
     * @param id the order identifier
     * @return the order with all details
     */
    @Operation(summary = "Get order by id", description = "Retrieves a single order by its identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@Parameter(description = "Order identifier") @PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    /**
     * Returns a paginated list of orders, optionally filtered by buyer and/or status.
     *
     * @param buyerId optional buyer ID filter
     * @param status optional status filter (e.g., CONFIRMED, SHIPPED)
     * @param pageable pagination and sorting parameters
     * @return paginated orders
     */
    @Operation(summary = "List orders", description = "Returns a paginated list of orders, optionally filtered by buyer and/or status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @Parameter(description = "Filter orders by buyer ID") @RequestParam(required = false) Long buyerId,
            @Parameter(description = "Filter orders by status") @RequestParam(required = false) String status,
            @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        Page<OrderResponse> orders = orderService.searchOrders(buyerId, status, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Retrieves all orders placed by a specific buyer.
     *
     * @param buyerId the buyer identifier
     * @return list of orders for that buyer
     */
    @Operation(summary = "Get orders by buyer", description = "Retrieves all orders placed by a specific buyer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Buyer not found")
    })
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByBuyer(@Parameter(description = "Buyer identifier") @PathVariable Long buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(ApiResponse.success(orders.stream().map(orderService::toResponse).toList()));
    }

    /**
     * Searches orders with optional buyer and status filters, paginated.
     * Alias for the main list endpoint with identical behavior.
     *
     * @param buyerId optional buyer ID filter
     * @param status optional status filter
     * @param pageable pagination and sorting parameters
     * @return paginated orders
     */
    @Operation(summary = "Search orders", description = "Searches orders with optional buyer and status filters, paginated.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrders(
            @Parameter(description = "Filter orders by buyer ID") @RequestParam(required = false) Long buyerId,
            @Parameter(description = "Filter orders by status") @RequestParam(required = false) String status,
            @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        Page<OrderResponse> responses = orderService.searchOrders(buyerId, status, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Transitions an order to a new status.
     * Valid statuses: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED.
     *
     * @param id the order identifier
     * @param status the new order status
     * @return the updated order
     */
    @Operation(summary = "Update order status", description = "Transitions an order to a new status and returns the updated order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or unknown status value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order identifier") @PathVariable Long id,
            @Parameter(description = "New order status value") @RequestParam String status) {
        Order order = orderService.updateOrderStatus(id, OrderStatus.from(status));
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    /**
     * Cancels an order with an optional reason.
     *
     * @param id the order identifier
     * @param reason optional cancellation reason
     * @return the updated (cancelled) order
     */
    @Operation(summary = "Cancel order", description = "Cancels an order with an optional reason and returns the updated order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order identifier") @PathVariable Long id,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        Order order = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    /**
     * Marks an order as received by the buyer (confirms receipt).
     *
     * @param id the order identifier
     * @return the updated order
     */
    @Operation(summary = "Confirm receipt", description = "Marks an order as received by the buyer and returns the updated order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order confirmed and receipt acknowledged"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmReceipt(@Parameter(description = "Order identifier") @PathVariable Long id) {
        Order order = orderService.confirmReceipt(id);
        return ResponseEntity.ok(ApiResponse.success(orderService.toResponse(order)));
    }

    /**
     * Accepts a status transition request body and delegates to the status update workflow.
     * Alternative POST-based endpoint for clients that cannot use PUT.
     *
     * @param id the order identifier
     * @param status the new order status
     * @return the updated order
     */
    @Operation(summary = "Post order status", description = "Accepts a status transition request body and delegates to the status update workflow.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or unknown status value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> postOrderStatus(
            @Parameter(description = "Order identifier") @PathVariable Long id,
            @Parameter(description = "New order status value") @RequestParam String status) {
        return updateOrderStatus(id, status);
    }

    /**
     * Retrieves the event history (timeline) for a given order.
     *
     * @param id the order identifier
     * @return list of order events (status changes, payments, etc.)
     */
    @Operation(summary = "Get order timeline", description = "Retrieves the event history for a given order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Timeline retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<List<OrderEvent>>> getOrderTimeline(@Parameter(description = "Order identifier") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderTimeline(id)));
    }

    /**
     * Deletes an order by its identifier.
     *
     * @param id the order identifier
     * @return success message
     */
    @Operation(summary = "Delete order", description = "Deletes an order by its identifier and returns a confirmation message.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@Parameter(description = "Order identifier") @PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Order deleted successfully"));
    }
}
