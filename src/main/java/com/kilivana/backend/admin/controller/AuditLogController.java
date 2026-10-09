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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The immutable audit trail. Every state change an administrator makes — suspend, activate,
 * approve, reject, delete — is recorded here with the actor, the entity it touched and the
 * metadata that explains what happened, so an operator can later answer "who did what and
 * when". Read-only: nothing here is ever created, updated or deleted through the API.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Fetches one audit log entry by its id.
     *
     * @param id the audit log id
     * @return the entry, or 404 if no such log exists
     */
    @Operation(
        summary = "Get an audit log by id",
        description = "Returns a single audit log entry."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit log found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No audit log with that id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable Long id) {
        AuditLogResponse auditLog = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(auditLog));
    }

    /**
     * Lists every audit log entry written by one actor.
     *
     * @param actorId the user id of the actor
     * @return all entries the actor wrote, newest first
     */
    @Operation(
        summary = "List audit logs by actor",
        description = "Returns every audit log entry written by the given actor."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No such actor")
    })
    @GetMapping("/actor/{actorId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByActor(
            @PathVariable Long actorId) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByActor(actorId);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    /**
     * Lists every audit log entry touching one entity, e.g. all the approvals and rejections
     * recorded against a single farmer account.
     *
     * @param entityType the kind of entity, e.g. "USER" or "ORDER"
     * @param entityId   the entity's id
     * @return all entries for that entity, newest first
     */
    @Operation(
        summary = "List audit logs by entity",
        description = "Returns every audit log entry touching the given entity type and id."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No such entity")
    })
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    /**
     * Searches the audit trail, filtering by actor, entity type, action and a date range.
     * Every filter is optional; omitting one means "do not filter on it".
     *
     * @param actorId    restrict to one actor
     * @param entityType restrict to one kind of entity
     * @param action     restrict to one action, e.g. "SUSPEND"
     * @param startDate  only entries on or after this date
     * @param endDate    only entries on or before this date
     * @param pageable   pagination and sorting
     * @return a page of matching audit logs
     */
    @Operation(
        summary = "Search audit logs",
        description = "Filters the audit trail by actor, entity type, action and date range. "
                + "All filters are optional; omit any to leave it unconstrained."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date or filter format")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Pageable pageable) {
        Page<AuditLogResponse> auditLogs = auditLogService.searchAuditLogs(
                actorId, entityType, action, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }
}