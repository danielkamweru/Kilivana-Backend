package com.kilivana.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String action;
    private String entityType;
    private Long entityId;
    private String metadata;
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
