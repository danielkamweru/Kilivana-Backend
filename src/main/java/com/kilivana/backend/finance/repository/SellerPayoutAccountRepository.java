package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.SellerPayoutAccount;
import com.kilivana.backend.finance.enums.PayoutMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for seller payout accounts.
 */
@Repository
public interface SellerPayoutAccountRepository extends JpaRepository<SellerPayoutAccount, Long> {

    List<SellerPayoutAccount> findBySellerId(Long sellerId);

    Optional<SellerPayoutAccount> findBySellerIdAndIsDefaultTrue(Long sellerId);

    Optional<SellerPayoutAccount> findBySellerIdAndPayoutMethod(Long sellerId, PayoutMethod payoutMethod);

    Optional<SellerPayoutAccount> findByUuid(UUID uuid);

    @Query("SELECT a FROM SellerPayoutAccount a WHERE a.sellerId = :sellerId AND a.verificationStatus = 'VERIFIED'")
    List<SellerPayoutAccount> findVerifiedBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM SellerPayoutAccount a WHERE a.sellerId = :sellerId AND a.isDefault = true")
    boolean existsDefaultBySellerId(@Param("sellerId") Long sellerId);
}