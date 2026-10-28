package com.kilivana.backend.logistics.dto;

import com.kilivana.backend.logistics.entity.PickupStation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO representing a pickup station.
 */
@Schema(description = "Pickup station response")
public class PickupStationResponse {

    @Schema(description = "Unique identifier of the pickup station", example = "1")
    private Long id;

    @Schema(description = "Name of the pickup station", example = "Kilivana Hub - Westlands")
    private String name;

    @Schema(description = "Full address of the station", example = "123 Mombasa Road, Nairobi")
    private String address;

    @Schema(description = "Latitude coordinate", example = "-1.2921")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "36.8219")
    private Double longitude;

    @Schema(description = "Region/area for filtering", example = "Nairobi")
    private String region;

    @Schema(description = "Whether the station is currently accepting pickups", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Operating hours description", example = "8:00 AM - 8:00 PM, Mon-Sat")
    private String openingHours;

    @Schema(description = "Contact phone number for the station", example = "+254712345678")
    private String contactPhone;

    @Schema(description = "Additional instructions for pickup", example = "Bring your order confirmation")
    private String pickupInstructions;

    @Schema(description = "Timestamp when the station was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the station was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getPickupInstructions() {
        return pickupInstructions;
    }

    public void setPickupInstructions(String pickupInstructions) {
        this.pickupInstructions = pickupInstructions;
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

    public static PickupStationResponseBuilder builder() {
        return new PickupStationResponseBuilder();
    }

    public static class PickupStationResponseBuilder {
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

        public PickupStationResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PickupStationResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PickupStationResponseBuilder address(String address) {
            this.address = address;
            return this;
        }

        public PickupStationResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public PickupStationResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public PickupStationResponseBuilder region(String region) {
            this.region = region;
            return this;
        }

        public PickupStationResponseBuilder isAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
            return this;
        }

        public PickupStationResponseBuilder openingHours(String openingHours) {
            this.openingHours = openingHours;
            return this;
        }

        public PickupStationResponseBuilder contactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
            return this;
        }

        public PickupStationResponseBuilder pickupInstructions(String pickupInstructions) {
            this.pickupInstructions = pickupInstructions;
            return this;
        }

        public PickupStationResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PickupStationResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PickupStationResponse build() {
            PickupStationResponse response = new PickupStationResponse();
            response.id = this.id;
            response.name = this.name;
            response.address = this.address;
            response.latitude = this.latitude;
            response.longitude = this.longitude;
            response.region = this.region;
            response.isAvailable = this.isAvailable;
            response.openingHours = this.openingHours;
            response.contactPhone = this.contactPhone;
            response.pickupInstructions = this.pickupInstructions;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
        }
    }

    public static PickupStationResponse fromEntity(PickupStation station) {
        return PickupStationResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .address(station.getAddress())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .region(station.getRegion())
                .isAvailable(station.getIsAvailable())
                .openingHours(station.getOpeningHours())
                .contactPhone(station.getContactPhone())
                .pickupInstructions(station.getPickupInstructions())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }
}
