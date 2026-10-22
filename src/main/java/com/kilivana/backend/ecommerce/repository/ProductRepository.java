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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Serialize stock changes with order reservations; no schema change required. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    
    List<Product> findBySellerId(Long sellerId);

    /** Stable identity for a seeded listing: its name within one seller's catalogue. */
    Optional<Product> findByNameAndSellerId(String name, Long sellerId);

    List<Product> findBySellerIdAndSellerType(Long sellerId, SellerType sellerType);
    
    List<Product> findByCategoryId(Long categoryId);
    
    List<Product> findByStatus(ProductStatus status);
    
    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.createdAt DESC")
    Page<Product> findByStatusWithPagination(@Param("status") ProductStatus status, Pageable pageable);
    
    /**
     * Optional-filter search. The {@code IS NULL} branch parameters are cast
     * explicitly: an uncast parameter used only in a null check has no type
     * PostgreSQL can infer, and the statement is rejected at execution time.
     */
    @Query(value = "SELECT * FROM products p WHERE " +
           "(CAST(:name AS VARCHAR) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(CAST(:categoryId AS BIGINT) IS NULL OR p.category_id = :categoryId) AND " +
           "(CAST(:sellerType AS VARCHAR) IS NULL OR p.seller_type = :sellerType) AND " +
           "p.status = 'ACTIVE'", nativeQuery = true)
    Page<Product> searchProducts(@Param("name") String name,
                                 @Param("categoryId") Long categoryId,
                                 @Param("sellerType") String sellerType,
                                 Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.status = :status")
    List<Product> findBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ProductStatus status);

    long countByStatus(ProductStatus status);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE' AND (p.lowStockThreshold IS NULL AND p.stockQty < 10 OR p.lowStockThreshold IS NOT NULL AND p.stockQty <= p.lowStockThreshold)")
    long countNeedsRestock();

    @Query("SELECT COALESCE(SUM(p.price * p.stockQty), 0) FROM Product p WHERE p.status = 'ACTIVE'")
    BigDecimal calculateLiveStockValue();

    /**
     * Explicit casts keep PostgreSQL from receiving untyped null parameters
     * when the admin list is requested with no filters.
     */
    @Query(value = "SELECT * FROM products p WHERE " +
           "((CAST(:status AS VARCHAR) IS NULL AND p.status <> 'DELETED') OR p.status = CAST(:status AS VARCHAR)) AND " +
           "(CAST(:categoryId AS BIGINT) IS NULL OR p.category_id = :categoryId) AND " +
           "(CAST(:sellerId AS BIGINT) IS NULL OR p.seller_id = :sellerId) AND " +
           "(CAST(:search AS VARCHAR) IS NULL OR p.name ILIKE '%' || :search || '%' OR p.description ILIKE '%' || :search || '%')",
           countQuery = "SELECT COUNT(*) FROM products p WHERE " +
           "((CAST(:status AS VARCHAR) IS NULL AND p.status <> 'DELETED') OR p.status = CAST(:status AS VARCHAR)) AND " +
           "(CAST(:categoryId AS BIGINT) IS NULL OR p.category_id = :categoryId) AND " +
           "(CAST(:sellerId AS BIGINT) IS NULL OR p.seller_id = :sellerId) AND " +
           "(CAST(:search AS VARCHAR) IS NULL OR p.name ILIKE '%' || :search || '%' OR p.description ILIKE '%' || :search || '%')",
           nativeQuery = true)
    Page<Product> findFilteredProductsByStatusName(
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("sellerId") Long sellerId,
            @Param("search") String search,
            Pageable pageable);
    /** Bind enum names explicitly: native SQL otherwise receives ordinal values. */
    default Page<Product> findFilteredProducts(ProductStatus status, Long categoryId, Long sellerId,
                                                String search, Pageable pageable) {
        return findFilteredProductsByStatusName(status == null ? null : status.name(),
                categoryId, sellerId, search, pageable);
    }

}
