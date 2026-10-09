package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing an audit log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit log entry response")
public class AuditLogResponse {

    @Schema(description = "Unique identifier of the audit log entry", example = "1")
    private Long id;

    @Schema(description = "ID of the actor who performed the action", example = "5")
    private Long actorId;

    @Schema(description = "Action performed", example = "SUPPLIER_SUSPENDED")
    private String action;

    @Schema(description = "Type of entity affected", example = "Supplier")
    private String entityType;

    @Schema(description = "ID of the affected entity", example = "12")
    private Long entityId;

    @Schema(description = "Additional metadata as JSON string", example = "{\"reason\":\"Contract expired\"}")
    private String metadata;

    @Schema(description = "Timestamp when the action was performed", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    public static AuditLogResponse fromEntity(com.kilivana.backend.admin.entity.AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorId(auditLog.getActorId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .metadata(auditLog.getMetadata())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
