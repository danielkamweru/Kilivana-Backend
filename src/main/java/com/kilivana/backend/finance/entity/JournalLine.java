package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.LedgerAccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single line (debit or credit) within a journal entry.
 * Each line references a ledger account and an amount.
 * The sum of debits must equal the sum of credits in the parent journal entry.
 */
@Entity
@Table(name = "journal_lines",
        indexes = {
                @Index(name = "idx_journal_line_entry", columnList = "journal_entry_id"),
                @Index(name = "idx_journal_line_account", columnList = "account_id"),
                @Index(name = "idx_journal_line_seller", columnList = "seller_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(name = "journal_entry_id", nullable = false, insertable = false, updatable = false)
    private Long journalEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;

    @Column(name = "account_id", nullable = false, insertable = false, updatable = false)
    private Long accountId;

    /** The account type at the time of posting (for reporting without joins). */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private LedgerAccountType accountType;

    /** Seller ID for seller-specific lines (e.g., SELLER_PAYABLES per seller). */
    private Long sellerId;

    /** Debit amount (positive). */
    @Column(name = "debit_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /** Credit amount (positive). */
    @Column(name = "credit_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    /** Description specific to this line. */
    @Column(columnDefinition = "TEXT")
    private String description;

    public boolean isDebit() {
        return debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCredit() {
        return creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getNetAmount() {
        BigDecimal debit = debitAmount == null ? BigDecimal.ZERO : debitAmount;
        BigDecimal credit = creditAmount == null ? BigDecimal.ZERO : creditAmount;
        return debit.subtract(credit);
    }
}