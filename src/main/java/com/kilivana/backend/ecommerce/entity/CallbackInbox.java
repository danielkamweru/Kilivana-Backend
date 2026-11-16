package com.kilivana.backend.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Durable inbox for Daraja callbacks that arrive before their payment record
 * exists (e.g., callback races ahead of payment persistence).
 *
 * <p>Entries are matched against payments by {@code checkoutRequestId} once the
 * payment is persisted. A unique constraint on {@code checkoutRequestId} plus
 * {@code merchantRequestId} prevents duplicate inbox entries for the same
 * callback.
 */
@Entity
@Table(name = "callback_inbox",
        uniqueConstraints = @UniqueConstraint(columnNames = {"checkout_request_id", "merchant_request_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String checkoutRequestId;

    @Column(nullable = false)
    private String merchantRequestId;

    @Column(nullable = false)
    private Integer resultCode;

    @Column(columnDefinition = "TEXT")
    private String resultDesc;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime receivedAt;
}
