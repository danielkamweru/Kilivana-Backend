package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.JournalLine;
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

/**
 * Persistence for journal lines.
 */
@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByJournalEntryId(Long journalEntryId);

    List<JournalLine> findByAccountId(Long accountId);

    List<JournalLine> findBySellerId(Long sellerId);

    @Query("SELECT l FROM JournalLine l JOIN l.journalEntry j WHERE j.posted = true AND l.account.id = :accountId ORDER BY j.entryDate DESC")
    List<JournalLine> findPostedByAccountIdOrderByDateDesc(Long accountId);

    @Query("SELECT l FROM JournalLine l JOIN l.journalEntry j WHERE j.posted = true AND l.sellerId = :sellerId ORDER BY j.entryDate DESC")
    List<JournalLine> findPostedBySellerIdOrderByDateDesc(Long sellerId);

    @Query("SELECT SUM(l.debitAmount) - SUM(l.creditAmount) FROM JournalLine l JOIN l.journalEntry j WHERE j.posted = true AND l.account.id = :accountId")
    BigDecimal getAccountBalance(Long accountId);

    @Query("SELECT SUM(l.debitAmount) - SUM(l.creditAmount) FROM JournalLine l JOIN l.journalEntry j WHERE j.posted = true AND l.sellerId = :sellerId AND l.account.accountType = :accountType")
    BigDecimal getSellerAccountBalance(@Param("sellerId") Long sellerId, @Param("accountType") com.kilivana.backend.finance.enums.LedgerAccountType accountType);

    Page<JournalLine> findByJournalEntryId(Long journalEntryId, Pageable pageable);
}