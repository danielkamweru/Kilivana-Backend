package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A shipment as returned to admins, drivers, and buyers.
 */
@Schema(description = "A shipment grouping one or more farmer pickups and one buyer delivery.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {

    @Schema(description = "Shipment ID", example = "7")
    private Long id;

    @Schema(description = "Shipment reference code", example = "SHIP-00042")
    private String code;

    @Schema(description = "Customer order ID", example = "100")
    private Long orderId;

    @Schema(description = "Shipment status", example = "IN_TRANSIT")
    private ShipmentStatus status;

    @Schema(description = "Assigned driver user ID", example = "31")
    private Long driverId;

    @Schema(description = "Driver display name", example = "John Mwangi")
    private String driverName;

    @Schema(description = "Driver phone number", example = "+254712345678")
    private String driverPhone;

    @Schema(description = "Vehicle type", example = "Truck")
    private String vehicleType;

    @Schema(description = "Vehicle plate", example = "KBA 123A")
    private String vehicleNumber;

    @Schema(description = "Estimated cargo weight in kg", example = "50.0")
    private Double estimatedWeightKg;

    @Schema(description = "Estimated cargo volume in m3", example = "2.5")
    private Double estimatedVolumeM3;

    @Schema(description = "Driver instructions", example = "Handle with care - fresh produce")
    private String driverInstructions;

    @Schema(description = "Route distance in km", example = "45.5")
    private Double distanceKm;

    @Schema(description = "Estimated route duration in minutes", example = "90")
    private Integer estimatedMinutes;

    @Schema(description = "When driver was assigned", example = "2024-01-15T09:00:00")
    private LocalDateTime assignedAt;

    @Schema(description = "When driver accepted", example = "2024-01-15T09:15:00")
    private LocalDateTime acceptedAt;

    @Schema(description = "When delivery started", example = "2024-01-15T10:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "When delivery completed", example = "2024-01-15T14:30:00")
    private LocalDateTime completedAt;

    @Schema(description = "Cancellation reason, if cancelled")
    private String cancellationReason;

    @Schema(description = "Creation timestamp", example = "2024-01-15T08:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Route stops in sequence order")
    private List<ShipmentStopResponse> stops;

    @Schema(description = "Items allocated to this shipment")
    private List<ShipmentItemResponse> items;
}
