package com.kilivana.backend.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private String url;
    private String publicId;
    private String assetId;
    private Integer sortOrder;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}