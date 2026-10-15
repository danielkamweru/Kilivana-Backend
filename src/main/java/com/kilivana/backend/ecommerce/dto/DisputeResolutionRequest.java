package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How a dispute was settled, and why.
 *
 * <p>The outcome decides what happens to the order and its payment: the
 * buyer's favour cancels the order and refunds the money, the farmer's
 * favour completes the order and releases the money to the seller.
 */
@Schema(description = "Request to resolve a dispute")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResolutionRequest {

    @Schema(description = "Outcome of the dispute resolution", example = "BUYER")
    @NotNull(message = "Outcome is required")
    private DisputeOutcome outcome;

    @Schema(description = "Explanation of the resolution decision", example = "Refunded buyer as item was never delivered")
    @NotBlank(message = "Resolution is required")
    private String resolution;

    /** Whose favour the dispute was resolved in. */
    @Schema(description = "Possible dispute outcomes")
    public enum DisputeOutcome {
        BUYER,
        FARMER
    }
}
