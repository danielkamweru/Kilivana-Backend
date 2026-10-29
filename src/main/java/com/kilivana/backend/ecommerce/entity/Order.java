package com.kilivana.backend.ecommerce.entity;

import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A purchase order placed by a buyer.
 *
 * <p>An order aggregates the basket (line items), computes the subtotal from
 * line-item prices, adds the caller-supplied delivery fee to form the total,
 * and tracks the payment lifecycle via {@code paymentStatus}. The reference
 * {@code code} is a panel-facing identifier (e.g. {@code ORD-2851}) assigned
 * on creation and never changed.
 *
 * <p>Line items are stored in {@link OrderItem} with the product name and unit
 * copied from the product at purchase time, so the order keeps reading correctly
 * even if the product is later renamed or deleted.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long buyerId;

    /** Panel-facing reference, e.g. {@code ORD-2851}. Assigned on creation. */
    @Column(nullable = false, updatable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal deliveryFee;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column
    private Long addressId;

    @Column(columnDefinition = "TEXT")
    private String pickupLocation;

    /** Delivery method: DOOR_DELIVERY or PICKUP_STATION */
    @Column(nullable = false)
    private String deliveryMethod;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private Long id;
        private Long buyerId;
        private String code;
        private OrderStatus status;
        private BigDecimal subtotal;
        private BigDecimal deliveryFee;
        private BigDecimal total;
        private PaymentStatus paymentStatus;
        private Long addressId;
        private String pickupLocation;
        private String deliveryMethod;
        private String cancellationReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<OrderItem> orderItems;

        public OrderBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderBuilder buyerId(Long buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public OrderBuilder code(String code) {
            this.code = code;
            return this;
        }

        public OrderBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public OrderBuilder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public OrderBuilder deliveryFee(BigDecimal deliveryFee) {
            this.deliveryFee = deliveryFee;
            return this;
        }

        public OrderBuilder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public OrderBuilder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public OrderBuilder addressId(Long addressId) {
            this.addressId = addressId;
            return this;
        }

        public OrderBuilder pickupLocation(String pickupLocation) {
            this.pickupLocation = pickupLocation;
            return this;
        }

        public OrderBuilder deliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
            return this;
        }

        public OrderBuilder cancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
            return this;
        }

        public OrderBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public OrderBuilder orderItems(List<OrderItem> orderItems) {
            this.orderItems = orderItems;
            return this;
        }

        public Order build() {
            Order order = new Order();
            order.id = this.id;
            order.buyerId = this.buyerId;
            order.code = this.code;
            order.status = this.status;
            order.subtotal = this.subtotal;
            order.deliveryFee = this.deliveryFee;
            order.total = this.total;
            order.paymentStatus = this.paymentStatus;
            order.addressId = this.addressId;
            order.pickupLocation = this.pickupLocation;
            order.deliveryMethod = this.deliveryMethod;
            order.cancellationReason = this.cancellationReason;
            order.createdAt = this.createdAt;
            order.updatedAt = this.updatedAt;
            order.orderItems = this.orderItems;
            return order;
        }
    }
}
