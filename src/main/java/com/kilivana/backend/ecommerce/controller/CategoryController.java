package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.CategoryRequest;
import com.kilivana.backend.ecommerce.dto.CategoryResponse;
import com.kilivana.backend.ecommerce.service.CategoryService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.SellerType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing product categories.
 * Supports creation, retrieval, updates, status toggling and deletion.
 * Categories are scoped to seller types (e.g., FARMER, SHOP).
 */
@Tag(name = "E-Commerce · Catalog", description = "Product category management")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Creates a new product category.
     *
     * @param request the category name and seller type
     * @return the created category
     */
    @Operation(summary = "Create category", description = "Creates a new product category and returns the created category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Parameter(description = "Category creation details") @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(category));
    }

    /**
     * Retrieves a single product category by its identifier.
     *
     * @param id the category identifier
     * @return the category
     */
    @Operation(summary = "Get category by id", description = "Retrieves a single product category by its identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@Parameter(description = "Category identifier") @PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Returns all product categories.
     * Note: pagination parameters are accepted but not currently applied to the result.
     *
     * @param pageable pagination and sorting parameters
     * @return list of all categories
     */
    @Operation(summary = "List all categories", description = "Returns all product categories.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(@Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Returns all categories associated with a specific seller type.
     *
     * @param type the seller type to filter by (e.g., FARMER, SHOP)
     * @return list of categories for that seller type
     */
    @Operation(summary = "Get categories by type", description = "Returns all categories associated with a specific seller type.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid seller type")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesByType(@Parameter(description = "Seller type to filter categories by") @PathVariable SellerType type) {
        List<CategoryResponse> categories = categoryService.getCategoriesByType(type);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Returns all categories that are currently active.
     *
     * @return list of active categories
     */
    @Operation(summary = "Get active categories", description = "Returns all categories that are currently active.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active categories retrieved successfully")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Updates an existing product category by its identifier.
     *
     * @param id the category identifier
     * @param request the updated category details
     * @return the updated category
     */
    @Operation(summary = "Update category", description = "Updates an existing product category by its identifier and returns the updated category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id,
            @Parameter(description = "Updated category details") @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Toggles the active status of a product category.
     *
     * @param id the category identifier
     * @param active the new active status (true = active, false = inactive)
     * @return the updated category
     */
    @Operation(summary = "Update category status", description = "Toggles the active status of a product category and returns the updated category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid active flag value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryStatus(
            @Parameter(description = "Category identifier") @PathVariable Long id,
            @Parameter(description = "New active status") @RequestParam Boolean active) {
        CategoryResponse category = categoryService.updateCategoryStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Deletes a product category by its identifier.
     *
     * @param id the category identifier
     * @return success message
     */
    @Operation(summary = "Delete category", description = "Deletes a product category by its identifier and returns a confirmation message.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@Parameter(description = "Category identifier") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Category deleted successfully"));
    }
}
