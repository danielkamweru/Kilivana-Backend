package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.ecommerce.dto.ProductResponse;
import com.kilivana.backend.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class CatalogCompatibilityController {

    private final ProductService productService;

    @GetMapping("/{sellerId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSellerProducts(@PathVariable Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsBySeller(sellerId)));
    }
}