package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.WalletTransactionStatus;
import com.kilivana.backend.finance.enums.WalletTransactionType;
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
 * Immutable record of a financial transaction affecting a seller's wallet.
 * Every balance change must have a corresponding wallet transaction.
 * Never deleted; corrections use reversal or adjustment transactions.
 */
@Entity
@Table(name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_txn_wallet", columnList = "wallet_id"),
                @Index(name = "idx_wallet_txn_seller", columnList = "seller_id"),
                @Index(name = "idx_wallet_txn_type", columnList = "transaction_type"),
                @Index(name = "idx_wallet_txn_status", columnList = "status"),
                @Index(name = "idx_wallet_txn_reference", columnList = "reference", unique = true),
                @Index(name = "idx_wallet_txn_order", columnList = "order_id"),
                @Index(name = "idx_wallet_txn_fulfilment", columnList = "seller_fulfilment_id"),
                @Index(name = "idx_wallet_txn_created", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /** Related parent order (if from a sale). */
    private Long orderId;

    /** Related seller fulfilment (if from a sale). */
    @Column(name = "seller_fulfilment_id")
    private Long sellerFulfilmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private WalletTransactionType transactionType;

    /** Amount (always positive). Direction is implied by transaction type. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionStatus status;

    /** Unique reference for idempotency and reconciliation. */
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    /** Human-readable description. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Running balance after this transaction (snapshot for audit). */
    @Column(name = "balance_after", precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    /** Pending balance after this transaction (snapshot for audit). */
    @Column(name = "pending_balance_after", precision = 18, scale = 2)
    private BigDecimal pendingBalanceAfter;

    /** Available balance after this transaction (snapshot for audit). */
    @Column(name = "available_balance_after", precision = 18, scale = 2)
    private BigDecimal availableBalanceAfter;

    /** Processing balance after this transaction (snapshot for audit). */
    @Column(name = "processing_balance_after", precision = 18, scale = 2)
    private BigDecimal processingBalanceAfter;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public BigDecimal getSignedAmount() {
        // Types that increase available/pending balance
        if (transactionType == WalletTransactionType.SALE_CREDIT
                || transactionType == WalletTransactionType.PENDING_TO_AVAILABLE
                || transactionType == WalletTransactionType.WITHDRAWAL_REVERSAL
                || transactionType == WalletTransactionType.ADJUSTMENT) {
            return amount;
        }
        // Types that decrease available/pending balance
        return amount.negate();
    }
}