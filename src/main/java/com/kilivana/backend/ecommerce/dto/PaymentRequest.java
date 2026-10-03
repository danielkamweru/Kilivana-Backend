package com.kilivana.backend.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    /** The admin panel sends the wallet or rail here, as MPESA, BANK or CARD. */
    private String method;

    /** What this field was called before the panel named it. Kept so older callers work. */
    private String provider;

    /**
     * @return whichever of the two names the caller used, so a request written against
     *     either version of the contract is accepted
     */
    @JsonIgnore
    public String resolveMethod() {
        return method != null && !method.isBlank() ? method : provider;
    }

    @NotBlank(message = "Reference is required")
    private String reference;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
