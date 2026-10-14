package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.NotificationResponse;
import com.kilivana.backend.admin.entity.Notification;
import com.kilivana.backend.admin.repository.NotificationRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@PreAuthorize("@resourceAuthorization.admin()")
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse createNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    @PreAuthorize("@resourceAuthorization.notification(#id)")
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        return NotificationResponse.fromEntity(notification);
    }

    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadAtIsNull(userId).stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Every user's notifications, newest first, for the admin feed. Sorted in the database
     * rather than in memory so the ordering does not depend on insertion order.
     */
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.notification(#id)")
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setReadAt(java.time.LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadAtIsNull(userId);
        unread.forEach(n -> n.setReadAt(java.time.LocalDateTime.now()));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.notification(#id)")
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification", id);
        }
        notificationRepository.deleteById(id);
    }
}
