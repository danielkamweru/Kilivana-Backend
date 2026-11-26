package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.FinancialAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for financial audit logs.
 */
@Repository
public interface FinancialAuditLogRepository extends JpaRepository<FinancialAuditLog, Long> {

    Page<FinancialAuditLog> findByActorId(Long actorId, Pageable pageable);

    Page<FinancialAuditLog> findByAction(String action, Pageable pageable);

    Page<FinancialAuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<FinancialAuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT f FROM FinancialAuditLog f WHERE f.entityType = :entityType AND f.entityId = :entityId ORDER BY f.createdAt DESC")
    List<FinancialAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    Optional<FinancialAuditLog> findByUuid(UUID uuid);
}