package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.SellerPayout;
import com.kilivana.backend.finance.enums.PayoutStatus;
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
 * Persistence for seller payouts/withdrawals.
 */
@Repository
public interface SellerPayoutRepository extends JpaRepository<SellerPayout, Long> {

    Optional<SellerPayout> findByReference(String reference);

    Optional<SellerPayout> findByUuid(UUID uuid);

    List<SellerPayout> findBySellerId(Long sellerId);

    List<SellerPayout> findByWalletId(Long walletId);

    List<SellerPayout> findByPayoutAccountId(Long payoutAccountId);

    List<SellerPayout> findByStatus(PayoutStatus status);

    Optional<SellerPayout> findByProviderReference(String providerReference);

    List<SellerPayout> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SellerPayout p WHERE p.id = :id")
    Optional<SellerPayout> findByIdForUpdate(@Param("id") Long id);

    Page<SellerPayout> findBySellerId(Long sellerId, Pageable pageable);

    Page<SellerPayout> findByStatus(PayoutStatus status, Pageable pageable);

    Page<SellerPayout> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT p FROM SellerPayout p WHERE p.status IN (:statuses) ORDER BY p.createdAt ASC")
    List<SellerPayout> findByStatusInOrderByCreatedAtAsc(@Param("statuses") List<PayoutStatus> statuses);

    @Query("SELECT SUM(p.amount) FROM SellerPayout p WHERE p.status = :status AND p.sellerId = :sellerId")
    BigDecimal sumBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") PayoutStatus status);

    @Query("SELECT SUM(p.amount) FROM SellerPayout p WHERE p.status = :status AND p.createdAt BETWEEN :from AND :to")
    BigDecimal sumByStatusAndDateRange(@Param("status") PayoutStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM SellerPayout p WHERE p.status = :status AND p.createdAt BETWEEN :from AND :to")
    long countByStatusAndDateRange(@Param("status") PayoutStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}