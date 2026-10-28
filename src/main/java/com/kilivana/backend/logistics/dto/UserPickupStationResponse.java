package com.kilivana.backend.logistics.dto;

import com.kilivana.backend.logistics.entity.UserPickupStation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user's saved pickup station.
 */
@Schema(description = "User's saved pickup station response")
public class UserPickupStationResponse {

    @Schema(description = "Unique identifier of the saved station record", example = "1")
    private Long id;

    @Schema(description = "ID of the user who saved this station", example = "42")
    private Long userId;

    @Schema(description = "ID of the pickup station", example = "5")
    private Long pickupStationId;

    @Schema(description = "Name of the pickup station", example = "Kilivana Hub - Westlands")
    private String pickupStationName;

    @Schema(description = "Address of the pickup station", example = "123 Mombasa Road, Nairobi")
    private String pickupStationAddress;

    @Schema(description = "Region of the pickup station", example = "Nairobi")
    private String pickupStationRegion;

    @Schema(description = "Whether the station is currently available", example = "true")
    private Boolean pickupStationAvailable;

    @Schema(description = "Optional custom label", example = "My usual station")
    private String label;

    @Schema(description = "Whether this is the user's default pickup station", example = "true")
    private Boolean isDefault;

    @Schema(description = "Timestamp when the station was saved", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the saved station was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPickupStationId() {
        return pickupStationId;
    }

    public void setPickupStationId(Long pickupStationId) {
        this.pickupStationId = pickupStationId;
    }

    public String getPickupStationName() {
        return pickupStationName;
    }

    public void setPickupStationName(String pickupStationName) {
        this.pickupStationName = pickupStationName;
    }

    public String getPickupStationAddress() {
        return pickupStationAddress;
    }

    public void setPickupStationAddress(String pickupStationAddress) {
        this.pickupStationAddress = pickupStationAddress;
    }

    public String getPickupStationRegion() {
        return pickupStationRegion;
    }

    public void setPickupStationRegion(String pickupStationRegion) {
        this.pickupStationRegion = pickupStationRegion;
    }

    public Boolean getPickupStationAvailable() {
        return pickupStationAvailable;
    }

    public void setPickupStationAvailable(Boolean pickupStationAvailable) {
        this.pickupStationAvailable = pickupStationAvailable;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static UserPickupStationResponseBuilder builder() {
        return new UserPickupStationResponseBuilder();
    }

    public static class UserPickupStationResponseBuilder {
        private Long id;
        private Long userId;
        private Long pickupStationId;
        private String pickupStationName;
        private String pickupStationAddress;
        private String pickupStationRegion;
        private Boolean pickupStationAvailable;
        private String label;
        private Boolean isDefault;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserPickupStationResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserPickupStationResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserPickupStationResponseBuilder pickupStationId(Long pickupStationId) {
            this.pickupStationId = pickupStationId;
            return this;
        }

        public UserPickupStationResponseBuilder pickupStationName(String pickupStationName) {
            this.pickupStationName = pickupStationName;
            return this;
        }

        public UserPickupStationResponseBuilder pickupStationAddress(String pickupStationAddress) {
            this.pickupStationAddress = pickupStationAddress;
            return this;
        }

        public UserPickupStationResponseBuilder pickupStationRegion(String pickupStationRegion) {
            this.pickupStationRegion = pickupStationRegion;
            return this;
        }

        public UserPickupStationResponseBuilder pickupStationAvailable(Boolean pickupStationAvailable) {
            this.pickupStationAvailable = pickupStationAvailable;
            return this;
        }

        public UserPickupStationResponseBuilder label(String label) {
            this.label = label;
            return this;
        }

        public UserPickupStationResponseBuilder isDefault(Boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public UserPickupStationResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserPickupStationResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserPickupStationResponse build() {
            UserPickupStationResponse response = new UserPickupStationResponse();
            response.id = this.id;
            response.userId = this.userId;
            response.pickupStationId = this.pickupStationId;
            response.pickupStationName = this.pickupStationName;
            response.pickupStationAddress = this.pickupStationAddress;
            response.pickupStationRegion = this.pickupStationRegion;
            response.pickupStationAvailable = this.pickupStationAvailable;
            response.label = this.label;
            response.isDefault = this.isDefault;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
        }
    }

    public static UserPickupStationResponse fromEntity(UserPickupStation userStation, String stationName, String stationAddress, String stationRegion, Boolean stationAvailable) {
        return UserPickupStationResponse.builder()
                .id(userStation.getId())
                .userId(userStation.getUserId())
                .pickupStationId(userStation.getPickupStationId())
                .pickupStationName(stationName)
                .pickupStationAddress(stationAddress)
                .pickupStationRegion(stationRegion)
                .pickupStationAvailable(stationAvailable)
                .label(userStation.getLabel())
                .isDefault(userStation.getIsDefault())
                .createdAt(userStation.getCreatedAt())
                .updatedAt(userStation.getUpdatedAt())
                .build();
    }
}
