package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.DisputeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response representing a dispute.
 */
@Schema(description = "Dispute response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {

    @Schema(description = "Dispute ID", example = "15")
    private Long id;

    @Schema(description = "Order ID the dispute relates to", example = "100")
    private Long orderId;

    @Schema(description = "User ID who raised the dispute", example = "42")
    private Long raisedBy;

    @Schema(description = "Short reason for the dispute", example = "Item not delivered")
    private String reason;

    @Schema(description = "Detailed description of the dispute", example = "Order was marked delivered but never arrived")
    private String description;

    @Schema(description = "Current dispute status", example = "OPEN")
    private DisputeStatus status;

    @Schema(description = "Resolution explanation when resolved", example = "Refunded buyer as item was never delivered")
    private String resolution;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;
}
