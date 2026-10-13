package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user address with audit timestamps.
 */
@Schema(description = "User address response with metadata")
public class AddressResponse {

    @Schema(description = "Unique identifier of the address", example = "1")
    private Long id;

    @Schema(description = "ID of the user this address belongs to", example = "42")
    private Long userId;

    @Schema(description = "Full textual address", example = "123 Mombasa Road, Nairobi")
    private String addressText;

    @Schema(description = "Latitude coordinate", example = "-1.2921")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "36.8219")
    private Double longitude;

    @Schema(description = "Optional label for the address", example = "Home")
    private String label;

    @Schema(description = "Timestamp when the address was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the address was last updated", example = "2026-01-15T10:30:00")
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

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public static AddressResponseBuilder builder() {
        return new AddressResponseBuilder();
    }

    public static class AddressResponseBuilder {
        private Long id;
        private Long userId;
        private String addressText;
        private Double latitude;
        private Double longitude;
        private String label;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AddressResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AddressResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AddressResponseBuilder addressText(String addressText) {
            this.addressText = addressText;
            return this;
        }

        public AddressResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public AddressResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public AddressResponseBuilder label(String label) {
            this.label = label;
            return this;
        }

        public AddressResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AddressResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AddressResponse build() {
            AddressResponse addressResponse = new AddressResponse();
            addressResponse.id = this.id;
            addressResponse.userId = this.userId;
            addressResponse.addressText = this.addressText;
            addressResponse.latitude = this.latitude;
            addressResponse.longitude = this.longitude;
            addressResponse.label = this.label;
            addressResponse.createdAt = this.createdAt;
            addressResponse.updatedAt = this.updatedAt;
            return addressResponse;
        }
    }

    public static AddressResponse fromEntity(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUserId())
                .addressText(address.getAddressText())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .label(address.getLabel())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
