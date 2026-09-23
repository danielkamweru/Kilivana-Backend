package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AuditLogResponse;
import com.kilivana.backend.admin.service.AuditLogService;
import com.kilivana.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable Long id) {
        AuditLogResponse auditLog = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(auditLog));
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByActor(@PathVariable Long actorId) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByActor(actorId);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByEntity(@PathVariable String entityType, @PathVariable Long entityId) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable) {
        Page<AuditLogResponse> auditLogs = auditLogService.searchAuditLogs(actorId, entityType, action, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }
}
