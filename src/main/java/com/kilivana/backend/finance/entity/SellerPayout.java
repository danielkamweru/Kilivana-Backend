package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.PayoutMethod;
import com.kilivana.backend.finance.enums.PayoutStatus;
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
 * A seller's withdrawal/payout request.
 * Goes through admin approval then M-Pesa B2C processing.
 */
@Entity
@Table(name = "seller_payouts",
        indexes = {
                @Index(name = "idx_seller_payout_seller", columnList = "seller_id"),
                @Index(name = "idx_seller_payout_status", columnList = "status"),
                @Index(name = "idx_seller_payout_wallet", columnList = "wallet_id"),
                @Index(name = "idx_seller_payout_account", columnList = "payout_account_id"),
                @Index(name = "idx_seller_payout_reference", columnList = "reference", unique = true),
                @Index(name = "idx_seller_payout_provider_ref", columnList = "provider_reference"),
                @Index(name = "idx_seller_payout_created", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "payout_account_id", nullable = false)
    private Long payoutAccountId;

    /** Amount requested by seller. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false)
    private PayoutMethod payoutMethod;

    /** Destination details snapshot at time of request. */
    @Column(name = "destination_snapshot", columnDefinition = "TEXT")
    private String destinationSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status;

    /** Unique reference for idempotency (e.g., PO-20260115-001). */
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    /** Admin who approved/rejected. */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Rejection reason (if rejected). */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** M-Pesa B2C conversation ID / provider reference. */
    @Column(name = "provider_reference", length = 64)
    private String providerReference;

    /** M-Pesa B2C result code. */
    @Column(name = "provider_result_code")
    private Integer providerResultCode;

    /** M-Pesa B2C result description. */
    @Column(name = "provider_result_desc", columnDefinition = "TEXT")
    private String providerResultDesc;

    /** M-Pesa transaction receipt number. */
    @Column(name = "mpesa_receipt_number", length = 32)
    private String mpesaReceiptNumber;

    /** When the payout was actually paid. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Number of payout attempts. */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    /** Last attempt timestamp. */
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    /** Idempotency key for B2C request (to prevent duplicate submissions). */
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean canRetry() {
        return status == PayoutStatus.FAILED && attemptCount < 3;
    }
}