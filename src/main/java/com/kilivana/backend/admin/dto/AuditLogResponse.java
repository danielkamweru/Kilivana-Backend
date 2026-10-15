package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO representing an audit log entry.
 */
/**
 * Response DTO for one audit log entry. The {@code metadata} field is a free JSON string that
 * explains what happened — the suspension reason, the before/after values, whatever the action
 * needed to record.
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static AuditLogResponseBuilder builder() {
        return new AuditLogResponseBuilder();
    }

    public static class AuditLogResponseBuilder {
        private Long id;
        private Long actorId;
        private String action;
        private String entityType;
        private Long entityId;
        private String metadata;
        private LocalDateTime createdAt;

        public AuditLogResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuditLogResponseBuilder actorId(Long actorId) {
            this.actorId = actorId;
            return this;
        }

        public AuditLogResponseBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditLogResponseBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AuditLogResponseBuilder entityId(Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditLogResponseBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public AuditLogResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditLogResponse build() {
            AuditLogResponse auditLogResponse = new AuditLogResponse();
            auditLogResponse.id = this.id;
            auditLogResponse.actorId = this.actorId;
            auditLogResponse.action = this.action;
            auditLogResponse.entityType = this.entityType;
            auditLogResponse.entityId = this.entityId;
            auditLogResponse.metadata = this.metadata;
            auditLogResponse.createdAt = this.createdAt;
            return auditLogResponse;
        }
    }

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
