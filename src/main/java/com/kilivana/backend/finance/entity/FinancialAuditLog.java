package com.kilivana.backend.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for sensitive financial operations.
 * Immutable record of who did what, when, and with what parameters.
 */
@Entity
@Table(name = "financial_audit_logs",
        indexes = {
                @Index(name = "idx_financial_audit_actor", columnList = "actor_id"),
                @Index(name = "idx_financial_audit_action", columnList = "action"),
                @Index(name = "idx_financial_audit_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_financial_audit_created", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /** User who performed the action (null for system actions). */
    private Long actorId;

    /** Action performed (e.g., WALLET_CREDIT, PAYOUT_APPROVED, LEDGER_POSTED, REFUND_PROCESSED). */
    @Column(nullable = false, length = 64)
    private String action;

    /** Entity type affected (e.g., SellerWallet, SellerPayout, JournalEntry). */
    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    /** Entity ID affected. */
    @Column(name = "entity_id")
    private Long entityId;

    /** Human-readable description. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** JSON snapshot of relevant parameters (amounts, IDs, etc.). */
    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters;

    /** IP address of the actor (for security audit). */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** User agent of the actor. */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}