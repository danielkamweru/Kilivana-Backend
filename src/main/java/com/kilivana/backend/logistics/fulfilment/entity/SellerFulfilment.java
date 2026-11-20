package com.kilivana.backend.logistics.fulfilment.entity;

import com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One farmer's fulfilment portion of a multi-farmer customer order.
 * <p>
 * A {@link com.kilivana.backend.ecommerce.entity.Order} with products from several
 * farmers produces one {@code SellerFulfilment} per distinct {@code sellerId}.
 * Each record tracks only that farmer's items and preparation status.
 * <p>
 * Creation is idempotent: re-creating for the same (order, seller) pair returns
 * the existing record instead of duplicating it.
 */
@Entity
@Table(name = "seller_fulfilments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "seller_id"}),
        indexes = {
                @Index(name = "idx_seller_fulfilment_order", columnList = "order_id"),
                @Index(name = "idx_seller_fulfilment_seller", columnList = "seller_id"),
                @Index(name = "idx_seller_fulfilment_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerFulfilment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long sellerId;

    /** Total value of this seller's items within the order (read-only snapshot). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellerSubtotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfilmentStatus status;

    /** Free-text note from the farmer, e.g. "produce packed in crates". */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** When the farmer marked the products ready for collection. */
    private LocalDateTime readyAt;

    /** When the driver collected the products. */
    private LocalDateTime collectedAt;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public BigDecimal getSellerSubtotal() {
        return sellerSubtotal;
    }

    public void setSellerSubtotal(BigDecimal sellerSubtotal) {
        this.sellerSubtotal = sellerSubtotal;
    }

    public FulfilmentStatus getStatus() {
        return status;
    }

    public void setStatus(FulfilmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(LocalDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ShipmentItem> getShipmentItems() {
        return shipmentItems;
    }

    public void setShipmentItems(List<ShipmentItem> shipmentItems) {
        this.shipmentItems = shipmentItems;
    }

    public static SellerFulfilmentBuilder builder() {
        return new SellerFulfilmentBuilder();
    }

    public static class SellerFulfilmentBuilder {
        private Long id;
        private Long orderId;
        private Long sellerId;
        private BigDecimal sellerSubtotal;
        private FulfilmentStatus status;
        private String notes;
        private LocalDateTime readyAt;
        private LocalDateTime collectedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public SellerFulfilmentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SellerFulfilmentBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public SellerFulfilmentBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public SellerFulfilmentBuilder sellerSubtotal(BigDecimal sellerSubtotal) {
            this.sellerSubtotal = sellerSubtotal;
            return this;
        }

        public SellerFulfilmentBuilder status(FulfilmentStatus status) {
            this.status = status;
            return this;
        }

        public SellerFulfilmentBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public SellerFulfilmentBuilder readyAt(LocalDateTime readyAt) {
            this.readyAt = readyAt;
            return this;
        }

        public SellerFulfilmentBuilder collectedAt(LocalDateTime collectedAt) {
            this.collectedAt = collectedAt;
            return this;
        }

        public SellerFulfilmentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SellerFulfilmentBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SellerFulfilment build() {
            SellerFulfilment fulfilment = new SellerFulfilment();
            fulfilment.id = this.id;
            fulfilment.orderId = this.orderId;
            fulfilment.sellerId = this.sellerId;
            fulfilment.sellerSubtotal = this.sellerSubtotal;
            fulfilment.status = this.status;
            fulfilment.notes = this.notes;
            fulfilment.readyAt = this.readyAt;
            fulfilment.collectedAt = this.collectedAt;
            fulfilment.createdAt = this.createdAt;
            fulfilment.updatedAt = this.updatedAt;
            return fulfilment;
        }
    }
}
