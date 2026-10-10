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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing the product catalogue: creation, retrieval,
 * updates, status, category association and product imagery.
 */
@Tag(name = "E-Commerce · Products", description = "Product catalogue, pricing and product imagery")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @Operation(summary = "Create product with images", description = "Creates a new product with one or more images using multipart form data and returns the created product.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product details (multipart part named \"product\")") @Valid @RequestPart("product") ProductRequest request,
            @Parameter(description = "Product images (multipart part named \"images\")") @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        ProductResponse product;
        // Use image-aware creation when files are provided; otherwise fall back to JSON-only path
        if (images != null && !images.isEmpty()) {
            product = productService.createProductWithImages(request, images, userId);
        } else {
            product = productService.createProduct(request, userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product));
    }

    @Operation(summary = "Create product (JSON)", description = "Creates a new product using a JSON request body and returns the created product.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProductJson(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product details") @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product));
    }

    @Operation(summary = "List categories", description = "Returns all product categories.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<com.kilivana.backend.ecommerce.dto.CategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @Operation(summary = "List all products", description = "Returns all products in the catalogue.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @Operation(summary = "Get product by id", description = "Retrieves a single product by its identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@Parameter(description = "Product identifier") @PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @Operation(summary = "Get products by seller", description = "Retrieves all products belonging to a specific seller.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsBySeller(@Parameter(description = "Seller identifier") @PathVariable Long sellerId) {
        List<ProductResponse> products = productService.getProductsBySeller(sellerId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @Operation(summary = "Get products by category", description = "Retrieves all products within a specific category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(@Parameter(description = "Category identifier") @PathVariable Long categoryId) {
        List<ProductResponse> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @Operation(summary = "Search products", description = "Searches products with optional name, category and seller type filters, paginated.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @Parameter(description = "Filter by product name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by seller type") @RequestParam(required = false) String sellerType,
            @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        Page<ProductResponse> products = productService.searchProducts(name, categoryId, sellerType, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @Operation(summary = "Update product with images", description = "Updates an existing product and optionally its images using multipart form data. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @Parameter(description = "Updated product details (multipart part named \"product\")") @Valid @RequestPart("product") ProductRequest request,
            @Parameter(description = "Additional or replacement images (multipart part named \"images\")") @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        // Verify ownership before any modification to prevent unauthorized updates
        productService.ensureProductOwnership(id, userId);
        ProductResponse product;
        if (images != null && !images.isEmpty()) {
            product = productService.updateProductWithImages(id, request, images);
        } else {
            product = productService.updateProduct(id, request);
        }
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @Operation(summary = "Update product (JSON)", description = "Updates an existing product using a JSON request body. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductJson(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @Parameter(description = "Updated product details") @Valid @RequestBody ProductRequest request) {
        // Verify ownership before any modification to prevent unauthorized updates
        productService.ensureProductOwnership(id, userId);
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @Operation(summary = "Update product status", description = "Updates the lifecycle status of a product. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @Parameter(description = "New product status") @RequestParam ProductStatus status) {
        // Verify ownership before status change to prevent unauthorized status transitions
        productService.ensureProductOwnership(id, userId);
        ProductResponse product = productService.updateProductStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @Operation(summary = "Delete product", description = "Deletes a product and its associated images by identifier. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long id) throws IOException {
        // Verify ownership before deletion to prevent unauthorized removal
        productService.ensureProductOwnership(id, userId);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Product deleted successfully"));
    }

    @Operation(summary = "Upload product image", description = "Uploads a new image to an existing product. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Image uploaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadProductImage(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @Parameter(description = "Image file to upload") @RequestPart("image") MultipartFile image,
            @Parameter(description = "Display order of the image") @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @Parameter(description = "Whether this image should be the primary image") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        // Verify ownership before allowing image upload
        productService.ensureProductOwnership(id, userId);
        ProductImageResponse imageResponse = productService.uploadProductImage(id, image, sortOrder, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(imageResponse));
    }

    @Operation(summary = "Get product images", description = "Retrieves all images associated with a product.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Images retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getProductImages(@Parameter(description = "Product identifier") @PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product.getImages()));
    }

    @Operation(summary = "Update product image", description = "Updates an existing product image. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or image not found")
    })
    @PutMapping(value = "/{productId}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateProductImage(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long productId,
            @Parameter(description = "Image identifier") @PathVariable Long imageId,
            @Parameter(description = "Updated image file") @RequestPart("image") MultipartFile image,
            @Parameter(description = "Display order of the image") @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @Parameter(description = "Whether this image should be the primary image") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        // Verify ownership before allowing image update
        productService.ensureProductOwnership(productId, userId);
        ProductImageResponse imageResponse = productService.updateProductImage(productId, imageId, image, sortOrder, isPrimary);
        return ResponseEntity.ok(ApiResponse.success(imageResponse));
    }

    @Operation(summary = "Set primary image", description = "Marks a specific product image as the primary image. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Primary image set successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or image not found")
    })
    @PutMapping("/{productId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long productId,
            @Parameter(description = "Image identifier") @PathVariable Long imageId) {
        // Verify ownership before changing primary image
        productService.ensureProductOwnership(productId, userId);
        productService.setPrimaryImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @Operation(summary = "Reorder product images", description = "Reorders the images of a product based on the provided list of image IDs. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Images reordered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{productId}/images/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderImages(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long productId,
            @Parameter(description = "Image IDs in the desired order") @RequestBody List<Long> imageIdsInOrder) {
        // Verify ownership before reordering images
        productService.ensureProductOwnership(productId, userId);
        productService.reorderImages(productId, imageIdsInOrder);
        return ResponseEntity.ok(ApiResponse.successMessage("Images reordered successfully"));
    }

@Operation(summary = "Delete product image", description = "Deletes an image from a product. Only the product owner may perform this operation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or image not found")
    })
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId,
            @Parameter(description = "Product identifier") @PathVariable Long productId,
            @Parameter(description = "Image identifier") @PathVariable Long imageId) throws IOException {
        // Verify ownership before allowing image deletion
        productService.ensureProductOwnership(productId, userId);
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }
}
