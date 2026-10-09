package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.DisputeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to raise a dispute on an order.
 */
@Schema(description = "Request to raise a dispute on an order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRequest {

    @Schema(description = "Order ID the dispute relates to", example = "100")
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Schema(description = "User ID raising the dispute", example = "42")
    @NotNull(message = "Raised by is required")
    private Long raisedBy;

    @Schema(description = "Short reason for the dispute", example = "Item not delivered")
    @NotBlank(message = "Reason is required")
    private String reason;

    @Schema(description = "Detailed description of the dispute", example = "Order was marked delivered but never arrived")
    private String description;

    @Schema(description = "Initial dispute status", example = "OPEN")
    private DisputeStatus status;
}
