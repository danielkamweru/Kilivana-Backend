package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.NotificationPreference;
import com.kilivana.backend.admin.enums.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByEventKey(NotificationEvent eventKey);
}