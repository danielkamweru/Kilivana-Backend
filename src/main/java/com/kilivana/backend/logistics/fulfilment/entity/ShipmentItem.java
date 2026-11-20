package com.kilivana.backend.logistics.fulfilment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Links a shipment to an original order item with an allocated quantity.
 * <p>
 * Every allocated quantity must originate from an existing paid OrderItem, and
 * allocated quantities must never exceed the quantity purchased. The same
 * quantity cannot be allocated to two active shipments.
 */
@Entity
@Table(name = "shipment_items",
        indexes = {
                @Index(name = "idx_shipment_item_shipment", columnList = "shipment_id"),
                @Index(name = "idx_shipment_item_order_item", columnList = "order_item_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(nullable = false)
    private Long orderItemId;

    @Column(nullable = false)
    private Integer allocatedQuantity;

    /** Unit price snapshot from the original order item. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)
    private Shipment shipment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Integer allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public static ShipmentItemBuilder builder() {
        return new ShipmentItemBuilder();
    }

    public static class ShipmentItemBuilder {
        private Long id;
        private Long shipmentId;
        private Long orderItemId;
        private Integer allocatedQuantity;
        private BigDecimal unitPrice;
        private LocalDateTime createdAt;
        private Shipment shipment;

        public ShipmentItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ShipmentItemBuilder shipmentId(Long shipmentId) {
            this.shipmentId = shipmentId;
            return this;
        }

        public ShipmentItemBuilder orderItemId(Long orderItemId) {
            this.orderItemId = orderItemId;
            return this;
        }

        public ShipmentItemBuilder allocatedQuantity(Integer allocatedQuantity) {
            this.allocatedQuantity = allocatedQuantity;
            return this;
        }

        public ShipmentItemBuilder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public ShipmentItemBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ShipmentItemBuilder shipment(Shipment shipment) {
            this.shipment = shipment;
            return this;
        }

        public ShipmentItem build() {
            ShipmentItem item = new ShipmentItem();
            item.id = this.id;
            item.shipmentId = this.shipmentId;
            item.orderItemId = this.orderItemId;
            item.allocatedQuantity = this.allocatedQuantity;
            item.unitPrice = this.unitPrice;
            item.createdAt = this.createdAt;
            item.shipment = this.shipment;
            return item;
        }
    }
}
