package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.dto.NotificationResponse;
import com.kilivana.backend.admin.entity.Notification;
import com.kilivana.backend.admin.repository.NotificationRepository;
import com.kilivana.backend.ecommerce.enums.EcommerceNotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Convenience wrapper that posts typed e-commerce notifications to the shared
 * {@link NotificationService}.
 *
 * <p>Each method builds a {@link Notification} with the appropriate type code, a default
 * title/message from {@link EcommerceNotificationType}, and the relevant entity ID appended
 * so the panel can deep-link. The underlying notification table is shared with admin and
 * logistics notifications; this service only knows the e-commerce subset.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EcommerceNotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Sends an order-related notification (e.g., ORDER_PLACED, ORDER_STATUS_CHANGED).
     */
    public NotificationResponse sendOrderNotification(Long userId, Long orderId, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (order #" + orderId + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    /**
     * Sends a payment-related notification (e.g., PAYMENT_RECEIVED, PAYMENT_FAILED).
     */
    public NotificationResponse sendPaymentNotification(Long userId, Long paymentId, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (payment #" + paymentId + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    /**
     * Sends a dispute-related notification (e.g., DISPUTE_RAISED, DISPUTE_RESOLVED).
     */
    public NotificationResponse sendDisputeNotification(Long userId, Long disputeId, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (dispute #" + disputeId + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    /**
     * Sends an inventory notification (e.g., INVENTORY_LOW) for a product.
     */
    public NotificationResponse sendInventoryNotification(Long userId, Long productId, String productName, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (" + productName + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    /**
     * Sends a custom notification with overridden title and/or message.
     */
    public NotificationResponse sendCustomNotification(Long userId, EcommerceNotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(title != null ? title : type.getDefaultTitle())
                .message(message != null ? message : type.getDefaultMessage())
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    /** Returns all e-commerce notifications for a user, newest first. */
    public List<NotificationResponse> getEcommerceNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> isEcommerceType(n.getType()))
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** Returns only unread e-commerce notifications for a user. */
    public List<NotificationResponse> getUnreadEcommerceNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadAtIsNull(userId).stream()
                .filter(n -> isEcommerceType(n.getType()))
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** Checks if a notification type code belongs to the e-commerce subset. */
    private boolean isEcommerceType(String type) {
        for (EcommerceNotificationType t : EcommerceNotificationType.values()) {
            if (t.getCode().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
