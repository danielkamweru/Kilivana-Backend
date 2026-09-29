package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);
    
    List<ProductImage> findByProductId(Long productId);
    
    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.isPrimary = true")
    Optional<ProductImage> findPrimaryByProductId(@Param("productId") Long productId);
    
    @Query("SELECT pi FROM ProductImage pi WHERE pi.publicId = :publicId")
    Optional<ProductImage> findByPublicId(@Param("publicId") String publicId);
}
