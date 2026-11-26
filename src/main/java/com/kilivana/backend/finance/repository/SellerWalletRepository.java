package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.SellerWallet;
import com.kilivana.backend.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for seller wallets.
 */
@Repository
public interface SellerWalletRepository extends JpaRepository<SellerWallet, Long> {

    Optional<SellerWallet> findBySellerId(Long sellerId);

    Optional<SellerWallet> findByUuid(UUID uuid);

    List<SellerWallet> findBySellerType(UserRole sellerType);

    List<SellerWallet> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM SellerWallet w WHERE w.id = :id")
    Optional<SellerWallet> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM SellerWallet w WHERE w.sellerId = :sellerId")
    Optional<SellerWallet> findBySellerIdForUpdate(@Param("sellerId") Long sellerId);

    @Query("SELECT SUM(w.pendingBalance) FROM SellerWallet w WHERE w.sellerType = :type")
    BigDecimal sumPendingBalanceByType(@Param("type") UserRole type);

    @Query("SELECT SUM(w.availableBalance) FROM SellerWallet w WHERE w.sellerType = :type")
    BigDecimal sumAvailableBalanceByType(@Param("type") UserRole type);

    @Query("SELECT SUM(w.processingBalance) FROM SellerWallet w WHERE w.sellerType = :type")
    BigDecimal sumProcessingBalanceByType(@Param("type") UserRole type);

    @Query("SELECT SUM(w.totalPaid) FROM SellerWallet w WHERE w.sellerType = :type")
    BigDecimal sumTotalPaidByType(@Param("type") UserRole type);
}