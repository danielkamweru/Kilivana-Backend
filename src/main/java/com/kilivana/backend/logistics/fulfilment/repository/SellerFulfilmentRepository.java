package com.kilivana.backend.logistics.fulfilment.repository;

import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for seller fulfilment records.
 */
@Repository
public interface SellerFulfilmentRepository extends JpaRepository<SellerFulfilment, Long> {

    List<SellerFulfilment> findByOrderId(Long orderId);

    List<SellerFulfilment> findBySellerId(Long sellerId);

    Optional<SellerFulfilment> findByOrderIdAndSellerId(Long orderId, Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM SellerFulfilment f WHERE f.id = :id")
    Optional<SellerFulfilment> findByIdForUpdate(@Param("id") Long id);

    List<SellerFulfilment> findByStatus(com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus status);
}
