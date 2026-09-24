package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.NotificationResponse;
import com.kilivana.backend.admin.entity.Notification;
import com.kilivana.backend.admin.service.NotificationService;
import com.kilivana.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(@RequestBody Notification notification) {
        NotificationResponse result = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByUserId(@PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(@PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        NotificationResponse notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsReadPost(@PathVariable Long id) {
        return markAsRead(id);
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("All notifications marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markOwnNotificationsAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("All notifications marked as read"));
    }

    @PostMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsReadPost(@PathVariable Long userId) {
        return markAllAsRead(userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Notification deleted successfully"));
    }
}
