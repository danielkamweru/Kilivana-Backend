package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Financial settlement for a specific shipment.
 * Tracks the complete financial lifecycle from payment to seller/transport provider payout.
 */
@Entity
@Table(name = "settlements",
        indexes = {
                @Index(name = "idx_settlement_shipment", columnList = "shipment_id"),
                @Index(name = "idx_settlement_order", columnList = "order_id"),
                @Index(name = "idx_settlement_seller", columnList = "seller_id"),
                @Index(name = "idx_settlement_transport_provider", columnList = "transport_provider_id"),
                @Index(name = "idx_settlement_status", columnList = "status"),
                @Index(name = "idx_settlement_reference", columnList = "settlement_reference"),
                @Index(name = "idx_settlement_created", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /** The shipment this settlement belongs to. */
    @Column(name = "shipment_id", nullable = false, unique = true)
    private Long shipmentId;

    /** The parent order. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** The seller (farmer/supplier) for this settlement. */
    @Column(name = "seller_id")
    private Long sellerId;

    /** The buyer for this settlement. */
    @Column(name = "buyer_id")
    private Long buyerId;

    /** The driver assigned to the shipment. */
    @Column(name = "driver_id")
    private Long driverId;

    /** The transport provider company/user. */
    @Column(name = "transport_provider_id")
    private Long transportProviderId;

    /** Settlement status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING_PAYMENT;

    // ==================== FINANCIAL AMOUNTS ====================

    /** Product subtotal for this shipment's seller. */
    @Column(name = "product_subtotal", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal productSubtotal = BigDecimal.ZERO;

    /** Transport fee collected for this shipment. */
    @Column(name = "transport_fee", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal transportFee = BigDecimal.ZERO;

    /** Inspection fee collected for this shipment. */
    @Column(name = "inspection_fee", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal inspectionFee = BigDecimal.ZERO;

    /** Platform commission/fee. */
    @Column(name = "platform_fee", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    /** Seller's net earnings after commission. */
    @Column(name = "seller_net_earnings", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal sellerNetEarnings = BigDecimal.ZERO;

    /** Transport provider's payable amount. */
    @Column(name = "transport_provider_payable", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal transportProviderPayable = BigDecimal.ZERO;

    /** Platform's revenue from this settlement. */
    @Column(name = "platform_revenue", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal platformRevenue = BigDecimal.ZERO;

    /** Total amount collected from buyer. */
    @Column(name = "total_collected", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalCollected = BigDecimal.ZERO;

    // ==================== FEE CALCULATION DETAILS ====================

    /** Platform fee calculation type: PERCENTAGE or FIXED. */
    @Column(name = "platform_fee_type", nullable = false, length = 16)
    @Builder.Default
    private String platformFeeType = "PERCENTAGE";

    /** Platform fee value (percentage or fixed amount). */
    @Column(name = "platform_fee_value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal platformFeeValue = BigDecimal.ZERO;

    /** Who bears the platform fee: SELLER or BUYER. */
    @Column(name = "platform_fee_charged_to", nullable = false, length = 16)
    @Builder.Default
    private String platformFeeChargedTo = "SELLER";

    /** Transport fee calculation type: FIXED or PER_KM. */
    @Column(name = "transport_fee_type", nullable = false, length = 16)
    @Builder.Default
    private String transportFeeType = "FIXED";

    /** Transport fee per km (if PER_KM). */
    @Column(name = "transport_fee_per_km", precision = 10, scale = 2)
    private BigDecimal transportFeePerKm;

    /** Minimum transport fee. */
    @Column(name = "transport_fee_min", precision = 10, scale = 2)
    private BigDecimal transportFeeMin;

    /** Maximum transport fee. */
    @Column(name = "transport_fee_max", precision = 10, scale = 2)
    private BigDecimal transportFeeMax;

    // ==================== TIMESTAMPS ====================

    /** When payment was confirmed. */
    @Column(name = "payment_confirmed_at")
    private LocalDateTime paymentConfirmedAt;

    /** When delivery was confirmed via OTP. */
    @Column(name = "delivery_confirmed_at")
    private LocalDateTime deliveryConfirmedAt;

    /** When settlement processing started. */
    @Column(name = "settlement_processed_at")
    private LocalDateTime settlementProcessedAt;

    /** When settlement was completed. */
    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    /** When settlement failed. */
    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    /** Settlement description/notes. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ==================== REFERENCES ====================

    /** The payment that funded this settlement. */
    @Column(name = "payment_id")
    private Long paymentId;

    /** Reference to the OTP verification that confirmed delivery. */
    @Column(name = "otp_verification_id")
    private Long otpVerificationId;

    /** Unique settlement reference for reconciliation. */
    @Column(name = "settlement_reference", nullable = false, unique = true, length = 64)
    private String settlementReference;

    // ==================== AUDIT ====================

    /** Admin/user who created/processed this settlement. */
    @Column(name = "created_by")
    private Long createdBy;

    /** Failure reason if settlement failed. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Number of retry attempts for settlement processing. */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Allocation breakdown for this settlement. */
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SettlementAllocation> allocations = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (settlementReference == null) {
            settlementReference = "SETL-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        }
    }

    // ==================== HELPER METHODS ====================

    public BigDecimal getTotalPayable() {
        return sellerNetEarnings.add(transportProviderPayable).add(platformRevenue);
    }

    /** Gross amount (product subtotal + transport fee + inspection fee). */
    public BigDecimal getGrossAmount() {
        return productSubtotal.add(transportFee).add(inspectionFee);
    }

    /** Commission amount (same as platform fee). */
    public BigDecimal getCommissionAmount() {
        return platformFee;
    }

    /** Transport fee amount. */
    public BigDecimal getTransportFeeAmount() {
        return transportFee;
    }

    /** Inspection fee amount. */
    public BigDecimal getInspectionFeeAmount() {
        return inspectionFee;
    }

    /** Total amount paid by buyer. */
    public BigDecimal getBuyerPaidAmount() {
        return totalCollected;
    }

    /** Settlement date (uses creation timestamp). */
    public LocalDateTime getSettlementDate() {
        return createdAt;
    }

    public boolean isTerminal() {
        return status == SettlementStatus.SETTLED
                || status == SettlementStatus.REFUNDED
                || status == SettlementStatus.FAILED;
    }

    public boolean canProcess() {
        return status == SettlementStatus.DELIVERY_CONFIRMED
                || status == SettlementStatus.SETTLEMENT_PROCESSING;
    }
}