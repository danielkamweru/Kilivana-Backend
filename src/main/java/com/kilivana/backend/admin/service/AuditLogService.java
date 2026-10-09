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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
    @RequiredArgsConstructor
    public class AuditLogService {

    /**
     * Read-only access to the immutable audit trail.
     *
     * <p>Audit logs are written by the {@code @EnableJpaAuditing} listener on every entity
     * change; this service only exposes them. No write methods exist because the trail must
     * never be altered.
     */

    private final AuditLogRepository auditLogRepository;

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
