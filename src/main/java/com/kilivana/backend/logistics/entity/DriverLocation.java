package com.kilivana.backend.logistics.entity;

import com.kilivana.backend.common.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One GPS fix reported by the driver while on an active delivery. Append-only: a fix is
 * never edited or deleted by the business layer. The {@code tracking_events} table already
 * records status changes with optional coordinates; this table is the high-frequency
 * position stream, kept separate so recording a fix never touches the status history.
 *
 * <p>Retention is handled by {@code TrackingRetentionService}, which prunes rows older than
 * {@code TRACKING_HISTORY_RETENTION_DAYS}. The delivery record itself (the job, the proof,
 * the final fix) is never deleted.
 */
@Entity
@Table(name = "driver_locations",
        indexes = {
                @Index(name = "idx_driver_locations_job_recorded", columnList = "logisticsJobId,recordedAt"),
                @Index(name = "idx_driver_locations_driver_recorded", columnList = "driverId,recordedAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long logisticsJobId;

    @Column(nullable = false)
    private Long driverId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double speedKmh;
    private Double bearing;
    private Double accuracyMetres;

    /** The instant the fix was taken on the driver's device, when they supplied one. */
    private LocalDateTime clientTimestamp;

    /** The instant the server accepted the fix. Always populated. */
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime recordedAt;
}