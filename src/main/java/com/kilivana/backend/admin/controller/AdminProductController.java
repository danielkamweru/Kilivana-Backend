package com.kilivana.backend.admin.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.ecommerce.dto.ProductResponse;
import com.kilivana.backend.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for product management.
 * Handles product approval, rejection, suspension, and statistics.
 */
@Tag(name = "Admin · Products", description = "Product approval, status management and statistics")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "List all products with filters", description = "Returns paged list of all products with optional filters by status, category, seller, county, and search text.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @Parameter(description = "Filter by product status") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by seller ID") @RequestParam(required = false) Long sellerId,
            @Parameter(description = "Filter by seller county") @RequestParam(required = false) String county,
            @Parameter(description = "Search in product name and description") @RequestParam(required = false) String search,
            @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        Page<ProductResponse> products = productService.getFilteredProducts(status, categoryId, sellerId, county, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @Operation(summary = "Get product statistics", description = "Returns aggregate statistics: total, live, pending, needsRestock, liveStockValue")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductStats() {
        Map<String, Object> stats = productService.getProductStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Update product status with reason", description = "Updates product status (approve, reject, suspend) with a reason. Records who made the change and when.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @Parameter(description = "Authenticated admin user ID") @AuthenticationPrincipal Long adminId,
            @Parameter(description = "Product ID") @PathVariable Long id,
            @RequestBody Map<String, Object> statusUpdate) {
        ProductStatus status = ProductStatus.valueOf((String) statusUpdate.get("status"));
        String reason = (String) statusUpdate.get("reason");
        ProductResponse product = productService.updateProductStatusWithReason(id, status, reason, adminId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
