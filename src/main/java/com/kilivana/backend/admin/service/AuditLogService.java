package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.AuditLogResponse;
import com.kilivana.backend.admin.entity.AuditLog;
import com.kilivana.backend.admin.repository.AuditLogRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
    @RequiredArgsConstructor
    public class AuditLogService {

    /**
     * Read-only access to the immutable audit trail, plus a write method for
     * recording administrative actions.
     */

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an administrative action in the audit trail.
     * This is the only write method — audit records are never updated or deleted.
     *
     * @param actorId    the administrator who performed the action
     * @param action     the action type (e.g. UPDATE_PLATFORM_SETTINGS)
     * @param entityType the type of entity affected (e.g. PlatformSetting)
     * @param entityId   the ID of the affected entity
     * @param metadata   optional JSON or text metadata (no secrets)
     */
    @Transactional
    public AuditLog log(Long actorId, String action, String entityType, Long entityId, String metadata) {
        AuditLog entry = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();
        return auditLogRepository.save(entry);
    }

    public AuditLogResponse getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id));
        return AuditLogResponse.fromEntity(auditLog);
    }

    public List<AuditLogResponse> getAuditLogsByActor(Long actorId) {
        return auditLogRepository.findByActorId(actorId).stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<AuditLogResponse> searchAuditLogs(Long actorId, String entityType, String action,
                                                   LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(actorId, entityType, action, startDate, endDate, pageable)
                .map(AuditLogResponse::fromEntity);
    }
}
