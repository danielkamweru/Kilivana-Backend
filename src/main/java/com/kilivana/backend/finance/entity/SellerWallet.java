package com.kilivana.backend.finance.entity;

import com.kilivana.backend.common.enums.UserRole;
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
 * A seller's wallet tracking earnings from sales.
 * Balances are backed by immutable wallet transactions.
 * One wallet per eligible seller (farmer or supplier).
 */
@Entity
@Table(name = "seller_wallets",
        uniqueConstraints = @UniqueConstraint(columnNames = "seller_id"),
        indexes = {
                @Index(name = "idx_seller_wallet_seller", columnList = "seller_id"),
                @Index(name = "idx_seller_wallet_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /** Seller (farmer or supplier) user ID. */
    @Column(name = "seller_id", nullable = false, unique = true)
    private Long sellerId;

    /** Seller role at wallet creation (FARMER or SUPPLIER). */
    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", nullable = false)
    private UserRole sellerType;

    /** Earnings awaiting delivery confirmation. */
    @Column(name = "pending_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    /** Earnings confirmed and available for withdrawal. */
    @Column(name = "available_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /** Funds reserved for in-progress withdrawals. */
    @Column(name = "processing_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal processingBalance = BigDecimal.ZERO;

    /** Total amount ever paid out to this seller. */
    @Column(name = "total_paid", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /** Total lifetime earnings (credited). */
    @Column(name = "total_earned", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    /** Wallet status. */
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE";

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
    }

    public BigDecimal getTotalBalance() {
        return pendingBalance.add(availableBalance).add(processingBalance);
    }

    public boolean canWithdraw(BigDecimal amount) {
        return "ACTIVE".equals(status)
                && availableBalance.compareTo(amount) >= 0;
    }
}