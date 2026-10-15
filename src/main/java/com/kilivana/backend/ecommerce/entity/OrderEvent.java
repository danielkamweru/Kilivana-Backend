package com.kilivana.backend.ecommerce.entity;

import com.kilivana.backend.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single state change in an order's lifecycle.
 *
 * <p>Each transition (e.g. {@code PLACED} -> {@code CONFIRMED} -> {@code DELIVERED})
 * is recorded here so the panel can render a timeline. The optional {@code note}
 * captures human-readable context for the transition (e.g. cancellation reason).
 */
@Entity
@Table(name = "order_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String note;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}