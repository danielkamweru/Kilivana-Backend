package com.kilivana.backend.finance.entity;

import com.kilivana.backend.common.enums.PaymentStatus;
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
 * A refund transaction linked to a payment and seller fulfilment.
 * Tracks the refund lifecycle from request to provider confirmation.
 */
@Entity
@Table(name = "refund_transactions",
        indexes = {
                @Index(name = "idx_refund_payment", columnList = "payment_id"),
                @Index(name = "idx_refund_order", columnList = "order_id"),
                @Index(name = "idx_refund_fulfilment", columnList = "seller_fulfilment_id"),
                @Index(name = "idx_refund_seller", columnList = "seller_id"),
                @Index(name = "idx_refund_buyer", columnList = "buyer_id"),
                @Index(name = "idx_refund_status", columnList = "status"),
                @Index(name = "idx_refund_reference", columnList = "reference", unique = true),
                @Index(name = "idx_refund_provider_ref", columnList = "provider_reference"),
                @Index(name = "idx_refund_created", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** Seller fulfilment being refunded (for partial refunds). Null for full order refunds. */
    @Column(name = "seller_fulfilment_id")
    private Long sellerFulfilmentId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    /** Refund amount. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** Reason for refund. */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Refund status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** Unique reference (e.g., RF-20260115-001). */
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    /** M-Pesa/refund provider reference. */
    @Column(name = "provider_reference", length = 64)
    private String providerReference;

    /** Provider result code. */
    @Column(name = "provider_result_code")
    private Integer providerResultCode;

    /** Provider result description. */
    @Column(name = "provider_result_desc", columnDefinition = "TEXT")
    private String providerResultDesc;

    /** Admin who approved the refund. */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Rejection reason (if rejected). */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** When refund was completed. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
}