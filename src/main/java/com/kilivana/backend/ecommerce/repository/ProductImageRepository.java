package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for product images.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** Returns all images for a product, ordered by gallery sort order. */
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    /** Returns all images for a product (unordered). */
    List<ProductImage> findByProductId(Long productId);

    /** Returns the primary image for a product, if one exists. */
    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.isPrimary = true")
    Optional<ProductImage> findPrimaryByProductId(@Param("productId") Long productId);

    /** Finds an image by its Cloudinary public ID (for deletion/replacement). */
    @Query("SELECT pi FROM ProductImage pi WHERE pi.publicId = :publicId")
    Optional<ProductImage> findByPublicId(@Param("publicId") String publicId);
}
