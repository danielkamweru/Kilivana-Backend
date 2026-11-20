package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to create a shipment from a paid order.
 */
@Schema(description = "Request to create a shipment from a paid order.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCreateRequest {

    @Schema(description = "Customer order ID", example = "100")
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Schema(description = "Seller fulfilment IDs to include in this shipment")
    @NotEmpty(message = "At least one seller fulfilment is required")
    private List<Long> sellerFulfilmentIds;

    @Schema(description = "Driver user ID to assign (optional)", example = "31")
    private Long driverId;

    @Schema(description = "Vehicle type", example = "Truck")
    private String vehicleType;

    @Schema(description = "Vehicle plate number", example = "KBA 123A")
    private String vehicleNumber;

    @Schema(description = "Driver instructions", example = "Handle with care - fresh produce")
    private String driverInstructions;
}
