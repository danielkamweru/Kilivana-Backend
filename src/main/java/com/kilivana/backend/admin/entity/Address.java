package com.kilivana.backend.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A saved delivery address belonging to one buyer. The owner is carried on the row
 * ({@code userId}) rather than implied by a relationship, because the profile tables have
 * no foreign key to users — the ownership check has to be done in code.
 */
@Entity
@Table(name = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owner; not a foreign key — ownership is enforced in AddressService. */
    @Column(nullable = false)
    private Long userId;

    /** Free-text address as typed or geocoded, e.g. "123 Mombasa Road, Nairobi". */
    @Column(nullable = false)
    private String addressText;

    /** Latitude, set when the address is geocoded; null otherwise. */
    private Double latitude;

    /** Longitude, set when the address is geocoded; null otherwise. */
    private Double longitude;

    /** Optional short label, e.g. "Home", "Warehouse". */
    private String label;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Explicit getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static AddressBuilder builder() {
        return new AddressBuilder();
    }

    public static class AddressBuilder {
        private Long id;
        private Long userId;
        private String addressText;
        private Double latitude;
        private Double longitude;
        private String label;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AddressBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AddressBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AddressBuilder addressText(String addressText) {
            this.addressText = addressText;
            return this;
        }

        public AddressBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public AddressBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public AddressBuilder label(String label) {
            this.label = label;
            return this;
        }

        public AddressBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AddressBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Address build() {
            Address address = new Address();
            address.id = this.id;
            address.userId = this.userId;
            address.addressText = this.addressText;
            address.latitude = this.latitude;
            address.longitude = this.longitude;
            address.label = this.label;
            address.createdAt = this.createdAt;
            address.updatedAt = this.updatedAt;
            return address;
        }
    }
}
