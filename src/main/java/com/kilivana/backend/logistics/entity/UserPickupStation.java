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
 * A pickup station saved by a buyer for quick access.
 * Similar to addresses, the owner is carried on the row ({@code userId})
 * rather than implied by a relationship.
 */
@Entity
@Table(name = "user_pickup_stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPickupStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owner; not a foreign key — ownership is enforced in PickupStationService. */
    @Column(nullable = false)
    private Long userId;

    /** Reference to the pickup station */
    @Column(nullable = false)
    private Long pickupStationId;

    /** Optional custom label, e.g. "My usual station" */
    private String label;

    /** Whether this is the user's default pickup station */
    @Column(nullable = false)
    private Boolean isDefault;

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
    public Long getPickupStationId() { return pickupStationId; }
    public void setPickupStationId(Long pickupStationId) { this.pickupStationId = pickupStationId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static UserPickupStationBuilder builder() {
        return new UserPickupStationBuilder();
    }

    public static class UserPickupStationBuilder {
        private Long id;
        private Long userId;
        private Long pickupStationId;
        private String label;
        private Boolean isDefault;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserPickupStationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserPickupStationBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserPickupStationBuilder pickupStationId(Long pickupStationId) {
            this.pickupStationId = pickupStationId;
            return this;
        }

        public UserPickupStationBuilder label(String label) {
            this.label = label;
            return this;
        }

        public UserPickupStationBuilder isDefault(Boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public UserPickupStationBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserPickupStationBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserPickupStation build() {
            UserPickupStation userStation = new UserPickupStation();
            userStation.id = this.id;
            userStation.userId = this.userId;
            userStation.pickupStationId = this.pickupStationId;
            userStation.label = this.label;
            userStation.isDefault = this.isDefault;
            userStation.createdAt = this.createdAt;
            userStation.updatedAt = this.updatedAt;
            return userStation;
        }
    }
}
