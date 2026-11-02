package com.kilivana.backend.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_events", indexes = {
        @Index(name = "delivery_events_job_created_idx", columnList = "delivery_job_id,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_job_id", nullable = false)
    private Long deliveryJobId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
