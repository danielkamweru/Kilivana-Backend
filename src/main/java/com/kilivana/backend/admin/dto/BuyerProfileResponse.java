package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.BuyerProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO representing a buyer profile with audit timestamps.
 */
@Data
@Builder
@Schema(description = "Buyer profile response with metadata")
public class BuyerProfileResponse {

    @Schema(description = "Unique identifier of the buyer profile", example = "5")
    private Long id;

    @Schema(description = "ID of the user this profile belongs to", example = "10")
    private Long userId;

    @Schema(description = "Contact details for the buyer", example = "Preferred contact via WhatsApp: +254700000001")
    private String contactDetails;

    @Schema(description = "Saved addresses as JSON or comma-separated string", example = "[{\"label\":\"Home\",\"address\":\"123 Mombasa Road, Nairobi\"}]")
    private String savedAddresses;

    @Schema(description = "Timestamp when the profile was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the profile was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    public static BuyerProfileResponse fromEntity(BuyerProfile profile) {
        return BuyerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .contactDetails(profile.getContactDetails())
                .savedAddresses(profile.getSavedAddresses())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}