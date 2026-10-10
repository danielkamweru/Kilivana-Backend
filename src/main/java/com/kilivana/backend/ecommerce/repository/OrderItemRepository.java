package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Persistence for order line items.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Returns all line items for a specific order. */
    List<OrderItem> findByOrderId(Long orderId);

    /** Returns all line items for a specific seller (across all their orders). */
    List<OrderItem> findBySellerId(Long sellerId);

    /**
     * Category name to order count, for the dashboard breakdown. Only settled orders count,
     * so a cancelled basket does not inflate a category's total.
     */
    @Query("SELECT c.name, COUNT(oi) FROM OrderItem oi " +
           "JOIN Order o ON o.id = oi.orderId " +
           "JOIN Product p ON p.id = oi.productId " +
           "JOIN Category c ON c.id = p.categoryId " +
           "WHERE o.status IN :statuses " +
           "GROUP BY c.name ORDER BY COUNT(oi) DESC")
    List<Object[]> countByCategoryForStatuses(@Param("statuses") List<OrderStatus> statuses);
}
