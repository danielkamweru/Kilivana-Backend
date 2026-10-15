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

    // Explicit getters and setters for Lombok compatibility
    public Long getLogisticsJobId() { return logisticsJobId; }
    public void setLogisticsJobId(Long logisticsJobId) { this.logisticsJobId = logisticsJobId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }
    public Double getBearing() { return bearing; }
    public void setBearing(Double bearing) { this.bearing = bearing; }
    public Double getAccuracyMetres() { return accuracyMetres; }
    public void setAccuracyMetres(Double accuracyMetres) { this.accuracyMetres = accuracyMetres; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public LocalDateTime getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(LocalDateTime clientTimestamp) { this.clientTimestamp = clientTimestamp; }

    public static DriverLatestLocationBuilder builder() {
        return new DriverLatestLocationBuilder();
    }

    public static class DriverLatestLocationBuilder {
        private Long logisticsJobId;
        private Long driverId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double bearing;
        private Double accuracyMetres;
        private Long locationId;
        private LocalDateTime recordedAt;
        private LocalDateTime clientTimestamp;

        public DriverLatestLocationBuilder logisticsJobId(Long logisticsJobId) {
            this.logisticsJobId = logisticsJobId;
            return this;
        }

        public DriverLatestLocationBuilder driverId(Long driverId) {
            this.driverId = driverId;
            return this;
        }

        public DriverLatestLocationBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public DriverLatestLocationBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public DriverLatestLocationBuilder speedKmh(Double speedKmh) {
            this.speedKmh = speedKmh;
            return this;
        }

        public DriverLatestLocationBuilder bearing(Double bearing) {
            this.bearing = bearing;
            return this;
        }

        public DriverLatestLocationBuilder accuracyMetres(Double accuracyMetres) {
            this.accuracyMetres = accuracyMetres;
            return this;
        }

        public DriverLatestLocationBuilder locationId(Long locationId) {
            this.locationId = locationId;
            return this;
        }

        public DriverLatestLocationBuilder recordedAt(LocalDateTime recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public DriverLatestLocationBuilder clientTimestamp(LocalDateTime clientTimestamp) {
            this.clientTimestamp = clientTimestamp;
            return this;
        }

        public DriverLatestLocation build() {
            DriverLatestLocation driverLatestLocation = new DriverLatestLocation();
            driverLatestLocation.logisticsJobId = this.logisticsJobId;
            driverLatestLocation.driverId = this.driverId;
            driverLatestLocation.latitude = this.latitude;
            driverLatestLocation.longitude = this.longitude;
            driverLatestLocation.speedKmh = this.speedKmh;
            driverLatestLocation.bearing = this.bearing;
            driverLatestLocation.accuracyMetres = this.accuracyMetres;
            driverLatestLocation.locationId = this.locationId;
            driverLatestLocation.recordedAt = this.recordedAt;
            driverLatestLocation.clientTimestamp = this.clientTimestamp;
            return driverLatestLocation;
        }
    }
}