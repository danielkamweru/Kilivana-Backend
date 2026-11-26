package com.kilivana.backend.finance.entity;

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
 * A journal entry representing a balanced financial transaction.
 * Every entry must have total debits = total credits.
 * Entries are immutable once posted.
 */
@Entity
@Table(name = "journal_entries",
        indexes = {
                @Index(name = "idx_journal_entry_reference", columnList = "reference", unique = true),
                @Index(name = "idx_journal_entry_date", columnList = "entry_date"),
                @Index(name = "idx_journal_entry_source", columnList = "source_type, source_id"),
                @Index(name = "idx_journal_entry_posted", columnList = "posted")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /** Unique human-readable reference (e.g., JE-20260115-001). */
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    /** Date of the financial event. */
    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;

    /** Description of the transaction. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Type of source transaction (e.g., PAYMENT, PAYOUT, REFUND, ADJUSTMENT). */
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    /** ID of the source entity (Payment.id, SellerPayout.id, RefundTransaction.id, etc.). */
    @Column(name = "source_id")
    private Long sourceId;

    /** Actor who created this entry (user ID or SYSTEM). */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /** Whether this entry has been posted (balanced and committed). */
    @Column(nullable = false)
    @Builder.Default
    private boolean posted = false;

    /** If posted, when it was posted. */
    private LocalDateTime postedAt;

    /** Reversal entry ID if this entry was reversed. */
    @Column(name = "reversal_of")
    private Long reversalOf;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (entryDate == null) {
            entryDate = LocalDateTime.now();
        }
    }
}