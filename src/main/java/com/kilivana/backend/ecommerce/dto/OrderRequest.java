package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything needed to place an order.
 *
 * <p>The line items are the order: the subtotal and the total are computed
 * here rather than trusted from the caller, so a client cannot ask for a
 * basket worth less than it pays for. {@code subtotal} and {@code total} may
 * be sent for the caller's own reconciliation and are checked against the
 * computed figures, but they are never the source of truth.
 *
 * <p>The delivery fee is the one figure the caller supplies, because only the
 * caller knows what the delivery costs; it defaults to zero when omitted.
 */
@Schema(description = "Request to place an order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @Schema(description = "Buyer user ID", example = "42")
    @NotNull(message = "Buyer ID is required")
    private Long buyerId;

    @Schema(description = "Delivery address ID", example = "8")
    @NotNull(message = "Address ID is required")
    private Long addressId;

    @Schema(description = "Order line items")
    @NotEmpty(message = "Order must have at least one item")
    private List<@Valid OrderItemRequest> items;

    @Schema(description = "Delivery fee", example = "150.00")
    private BigDecimal deliveryFee;

    /** Reconciliation figure only; the computed subtotal is what is stored. */
    @Schema(description = "Reconciliation subtotal (computed server-side)", example = "3601.50")
    private BigDecimal subtotal;

    /** Reconciliation figure only; the computed total is what is stored. */
    @Schema(description = "Reconciliation total (computed server-side)", example = "3751.50")
    private BigDecimal total;
}
