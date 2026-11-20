package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update a seller fulfilment's status (farmer action).
 */
@Schema(description = "Request to update a seller fulfilment status.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentStatusUpdateRequest {

    @Schema(description = "New fulfilment status", example = "READY_FOR_PICKUP")
    @NotNull(message = "Status is required")
    private FulfilmentStatus status;

    @Schema(description = "Farmer's notes", example = "Produce packed and ready")
    private String notes;
}
