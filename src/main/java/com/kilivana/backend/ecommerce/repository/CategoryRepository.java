package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.common.enums.SellerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for product categories, partitioned by seller type.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Finds a category by exact name (case-sensitive). */
    Optional<Category> findByName(String name);

    /** Finds a category by name, case-insensitive. */
    Optional<Category> findByNameIgnoreCase(String name);

    /** Returns all categories for a given seller type (FARMER or SUPPLIER). */
    List<Category> findByType(SellerType type);

    /** Returns only active categories (for buyer-facing dropdowns). */
    List<Category> findByActiveTrue();

    /** Returns active categories filtered by seller type. */
    List<Category> findByTypeAndActiveTrue(SellerType type);

    /** Checks if a category name already exists (for uniqueness validation). */
    boolean existsByName(String name);
}
