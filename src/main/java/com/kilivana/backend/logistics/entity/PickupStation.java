package com.kilivana.backend.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A pickup station where buyers can collect their orders.
 * Stations are managed by the platform and can be saved by buyers for quick access.
 */
@Entity
@Table(name = "pickup_stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the pickup station, e.g. "Kilivana Hub - Westlands" */
    @Column(nullable = false)
    private String name;

    /** Full address of the station */
    @Column(nullable = false)
    private String address;

    /** Latitude coordinate */
    @Column(nullable = false)
    private Double latitude;

    /** Longitude coordinate */
    @Column(nullable = false)
    private Double longitude;

    /** Region/area for filtering, e.g. "Nairobi", "Mombasa" */
    @Column(nullable = false)
    private String region;

    /** Whether the station is currently accepting pickups */
    @Column(nullable = false)
    private Boolean isAvailable;

    /** Operating hours description, e.g. "8:00 AM - 8:00 PM, Mon-Sat" */
    private String openingHours;

    /** Contact phone number for the station */
    private String contactPhone;

    /** Additional instructions for pickup */
    @Column(columnDefinition = "TEXT")
    private String pickupInstructions;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Explicit getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getPickupInstructions() { return pickupInstructions; }
    public void setPickupInstructions(String pickupInstructions) { this.pickupInstructions = pickupInstructions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static PickupStationBuilder builder() {
        return new PickupStationBuilder();
    }

    public static class PickupStationBuilder {
        private Long id;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String region;
        private Boolean isAvailable;
        private String openingHours;
        private String contactPhone;
        private String pickupInstructions;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PickupStationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PickupStationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PickupStationBuilder address(String address) {
            this.address = address;
            return this;
        }

        public PickupStationBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public PickupStationBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public PickupStationBuilder region(String region) {
            this.region = region;
            return this;
        }

        public PickupStationBuilder isAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
            return this;
        }

        public PickupStationBuilder openingHours(String openingHours) {
            this.openingHours = openingHours;
            return this;
        }

        public PickupStationBuilder contactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
            return this;
        }

        public PickupStationBuilder pickupInstructions(String pickupInstructions) {
            this.pickupInstructions = pickupInstructions;
            return this;
        }

        public PickupStationBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PickupStationBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PickupStation build() {
            PickupStation station = new PickupStation();
            station.id = this.id;
            station.name = this.name;
            station.address = this.address;
            station.latitude = this.latitude;
            station.longitude = this.longitude;
            station.region = this.region;
            station.isAvailable = this.isAvailable;
            station.openingHours = this.openingHours;
            station.contactPhone = this.contactPhone;
            station.pickupInstructions = this.pickupInstructions;
            station.createdAt = this.createdAt;
            station.updatedAt = this.updatedAt;
            return station;
        }
    }
}
