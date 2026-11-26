package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.RefundTransaction;
import com.kilivana.backend.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for refund transactions.
 */
@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {

    Optional<RefundTransaction> findByReference(String reference);

    Optional<RefundTransaction> findByUuid(UUID uuid);

    List<RefundTransaction> findByPaymentId(Long paymentId);

    List<RefundTransaction> findByOrderId(Long orderId);

    List<RefundTransaction> findBySellerFulfilmentId(Long sellerFulfilmentId);

    List<RefundTransaction> findBySellerId(Long sellerId);

    List<RefundTransaction> findByBuyerId(Long buyerId);

    List<RefundTransaction> findByStatus(PaymentStatus status);

    Optional<RefundTransaction> findByProviderReference(String providerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundTransaction r WHERE r.id = :id")
    Optional<RefundTransaction> findByIdForUpdate(@Param("id") Long id);

    Page<RefundTransaction> findBySellerId(Long sellerId, Pageable pageable);

    Page<RefundTransaction> findByBuyerId(Long buyerId, Pageable pageable);

    Page<RefundTransaction> findByStatus(PaymentStatus status, Pageable pageable);

    Page<RefundTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT SUM(r.amount) FROM RefundTransaction r WHERE r.status = :status AND r.sellerId = :sellerId")
    BigDecimal sumBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") PaymentStatus status);

    @Query("SELECT SUM(r.amount) FROM RefundTransaction r WHERE r.status = :status AND r.paymentId = :paymentId")
    BigDecimal sumByPaymentIdAndStatus(@Param("paymentId") Long paymentId, @Param("status") PaymentStatus status);
}