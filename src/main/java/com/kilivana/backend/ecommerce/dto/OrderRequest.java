package com.kilivana.backend.ecommerce.dto;

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
public class OrderRequest {

    @NotNull(message = "Buyer ID is required")
    private Long buyerId;

    @NotNull(message = "Subtotal is required")
    private BigDecimal subtotal;

    @NotNull(message = "Delivery fee is required")
    private BigDecimal deliveryFee;

    @NotNull(message = "Total is required")
    private BigDecimal total;

    @NotNull(message = "Address ID is required")
    private Long addressId;
}
