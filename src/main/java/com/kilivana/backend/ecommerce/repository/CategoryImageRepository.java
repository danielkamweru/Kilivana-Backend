package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.CategoryImage;
import com.kilivana.backend.common.repository.ImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for category images, extending the shared {@link ImageRepository}
 * for common image operations (by publicId, primary lookup, etc.).
 */
@Repository
public interface CategoryImageRepository extends JpaRepository<CategoryImage, Long>, ImageRepository<CategoryImage> {

    List<CategoryImage> findByCategoryIdOrderBySortOrderAsc(Long categoryId);

    Optional<CategoryImage> findByCategoryIdAndIsPrimaryTrue(Long categoryId);
}