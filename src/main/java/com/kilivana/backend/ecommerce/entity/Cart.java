package com.kilivana.backend.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A buyer's shopping cart.
 *
 * <p>There is at most one {@code ACTIVE} cart per buyer. Adding the first item
 * creates the cart. Items store a price snapshot so the buyer sees the price
 * they agreed to at checkout even if the product price changes later. Checkout
 * flips the status to {@code CHECKOUT_PROCESSING} while the order service
 * reads the lines and creates the order.
 */
@Entity
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Lifecycle states for a shopping cart.
     */
    public enum CartStatus {
        /** Cart is accepting new items and modifications. */
        ACTIVE,
        /** Checkout has started; no further mutations allowed. */
        CHECKOUT_PROCESSING,
        /** Cart was abandoned and kept for analytics only. */
        ABANDONED
    }
}
