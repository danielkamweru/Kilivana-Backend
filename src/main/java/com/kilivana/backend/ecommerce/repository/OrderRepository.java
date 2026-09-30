package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByBuyerId(Long buyerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId ORDER BY o.createdAt DESC")
    Page<Order> findByBuyerIdWithPagination(@Param("buyerId") Long buyerId, Pageable pageable);
    
    /**
     * Optional-filter search. The {@code IS NULL} branch parameters are cast
     * explicitly: an uncast parameter used only in a null check has no type
     * PostgreSQL can infer, and the statement is rejected at execution time.
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(CAST(:buyerId AS Long) IS NULL OR o.buyerId = :buyerId) AND " +
           "(CAST(:status AS String) IS NULL OR o.status = :status) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(@Param("buyerId") Long buyerId,
                             @Param("status") OrderStatus status,
                             Pageable pageable);
}
