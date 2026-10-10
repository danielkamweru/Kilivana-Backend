package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.dto.CategoryRequest;
import com.kilivana.backend.ecommerce.dto.CategoryResponse;
import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Product categories, partitioned by seller type (FARMER vs SUPPLIER).
 *
 * <p>Categories are created by administrators and can be toggled active/inactive. A category
 * name is unique globally (not per type) so the buyer's filter dropdown never shows duplicates.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Creates a new category. Names must be globally unique.
     *
     * @throws BadRequestException if a category with the same name already exists
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .type(request.getType())
                .active(true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    /**
     * Retrieves a category by its ID.
     *
     * @throws ResourceNotFoundException if the category does not exist
     */
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return mapToResponse(category);
    }

    /** Returns all categories (active and inactive). */
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Returns categories filtered by seller type (FARMER or SUPPLIER). */
    public List<CategoryResponse> getCategoriesByType(SellerType type) {
        return categoryRepository.findByType(type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Returns only active categories for the buyer's filter dropdown. */
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates a category's name and type.
     *
     * @throws ResourceNotFoundException if the category does not exist
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setName(request.getName());
        category.setType(request.getType());

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    /**
     * Toggles a category's active status.
     *
     * @throws ResourceNotFoundException if the category does not exist
     */
    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, Boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setActive(active);
        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    /**
     * Deletes a category.
     *
     * @throws ResourceNotFoundException if the category does not exist
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        categoryRepository.delete(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
