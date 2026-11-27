package com.kilivana.backend.finance.service;

import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.entity.FinancialAuditLog;
import com.kilivana.backend.finance.repository.FinancialAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for immutable financial audit logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialAuditLogService {

    private final FinancialAuditLogRepository auditLogRepository;

    @Transactional
    public FinancialAuditLog log(Long actorId, String action, String entityType, Long entityId, String description) {
        FinancialAuditLog log = FinancialAuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        return auditLogRepository.save(log);
    }

    @Transactional
    public FinancialAuditLog logWithParams(Long actorId, String action, String entityType, Long entityId,
                                           String description, String parameters, String clientIp, String userAgent) {
        FinancialAuditLog log = FinancialAuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .parameters(parameters)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.FinancialAuditLogResponse> getByActorId(Long actorId) {
        return auditLogRepository.findByActorId(actorId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(FinanceDtos.FinancialAuditLogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.FinancialAuditLogResponse> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(FinanceDtos.FinancialAuditLogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.FinancialAuditLogResponse> getByAction(String action) {
        return auditLogRepository.findByAction(action, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(FinanceDtos.FinancialAuditLogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.FinancialAuditLogResponse> getByDateRange(LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByCreatedAtBetween(from, to, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(FinanceDtos.FinancialAuditLogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<FinancialAuditLog> findById(Long id) {
        return auditLogRepository.findById(id);
    }
}