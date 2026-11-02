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

    @Column(name = "dispatch_location_id_snapshot")
    private Long dispatchLocationIdSnapshot;

    @Column(name = "dispatch_location_type_snapshot", length = 32)
    private String dispatchLocationTypeSnapshot;

    @Column(name = "dispatch_location_name_snapshot")
    private String dispatchLocationNameSnapshot;

    @Column(name = "dispatch_address_snapshot", columnDefinition = "TEXT")
    private String dispatchAddressSnapshot;

    @Column(name = "dispatch_county_snapshot", length = 100)
    private String dispatchCountySnapshot;

    @Column(name = "dispatch_latitude_snapshot")
    private Double dispatchLatitudeSnapshot;

    @Column(name = "dispatch_longitude_snapshot")
    private Double dispatchLongitudeSnapshot;

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
    public Long getDispatchLocationIdSnapshot() { return dispatchLocationIdSnapshot; }
    public void setDispatchLocationIdSnapshot(Long value) { this.dispatchLocationIdSnapshot = value; }
    public String getDispatchLocationTypeSnapshot() { return dispatchLocationTypeSnapshot; }
    public void setDispatchLocationTypeSnapshot(String value) { this.dispatchLocationTypeSnapshot = value; }
    public String getDispatchLocationNameSnapshot() { return dispatchLocationNameSnapshot; }
    public void setDispatchLocationNameSnapshot(String value) { this.dispatchLocationNameSnapshot = value; }
    public String getDispatchAddressSnapshot() { return dispatchAddressSnapshot; }
    public void setDispatchAddressSnapshot(String value) { this.dispatchAddressSnapshot = value; }
    public String getDispatchCountySnapshot() { return dispatchCountySnapshot; }
    public void setDispatchCountySnapshot(String value) { this.dispatchCountySnapshot = value; }
    public Double getDispatchLatitudeSnapshot() { return dispatchLatitudeSnapshot; }
    public void setDispatchLatitudeSnapshot(Double value) { this.dispatchLatitudeSnapshot = value; }
    public Double getDispatchLongitudeSnapshot() { return dispatchLongitudeSnapshot; }
    public void setDispatchLongitudeSnapshot(Double value) { this.dispatchLongitudeSnapshot = value; }
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
        private Long dispatchLocationIdSnapshot;
        private String dispatchLocationTypeSnapshot;
        private String dispatchLocationNameSnapshot;
        private String dispatchAddressSnapshot;
        private String dispatchCountySnapshot;
        private Double dispatchLatitudeSnapshot;
        private Double dispatchLongitudeSnapshot;
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

        public OrderItemBuilder dispatchLocationIdSnapshot(Long value) {
            this.dispatchLocationIdSnapshot = value;
            return this;
        }

        public OrderItemBuilder dispatchLocationTypeSnapshot(String value) {
            this.dispatchLocationTypeSnapshot = value;
            return this;
        }

        public OrderItemBuilder dispatchLocationNameSnapshot(String value) {
            this.dispatchLocationNameSnapshot = value;
            return this;
        }

        public OrderItemBuilder dispatchAddressSnapshot(String value) {
            this.dispatchAddressSnapshot = value;
            return this;
        }

        public OrderItemBuilder dispatchCountySnapshot(String value) {
            this.dispatchCountySnapshot = value;
            return this;
        }

        public OrderItemBuilder dispatchLatitudeSnapshot(Double value) {
            this.dispatchLatitudeSnapshot = value;
            return this;
        }

        public OrderItemBuilder dispatchLongitudeSnapshot(Double value) {
            this.dispatchLongitudeSnapshot = value;
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
            orderItem.dispatchLocationIdSnapshot = this.dispatchLocationIdSnapshot;
            orderItem.dispatchLocationTypeSnapshot = this.dispatchLocationTypeSnapshot;
            orderItem.dispatchLocationNameSnapshot = this.dispatchLocationNameSnapshot;
            orderItem.dispatchAddressSnapshot = this.dispatchAddressSnapshot;
            orderItem.dispatchCountySnapshot = this.dispatchCountySnapshot;
            orderItem.dispatchLatitudeSnapshot = this.dispatchLatitudeSnapshot;
            orderItem.dispatchLongitudeSnapshot = this.dispatchLongitudeSnapshot;
            orderItem.createdAt = this.createdAt;
            return orderItem;
        }
    }
}
