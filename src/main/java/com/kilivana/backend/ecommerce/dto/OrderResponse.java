package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An order as the panel reads it: who bought, what was bought, and where
 * the money sits. The line items are the basket the buyer placed, with the
 * name and unit each product had at purchase time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    /** Panel-facing reference, e.g. {@code ORD-2851}. */
    private String code;
    private Long buyerId;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private PaymentStatus paymentStatus;
    private Long addressId;
    private String cancellationReason;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
