package com.kilivana.backend.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "checkout_quotes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutQuote {

    @Id
    @Column(name = "quote_id", nullable = false, length = 36)
    private String quoteId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "delivery_method", nullable = false, length = 32)
    private String deliveryMethod;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "pickup_station_id")
    private Long pickupStationId;

    @Column(name = "pickup_location", length = 500)
    private String pickupLocation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "order_id")
    private Long orderId;
}
