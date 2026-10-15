package com.kilivana.backend.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single line of an order: what was bought, at the price it was bought at.
 *
 * <p>The {@code productName} and {@code unit} are copied from the product at
 * purchase time so the order remains readable even if the product is later
 * renamed or removed. {@code subtotal} is {@code quantity * unitPrice}.
 */
@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    // Preserve canonical scalar writes while retaining the upstream read-only relationship.
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long sellerId;

    /** Copied from the product at purchase, so the order keeps reading
     *  correctly if the product is renamed or removed afterwards. */
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Explicit getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static OrderItemBuilder builder() {
        return new OrderItemBuilder();
    }

    public static class OrderItemBuilder {
        private Long id;
        private Order order;
        private Long orderId;
        private Long productId;
        private Long sellerId;
        private String productName;
        private String unit;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private LocalDateTime createdAt;

        public OrderItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderItemBuilder order(Order order) {
            this.order = order;
            return this;
        }

        public OrderItemBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderItemBuilder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public OrderItemBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public OrderItemBuilder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public OrderItemBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public OrderItemBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderItemBuilder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public OrderItemBuilder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public OrderItemBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderItem build() {
            OrderItem orderItem = new OrderItem();
            orderItem.id = this.id;
            orderItem.order = this.order;
            orderItem.orderId = this.orderId;
            orderItem.productId = this.productId;
            orderItem.sellerId = this.sellerId;
            orderItem.productName = this.productName;
            orderItem.unit = this.unit;
            orderItem.quantity = this.quantity;
            orderItem.unitPrice = this.unitPrice;
            orderItem.subtotal = this.subtotal;
            orderItem.createdAt = this.createdAt;
            return orderItem;
        }
    }
}
