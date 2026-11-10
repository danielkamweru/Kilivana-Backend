package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Seller information associated with an order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {

    @Schema(description = "Seller user ID", example = "28")
    private Long sellerId;

    @Schema(description = "Seller display name", example = "John Kamau")
    private String sellerName;

    @Schema(description = "Seller phone number", example = "+254712345678")
    private String sellerPhone;

    @Schema(description = "Seller county or region", example = "Kiambu")
    private String sellerCounty;

    @Schema(description = "Seller profile location", example = "Thika")
    private String sellerLocation;
}
