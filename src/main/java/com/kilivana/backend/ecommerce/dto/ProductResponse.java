package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.SellerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Long sellerId;
    private SellerType sellerType;
    private Long categoryId;
    private String name;
    private String description;
    private String unit;
    private BigDecimal price;
    private Integer stockQty;
    private Integer reservedQty;
    private Integer soldQty;
    private Integer minimumOrderQty;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
