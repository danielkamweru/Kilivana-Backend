package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.QualityGrade;
import com.kilivana.backend.common.enums.SellerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A product listing as the panel reads it: who sells it, what it is, the
 * price and stock, and the images attached to it.
 */
@Schema(description = "Product response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    @Schema(description = "Product ID", example = "25")
    private Long id;
    @Schema(description = "Seller user ID who owns the listing", example = "12")
    private Long sellerId;
    @Schema(description = "Type of seller the listing belongs to", example = "FARMER")
    private SellerType sellerType;
    @Schema(description = "Category ID the listing belongs to", example = "5")
    private Long categoryId;
    @Schema(description = "Product name", example = "Organic Tomatoes")
    private String name;
    @Schema(description = "Product description", example = "Fresh organic tomatoes grown on our farm")
    private String description;
    @Schema(description = "Unit of measurement", example = "kg")
    private String unit;
    @Schema(description = "Price per unit", example = "1200.50")
    private BigDecimal price;
    @Schema(description = "Stock quantity available", example = "50")
    private Integer stockQty;
    @Schema(description = "Quantity reserved for open orders", example = "5")
    private Integer reservedQty;
    @Schema(description = "Quantity already sold", example = "120")
    private Integer soldQty;
    @Schema(description = "Minimum quantity a buyer can order", example = "1")
    private Integer minimumOrderQty;
    @Schema(description = "Current product status", example = "ACTIVE")
    private ProductStatus status;
    @Schema(description = "Quality grade of the product", example = "GRADE_A")
    private QualityGrade qualityGrade;
    @Schema(description = "Harvest date for fresh products", example = "2024-01-10")
    private LocalDate harvestDate;
    @Schema(description = "Threshold for low stock warning", example = "10")
    private Integer lowStockThreshold;
    @Schema(description = "Farm ID if product is linked to a certified farm", example = "3")
    private Long farmId;
    @Schema(description = "Reason for status change", example = "Failed quality inspection")
    private String statusReason;
    @Schema(description = "Timestamp when status was last changed", example = "2024-01-15T12:00:00")
    private LocalDateTime statusChangedAt;
    @Schema(description = "User ID who last changed the status", example = "5")
    private Long statusChangedBy;
    @Schema(description = "Timestamp when product was verified", example = "2024-01-15T11:00:00")
    private LocalDateTime verifiedAt;
    @Schema(description = "User ID who verified the product", example = "5")
    private Long verifiedBy;
    @Schema(description = "Quantity verified during inspection", example = "50")
    private Integer verifiedQty;
    @Schema(description = "Last inspection ID", example = "12")
    private Long lastInspectionId;
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;
    @Schema(description = "Images attached to the product", example = "[{\"id\":9,\"isPrimary\":true}]")
    private List<ProductImageResponse> images;

    // Read-only fields for admin/customer views
    @Schema(description = "Seller name", example = "John Farmer")
    private String sellerName;
    @Schema(description = "Seller phone", example = "254712345678")
    private String sellerPhone;
    @Schema(description = "Seller county", example = "Kiambu")
    private String sellerCounty;
    @Schema(description = "Category name", example = "Fresh Vegetables")
    private String categoryName;
    @Schema(description = "Farm name if linked", example = "Green Valley Farm")
    private String farmName;
    @Schema(description = "Whether farm is certified", example = "true")
    private Boolean farmCertified;
    @Schema(description = "Whether product is live (active and has stock)", example = "true")
    private Boolean isLive;
}
