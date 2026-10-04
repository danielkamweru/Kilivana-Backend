package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByBuyerId(Long buyerId);

    /** The reference is unique, so it identifies a seed record across restarts. */
    Optional<Order> findByCode(String code);
    
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

    long countByStatus(OrderStatus status);

    long countByStatusIn(List<OrderStatus> statuses);

    /** Named queries cannot express {@code >=}, so the range is written out explicitly. */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.status IN :statuses AND o.createdAt >= :since")
    long countByStatusInSince(@Param("statuses") List<OrderStatus> statuses,
                              @Param("since") LocalDateTime since);

    /**
     * Per-day order count and revenue, grouped in the database so the dashboard does not
     * have to pull every order in the window back to aggregate it.
     */
    @Query("SELECT FUNCTION('date', o.createdAt), COUNT(o), COALESCE(SUM(o.total), 0) " +
           "FROM Order o WHERE o.status IN :statuses AND o.createdAt >= :since " +
           "GROUP BY FUNCTION('date', o.createdAt) ORDER BY FUNCTION('date', o.createdAt)")
    List<Object[]> dailyTotalsByStatusInSince(@Param("statuses") List<OrderStatus> statuses,
                                              @Param("since") LocalDateTime since);

    /**
     * Completed revenue for the calendar month, used by the admin dashboard. Cancelled and
     * failed orders are excluded so the figure matches what was actually settled.
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
           "WHERE o.status IN :statuses AND o.createdAt >= :since")
    BigDecimal sumTotalByStatusInSince(@Param("statuses") List<OrderStatus> statuses,
                                       @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(AVG(o.total), 0) FROM Order o WHERE o.status IN :statuses")
    BigDecimal averageTotalByStatusIn(@Param("statuses") List<OrderStatus> statuses);
}
