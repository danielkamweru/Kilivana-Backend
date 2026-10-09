package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.ecommerce.dto.ProductResponse;
import com.kilivana.backend.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Product catalogue for sellers.
 * Provides endpoints for listing the products associated with a seller,
 * used to back the e-commerce product catalogue.
 */
@Tag(name = "E-Commerce · Products", description = "Product catalogue, pricing and product imagery")
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class CatalogCompatibilityController {

    private final ProductService productService;

    @Operation(summary = "List products for a seller",
            description = "Returns all products listed by the given seller.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{sellerId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSellerProducts(
            @Parameter(description = "ID of the seller") @PathVariable Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsBySeller(sellerId)));
    }
}