package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User notification response")
public class NotificationResponse {

    @Schema(description = "Unique identifier of the notification", example = "100")
    private Long id;

    @Schema(description = "ID of the user this notification belongs to", example = "10")
    private Long userId;

    @Schema(description = "Type of notification", example = "ORDER_STATUS")
    private String type;

    @Schema(description = "Notification title", example = "Order Confirmed")
    private String title;

    @Schema(description = "Notification message content", example = "Your order #ORD-2026-001 has been confirmed and is being prepared.")
    private String message;

    @Schema(description = "Timestamp when the notification was read, null if unread", example = "2026-01-15T11:00:00")
    private LocalDateTime readAt;

    @Schema(description = "Timestamp when the notification was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(com.kilivana.backend.admin.entity.Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
