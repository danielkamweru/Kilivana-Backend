package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment as the panel reads it: the order it settles, how it was made,
 * the provider's reference, and where the money sits.
 */
@Schema(description = "Payment response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "33")
    private Long id;
    @Schema(description = "Order ID this payment settles", example = "100")
    private Long orderId;
    @Schema(description = "Payment method used", example = "MPESA")
    private PaymentMethod method;
    @Schema(description = "Payment transaction reference from the provider", example = "QF8H2K1A")
    private String reference;
    @Schema(description = "Amount paid, in the order currency", example = "3751.50")
    private BigDecimal amount;
    @Schema(description = "Current payment status", example = "HELD")
    private PaymentStatus status;
    @Schema(description = "Timestamp when the payment was made", example = "2024-01-15T11:00:00")
    private LocalDateTime paidAt;
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;
}
