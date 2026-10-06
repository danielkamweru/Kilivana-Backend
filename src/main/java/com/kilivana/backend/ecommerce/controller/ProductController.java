package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.ProductImageResponse;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.dto.ProductResponse;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.ecommerce.service.CategoryService;
import com.kilivana.backend.ecommerce.service.ProductService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "E-Commerce · Products", description = "Product catalogue, pricing and product imagery")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("product") ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        ProductResponse product;
        if (images != null && !images.isEmpty()) {
            product = productService.createProductWithImages(request, images, userId);
        } else {
            product = productService.createProduct(request, userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProductJson(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<com.kilivana.backend.ecommerce.dto.CategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsBySeller(@PathVariable Long sellerId) {
        List<ProductResponse> products = productService.getProductsBySeller(sellerId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(@PathVariable Long categoryId) {
        List<ProductResponse> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sellerType,
            Pageable pageable) {
        Page<ProductResponse> products = productService.searchProducts(name, categoryId, sellerType, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestPart("product") ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        productService.ensureProductOwnership(id, userId);
        ProductResponse product;
        if (images != null && !images.isEmpty()) {
            product = productService.updateProductWithImages(id, request, images);
        } else {
            product = productService.updateProduct(id, request);
        }
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductJson(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        productService.ensureProductOwnership(id, userId);
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestParam ProductStatus status) {
        productService.ensureProductOwnership(id, userId);
        ProductResponse product = productService.updateProductStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) throws IOException {
        productService.ensureProductOwnership(id, userId);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Product deleted successfully"));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadProductImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        productService.ensureProductOwnership(id, userId);
        ProductImageResponse imageResponse = productService.uploadProductImage(id, image, sortOrder, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(imageResponse));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getProductImages(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product.getImages()));
    }

    @PutMapping(value = "/{productId}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateProductImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        productService.ensureProductOwnership(productId, userId);
        ProductImageResponse imageResponse = productService.updateProductImage(productId, imageId, image, sortOrder, isPrimary);
        return ResponseEntity.ok(ApiResponse.success(imageResponse));
    }

    @PutMapping("/{productId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.ensureProductOwnership(productId, userId);
        productService.setPrimaryImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @PutMapping("/{productId}/images/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderImages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @RequestBody List<Long> imageIdsInOrder) {
        productService.ensureProductOwnership(productId, userId);
        productService.reorderImages(productId, imageIdsInOrder);
        return ResponseEntity.ok(ApiResponse.successMessage("Images reordered successfully"));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @PathVariable Long imageId) throws IOException {
        productService.ensureProductOwnership(productId, userId);
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }
}
