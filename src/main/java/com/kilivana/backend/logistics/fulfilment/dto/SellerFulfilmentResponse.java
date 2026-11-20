package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A seller fulfilment record as returned to farmers and admins.
 */
@Schema(description = "A seller fulfilment record: one farmer's portion of an order.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerFulfilmentResponse {

    @Schema(description = "Fulfilment ID", example = "12")
    private Long id;

    @Schema(description = "Customer order ID", example = "100")
    private Long orderId;

    @Schema(description = "Farmer/seller user ID", example = "28")
    private Long sellerId;

    @Schema(description = "Seller display name", example = "Green Valley Farm")
    private String sellerName;

    @Schema(description = "Seller's portion of the order total", example = "3601.50")
    private BigDecimal sellerSubtotal;

    @Schema(description = "Fulfilment status", example = "READY_FOR_PICKUP")
    private FulfilmentStatus status;

    @Schema(description = "Farmer's notes", example = "Produce packed in crates")
    private String notes;

    @Schema(description = "When the farmer marked products ready", example = "2024-01-15T11:00:00")
    private LocalDateTime readyAt;

    @Schema(description = "When the driver collected products", example = "2024-01-15T14:30:00")
    private LocalDateTime collectedAt;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Line items belonging to this seller")
    private List<FulfilmentItemResponse> items;
}
