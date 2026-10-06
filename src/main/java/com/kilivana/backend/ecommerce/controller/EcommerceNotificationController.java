package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.admin.dto.NotificationResponse;
import com.kilivana.backend.admin.service.NotificationService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.enums.EcommerceNotificationType;
import com.kilivana.backend.ecommerce.service.EcommerceNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Ecommerce · Notifications", description = "Order, payment, dispute and inventory notification delivery for buyers, sellers and couriers")
@RestController
@RequestMapping("/api/v1/ecommerce/notifications")
@RequiredArgsConstructor
public class EcommerceNotificationController {

    private final EcommerceNotificationService ecommerceNotificationService;
    private final NotificationService notificationService;

    @Operation(summary = "Send an order notification",
            description = "Creates an order-related notification (e.g. ORDER_PLACED, ORDER_STATUS_CHANGED) for the given user.")
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendOrderNotification(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long orderId,
            @RequestParam EcommerceNotificationType type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String message) {
        NotificationResponse result;
        if (title != null || message != null) {
            result = ecommerceNotificationService.sendCustomNotification(authenticatedUserId, type, title, message);
        } else {
            result = ecommerceNotificationService.sendOrderNotification(authenticatedUserId, orderId, type);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(summary = "Send a payment notification",
            description = "Creates a payment-related notification (e.g. PAYMENT_RECEIVED, PAYMENT_FAILED) for the given user.")
    @PostMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendPaymentNotification(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long paymentId,
            @RequestParam EcommerceNotificationType type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String message) {
        NotificationResponse result;
        if (title != null || message != null) {
            result = ecommerceNotificationService.sendCustomNotification(authenticatedUserId, type, title, message);
        } else {
            result = ecommerceNotificationService.sendPaymentNotification(authenticatedUserId, paymentId, type);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(summary = "Send a dispute notification",
            description = "Creates a dispute-related notification (e.g. DISPUTE_RAISED, DISPUTE_RESOLVED) for the given user.")
    @PostMapping("/disputes/{disputeId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendDisputeNotification(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long disputeId,
            @RequestParam EcommerceNotificationType type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String message) {
        NotificationResponse result;
        if (title != null || message != null) {
            result = ecommerceNotificationService.sendCustomNotification(authenticatedUserId, type, title, message);
        } else {
            result = ecommerceNotificationService.sendDisputeNotification(authenticatedUserId, disputeId, type);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(summary = "Send an inventory notification",
            description = "Creates an inventory notification (e.g. INVENTORY_LOW) for the supplier/admin user.")
    @PostMapping("/inventory/{productId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendInventoryNotification(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long productId,
            @RequestParam String productName,
            @RequestParam EcommerceNotificationType type) {
        NotificationResponse result = ecommerceNotificationService.sendInventoryNotification(authenticatedUserId, productId, productName, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(summary = "List user's ecommerce notifications",
            description = "Returns all ecommerce-related notifications for the authenticated user, newest first. " +
                    "Optionally filter to unread only.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal Long authenticatedUserId,
            @RequestParam(value = "unread", required = false) Boolean unread) {
        List<NotificationResponse> notifications = unread != null && unread
                ? ecommerceNotificationService.getUnreadEcommerceNotifications(authenticatedUserId)
                : ecommerceNotificationService.getEcommerceNotifications(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @Operation(summary = "Get a notification by id",
            description = "Returns a single notification by its id.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @Operation(summary = "Mark a notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        NotificationResponse notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @Operation(summary = "Mark all ecommerce notifications as read",
            description = "Marks all unread ecommerce notifications for the authenticated user as read.")
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal Long authenticatedUserId) {
        List<NotificationResponse> unread = ecommerceNotificationService.getUnreadEcommerceNotifications(authenticatedUserId);
        unread.forEach(n -> {
            try {
                notificationService.markAsRead(n.getId());
            } catch (ResourceNotFoundException ignored) {
            }
        });
        return ResponseEntity.ok(ApiResponse.successMessage("All ecommerce notifications marked as read"));
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Notification deleted successfully"));
    }
}
