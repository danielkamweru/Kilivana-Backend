package com.kilivana.backend.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** A line of an order as it is read back, with the display fields the panel shows. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private Long sellerId;
    /** Name and unit are copied from the product at purchase, so the order keeps
     *  reading correctly if the product is renamed or removed afterwards. */
    private String productName;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
