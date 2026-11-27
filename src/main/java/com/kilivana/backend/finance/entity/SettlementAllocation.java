package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.SettlementAllocationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed breakdown of a settlement into individual allocations.
 * Each allocation represents a financial flow to a specific recipient.
 */
@Entity
@Table(name = "settlement_allocations",
        indexes = {
                @Index(name = "idx_settlement_allocation_settlement", columnList = "settlement_id"),
                @Index(name = "idx_settlement_allocation_type", columnList = "allocation_type"),
                @Index(name = "idx_settlement_allocation_recipient", columnList = "recipient_user_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "settlement_id", nullable = false, insertable = false, updatable = false)
    private Long settlementId;

    /** Type of allocation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", nullable = false, length = 32)
    private SettlementAllocationType allocationType;

    /** Allocation amount (always positive). */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** The recipient of this allocation (seller or transport provider). */
    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    /** Reference to the wallet transaction that executed this allocation. */
    @Column(name = "wallet_transaction_id")
    private Long walletTransactionId;

    /** Reference to the journal entry for double-entry bookkeeping. */
    @Column(name = "ledger_journal_entry_id")
    private Long ledgerJournalEntryId;

    /** Human-readable description. */
    @Column(columnDefinition = "TEXT")
    private String description;

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