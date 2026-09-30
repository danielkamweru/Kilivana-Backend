package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByActorId(Long actorId);
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    /**
     * Optional-filter search. The parameters in each {@code IS NULL} branch are cast
     * explicitly: without a cast PostgreSQL cannot infer the type of a parameter that
     * only appears in a null check, and rejects the statement with
     * "could not determine data type of parameter".
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(CAST(:actorId AS Long) IS NULL OR a.actorId = :actorId) AND " +
           "(CAST(:entityType AS String) IS NULL OR a.entityType = :entityType) AND " +
           "(CAST(:action AS String) IS NULL OR a.action = :action) AND " +
           "(CAST(:startDate AS java.time.LocalDateTime) IS NULL OR a.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS java.time.LocalDateTime) IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(@Param("actorId") Long actorId,
                                   @Param("entityType") String entityType,
                                   @Param("action") String action,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);
}
