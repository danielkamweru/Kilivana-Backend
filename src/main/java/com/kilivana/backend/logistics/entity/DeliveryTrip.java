package com.kilivana.backend.logistics.entity;

import com.kilivana.backend.logistics.enums.DeliveryTripStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_trips", indexes = {
        @Index(name = "delivery_trips_job_created_idx", columnList = "delivery_job_id,created_at"),
        @Index(name = "delivery_trips_driver_status_idx", columnList = "driver_id,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_job_id", nullable = false)
    private Long deliveryJobId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryTripStatus status = DeliveryTripStatus.ACTIVE;

    private LocalDateTime startedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime completedAt;
    private Double lastLatitude;
    private Double lastLongitude;
    private LocalDateTime lastLocationUpdatedAt;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
