package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AuditLogResponse;
import com.kilivana.backend.admin.dto.PlatformSettingRequest;
import com.kilivana.backend.admin.dto.PlatformSettingResponse;
import com.kilivana.backend.admin.service.AuditLogService;
import com.kilivana.backend.admin.service.PlatformSettingService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Administration · Settings", description = "Platform settings and audit log retrieval")
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class PlatformSettingController {

    private final PlatformSettingService settingService;
    private final AuditLogService auditLogService;

    @Operation(summary = "Get platform settings",
            description = "Returns the current platform settings and notification preferences.")
    @GetMapping
    public ResponseEntity<ApiResponse<PlatformSettingResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success("Platform settings retrieved successfully",
                settingService.getSettings()));
    }

    @Operation(summary = "Update platform settings",
            description = "Updates platform settings and notification preferences. "
                    + "Records an audit log entry with the authenticated admin's identity.")
    @PutMapping
    public ResponseEntity<ApiResponse<PlatformSettingResponse>> updateSettings(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody PlatformSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Platform settings updated successfully",
                settingService.updateSettings(request, adminId)));
    }

    @Operation(summary = "List audit logs",
            description = "Returns paginated audit log entries, newest first.")
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.searchAuditLogs(
                null, null, null, null, null, pageable)));
    }

    @Operation(summary = "Get an audit log by id",
            description = "Returns a single audit log entry.")
    @GetMapping("/audit-logs/{id}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getAuditLogById(id)));
    }
}