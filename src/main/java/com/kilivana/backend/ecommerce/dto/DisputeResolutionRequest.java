package com.kilivana.backend.ecommerce.dto;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResolutionRequest {

    @NotNull(message = "Outcome is required")
    private DisputeOutcome outcome;

    @NotBlank(message = "Resolution is required")
    private String resolution;

    /** Whose favour the dispute was resolved in. */
    public enum DisputeOutcome {
        BUYER,
        FARMER
    }
}
