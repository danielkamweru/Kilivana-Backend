package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.LedgerAccount;
import com.kilivana.backend.finance.enums.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for ledger accounts.
 */
@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByAccountTypeAndSellerId(LedgerAccountType accountType, Long sellerId);

    Optional<LedgerAccount> findByAccountTypeAndSellerIdIsNull(LedgerAccountType accountType);

    List<LedgerAccount> findByAccountType(LedgerAccountType accountType);

    List<LedgerAccount> findBySellerId(Long sellerId);

    List<LedgerAccount> findByActiveTrue();

    Optional<LedgerAccount> findByUuid(UUID uuid);

    @Query("SELECT a FROM LedgerAccount a WHERE a.accountType = :type AND a.sellerId IS NULL")
    Optional<LedgerAccount> findPlatformAccountByType(@Param("type") LedgerAccountType type);

    @Query("SELECT a FROM LedgerAccount a WHERE a.accountType = :type AND a.sellerId = :sellerId")
    Optional<LedgerAccount> findSellerAccountByType(@Param("type") LedgerAccountType type, @Param("sellerId") Long sellerId);
}