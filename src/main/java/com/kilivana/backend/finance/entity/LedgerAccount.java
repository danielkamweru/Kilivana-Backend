package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.LedgerAccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A ledger account in the double-entry financial system.
 * Each account has a type and tracks its running balance.
 * Seller-specific payable accounts are created dynamically per seller.
 */
@Entity
@Table(name = "ledger_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_type", "seller_id"}),
        indexes = {
                @Index(name = "idx_ledger_account_type", columnList = "account_type"),
                @Index(name = "idx_ledger_account_seller", columnList = "seller_id"),
                @Index(name = "idx_ledger_account_active", columnList = "active")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private LedgerAccountType accountType;

    /** Seller ID for seller-specific sub-accounts (e.g., SELLER_PAYABLES per seller). Null for platform-level accounts. */
    private Long sellerId;

    /** Human-readable name. */
    @Column(nullable = false)
    private String name;

    /** Running balance. Positive = debit for asset/expense accounts, credit for liability/revenue accounts. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    /** Whether this account is active. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Description of the account's purpose. */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
    }
}