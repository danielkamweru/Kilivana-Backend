package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to the {@link Notification} table. Notifications are ordered by creation date in the
 * database rather than in memory, so the ordering does not depend on insertion order. The
 * paginated variant exists for the admin feed, which can grow large.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndReadAtIsNull(Long userId);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
