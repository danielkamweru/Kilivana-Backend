package com.kilivana.backend.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One row per delivery job: the driver's most recent fix. Written on every accepted update
 * so a "where is the driver now" read is a single row lookup instead of a history scan.
 *
 * <p>This is a denormalised cache of the latest {@link DriverLocation}; it is not itself a
 * source of truth. The history table remains the audit trail, and the two are kept in sync
 * inside the same transaction.
 */
@Entity
@Table(name = "driver_latest_location")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLatestLocation {

    /** Primary key: the delivery job this row is the cache for. */
    @Id
    @Column(nullable = false)
    private Long logisticsJobId;

    /** ID of the driver who reported the cached fix. */
    @Column(nullable = false)
    private Long driverId;

    /** Latitude of the cached fix, in decimal degrees, WGS84. */
    @Column(nullable = false)
    private Double latitude;

    /** Longitude of the cached fix, in decimal degrees, WGS84. */
    @Column(nullable = false)
    private Double longitude;

    /** Speed over ground in km/h, when the device reported it. */
    private Double speedKmh;
    /** Direction of travel in degrees, when the device reported it. */
    private Double bearing;
    /** Accuracy radius in metres, when the device reported it. */
    private Double accuracyMetres;

    /** ID of the DriverLocation row this cache entry mirrors. */
    private Long locationId;

    /** The instant the server accepted the fix that is cached here. */
    private LocalDateTime recordedAt;

    /** The instant the fix was taken on the driver's device, when supplied. */
    private LocalDateTime clientTimestamp;
}