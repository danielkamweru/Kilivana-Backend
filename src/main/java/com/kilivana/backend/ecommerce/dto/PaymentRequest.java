package com.kilivana.backend.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A payment the admin panel records against an order: the order it settles,
 * how the money moved, the provider's own reference, and the amount paid.
 */
@Schema(description = "Payment request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @Schema(description = "Order ID this payment settles", example = "100")
    @NotNull(message = "Order ID is required")
    private Long orderId;

    /** The admin panel sends the wallet or rail here, as MPESA, BANK or CARD. */
    @Schema(description = "Payment method, e.g. MPESA, BANK or CARD", example = "MPESA")
    private String method;

    /** What this field was called before the panel named it. Kept so older callers work. */
    @Schema(description = "Legacy payment provider field, still accepted for backward compatibility", example = "MPESA")
    private String provider;

    /**
     * @return whichever of the two names the caller used, so a request written against
     *     either version of the contract is accepted
     */
    @JsonIgnore
    public String resolveMethod() {
        return method != null && !method.isBlank() ? method : provider;
    }

    @Schema(description = "Payment transaction reference from the provider", example = "QF8H2K1A")
    @NotBlank(message = "Reference is required")
    private String reference;

    @Schema(description = "Amount paid, in the order currency", example = "3751.50")
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
