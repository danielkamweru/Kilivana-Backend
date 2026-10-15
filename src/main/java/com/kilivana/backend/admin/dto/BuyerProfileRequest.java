package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a buyer profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a buyer profile")
public class BuyerProfileRequest {

    @NotBlank
    @Schema(description = "Contact details for the buyer", example = "Preferred contact via WhatsApp: +254700000001")
    private String contactDetails;

    @Schema(description = "Saved addresses as JSON or comma-separated string", example = "[{\"label\":\"Home\",\"address\":\"123 Mombasa Road, Nairobi\"}]")
    private String savedAddresses;
}