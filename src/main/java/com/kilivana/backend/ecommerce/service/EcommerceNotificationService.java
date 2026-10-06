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

@Service
@RequiredArgsConstructor
@Transactional
public class EcommerceNotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse sendOrderNotification(Long userId, Long orderId, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (order #" + orderId + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    public NotificationResponse sendPaymentNotification(Long userId, Long paymentId, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (payment #" + paymentId + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    public NotificationResponse sendDisputeNotification(Long userId, Long disputeId, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (dispute #" + disputeId + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    public NotificationResponse sendInventoryNotification(Long userId, Long productId, String productName, EcommerceNotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(type.getDefaultTitle())
                .message(type.getDefaultMessage() + " (" + productName + ")")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    public NotificationResponse sendCustomNotification(Long userId, EcommerceNotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(title != null ? title : type.getDefaultTitle())
                .message(message != null ? message : type.getDefaultMessage())
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    public List<NotificationResponse> getEcommerceNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> isEcommerceType(n.getType()))
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadEcommerceNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadAtIsNull(userId).stream()
                .filter(n -> isEcommerceType(n.getType()))
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private boolean isEcommerceType(String type) {
        for (EcommerceNotificationType t : EcommerceNotificationType.values()) {
            if (t.getCode().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
