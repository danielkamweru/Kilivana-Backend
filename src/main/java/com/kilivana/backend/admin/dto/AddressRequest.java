package com.kilivana.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a user address. The {@code userId} is supplied by
 * the caller rather than the path, so the same body works for both the scoped and the
 * "my address" endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating a user address")
public class AddressRequest {

    @NotBlank(message = "Address text is required")
    @Schema(description = "Full textual address", example = "123 Mombasa Road, Nairobi")
    private String addressText;

    @Schema(description = "Latitude coordinate", example = "-1.2921")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "36.8219")
    private Double longitude;

    @Schema(description = "Optional label for the address", example = "Home")
    private String label;

    @Schema(description = "ID of the user this address belongs to", example = "42")
    private Long userId;
}
