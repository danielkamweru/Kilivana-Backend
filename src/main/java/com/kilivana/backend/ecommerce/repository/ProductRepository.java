package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findBySellerId(Long sellerId);
    
    List<Product> findBySellerIdAndSellerType(Long sellerId, SellerType sellerType);
    
    List<Product> findByCategoryId(Long categoryId);
    
    List<Product> findByStatus(ProductStatus status);
    
    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.createdAt DESC")
    Page<Product> findByStatusWithPagination(@Param("status") ProductStatus status, Pageable pageable);
    
    @Query(value = "SELECT * FROM products p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:categoryId IS NULL OR p.category_id = :categoryId) AND " +
           "(:sellerType IS NULL OR p.seller_type = :sellerType) AND " +
           "p.status = 'ACTIVE'", nativeQuery = true)
    Page<Product> searchProducts(@Param("name") String name, 
                                 @Param("categoryId") Long categoryId,
                                 @Param("sellerType") String sellerType,
                                 Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.status = :status")
    List<Product> findBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ProductStatus status);
}
