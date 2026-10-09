package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user address with audit timestamps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
