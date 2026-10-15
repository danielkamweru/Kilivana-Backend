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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * In-app notification delivery and read-state tracking. Mapped under both the
 * admin path (ADMIN role required) and the plain user path (any authenticated
 * user), so one feed serves the admin dashboard and end users alike.
 */
@Tag(name = "Administration · Notifications", description = "In-app notification delivery and read-state tracking")
@RestController
@RequestMapping({"/api/v1/admin/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Creates a new in-app notification.
     *
     * @param notification the notification to create
     * @return the created notification
     */
    @Operation(summary = "Create a notification",
            description = "Creates and delivers a new in-app notification.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notification created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(@RequestBody Notification notification) {
        NotificationResponse result = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * Fetches a single notification by id.
     *
     * @param id the notification id
     * @return the notification
     */
    @Operation(summary = "Get a notification by id",
            description = "Returns a single notification.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @Parameter(description = "Notification id", example = "1") @PathVariable Long id) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    /**
     * Lists notifications across every user, optionally filtered to unread only.
     *
     * @param unread optional filter for unread notifications
     * @return matching notifications, newest first
     */
    @Operation(summary = "List notifications for every user",
            description = "Returns every user's notifications, newest first. "
                    + "Set unread=true to keep only notifications that have not been read.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid unread filter format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications(
            @Parameter(description = "Filter to unread notifications only", example = "true") @RequestParam(required = false) Boolean unread) {
        // An admin screen needs one feed across users; without this the only option was to
        // loop over every user id. Optional unread filter mirrors /user/{userId}/unread.
        List<NotificationResponse> notifications = unread == null
                ? notificationService.getAllNotifications()
                : notificationService.getAllNotifications().stream()
                        .filter(n -> n.getReadAt() == null)
                        .toList();
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Lists all notifications for one user.
     *
     * @param userId the user id
     * @return the user's notifications, newest first
     */
    @Operation(summary = "List notifications for a user",
            description = "Returns every notification belonging to the given user, newest first.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByUserId(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Lists a user's unread notifications.
     *
     * @param userId the user id
     * @return the user's unread notifications
     */
    @Operation(summary = "List unread notifications for a user",
            description = "Returns the notifications the given user has not read yet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Marks one notification as read.
     *
     * @param id the notification id
     * @return the updated notification
     */
    @Operation(summary = "Mark a notification as read",
            description = "Sets the read timestamp on a single notification.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @Parameter(description = "Notification id", example = "1") @PathVariable Long id) {
        NotificationResponse notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    /**
     * Marks one notification as read (POST alias of PUT /{id}/read).
     *
     * @param id the notification id
     * @return the updated notification
     */
    @Operation(summary = "Mark a notification as read (POST)",
            description = "POST alias for marking a single notification as read.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsReadPost(
            @Parameter(description = "Notification id", example = "1") @PathVariable Long id) {
        return markAsRead(id);
    }

    /**
     * Marks all of a user's notifications as read.
     *
     * @param userId the user id
     * @return empty response with a success message
     */
    @Operation(summary = "Mark all of a user's notifications as read",
            description = "Sets the read timestamp on every unread notification of the given user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("All notifications marked as read"));
    }

    /**
     * Marks all of the authenticated user's notifications as read.
     *
     * @param userId the authenticated user's id
     * @return empty response with a success message
     */
    @Operation(summary = "Mark my notifications as read",
            description = "Marks every unread notification of the authenticated user as read.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markOwnNotificationsAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("All notifications marked as read"));
    }

    /**
     * Marks all of a user's notifications as read (POST alias of PUT /user/{userId}/read-all).
     *
     * @param userId the user id
     * @return empty response with a success message
     */
    @Operation(summary = "Mark all of a user's notifications as read (POST)",
            description = "POST alias for marking every unread notification of the given user as read.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @PostMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsReadPost(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId) {
        return markAllAsRead(userId);
    }

    /**
     * Deletes a notification.
     *
     * @param id the notification id
     * @return empty response with a success message
     */
    @Operation(summary = "Delete a notification",
            description = "Permanently removes a notification.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(description = "Notification id", example = "1") @PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Notification deleted successfully"));
    }
}
