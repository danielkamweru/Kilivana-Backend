package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.WalletTransaction;
import com.kilivana.backend.finance.enums.WalletTransactionStatus;
import com.kilivana.backend.finance.enums.WalletTransactionType;
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
import java.util.UUID;

/**
 * Persistence for wallet transactions.
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByReference(String reference);

    Optional<WalletTransaction> findByUuid(UUID uuid);

    List<WalletTransaction> findByWalletId(Long walletId);

    long countByWalletId(Long walletId);

    List<WalletTransaction> findBySellerId(Long sellerId);

    List<WalletTransaction> findByOrderId(Long orderId);

    List<WalletTransaction> findBySellerFulfilmentId(Long sellerFulfilmentId);

    List<WalletTransaction> findByTransactionType(WalletTransactionType type);

    List<WalletTransaction> findByStatus(WalletTransactionStatus status);

    Page<WalletTransaction> findByWalletId(Long walletId, Pageable pageable);

    Page<WalletTransaction> findBySellerId(Long sellerId, Pageable pageable);

    Page<WalletTransaction> findByTransactionType(WalletTransactionType type, Pageable pageable);

    Page<WalletTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT SUM(wt.amount) FROM WalletTransaction wt WHERE wt.sellerId = :sellerId AND wt.transactionType = :type AND wt.status = :status")
    BigDecimal sumBySellerIdAndTypeAndStatus(@Param("sellerId") Long sellerId, @Param("type") WalletTransactionType type, @Param("status") WalletTransactionStatus status);

    @Query("SELECT SUM(wt.amount) FROM WalletTransaction wt WHERE wt.orderId = :orderId AND wt.transactionType = :type AND wt.status = :status")
    BigDecimal sumByOrderIdAndTypeAndStatus(@Param("orderId") Long orderId, @Param("type") WalletTransactionType type, @Param("status") WalletTransactionStatus status);

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.orderId = :orderId AND wt.transactionType = :type AND wt.status = :status")
    List<WalletTransaction> findByOrderIdAndTypeAndStatus(@Param("orderId") Long orderId, @Param("type") WalletTransactionType type, @Param("status") WalletTransactionStatus status);

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.sellerFulfilmentId = :fulfilmentId AND wt.transactionType = :type AND wt.status = :status")
    List<WalletTransaction> findByFulfilmentIdAndTypeAndStatus(@Param("fulfilmentId") Long fulfilmentId, @Param("type") WalletTransactionType type, @Param("status") WalletTransactionStatus status);
}