package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.SettlementAllocation;
import com.kilivana.backend.finance.enums.SettlementAllocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementAllocationRepository extends JpaRepository<SettlementAllocation, Long> {

    List<SettlementAllocation> findBySettlementId(Long settlementId);

    List<SettlementAllocation> findByRecipientUserId(Long recipientUserId);

    List<SettlementAllocation> findByAllocationType(SettlementAllocationType type);

    @Query("SELECT SUM(sa.amount) FROM SettlementAllocation sa WHERE sa.recipientUserId = :userId AND sa.allocationType = :type")
    BigDecimal getTotalByRecipientAndType(@Param("userId") Long userId, @Param("type") SettlementAllocationType type);

    @Query("SELECT sa FROM SettlementAllocation sa WHERE sa.walletTransactionId = :txnId")
    Optional<SettlementAllocation> findByWalletTransactionId(@Param("txnId") Long txnId);

    @Query("SELECT sa FROM SettlementAllocation sa WHERE sa.ledgerJournalEntryId = :entryId")
    Optional<SettlementAllocation> findByLedgerJournalEntryId(@Param("entryId") Long entryId);
}