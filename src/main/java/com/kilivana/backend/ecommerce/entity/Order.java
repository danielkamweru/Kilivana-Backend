package com.kilivana.backend.ecommerce.entity;

import com.kilivana.backend.common.enums.OrderStatus;
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(nullable = false)
    private Long addressId;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
