package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for journal entries.
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByReference(String reference);

    Optional<JournalEntry> findByUuid(UUID uuid);

    List<JournalEntry> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    Page<JournalEntry> findByEntryDateBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<JournalEntry> findByPostedTrueOrderByEntryDateDesc(Pageable pageable);

    Page<JournalEntry> findByCreatedBy(Long createdBy, Pageable pageable);

    @Query("SELECT j FROM JournalEntry j WHERE j.posted = true AND j.entryDate BETWEEN :from AND :to ORDER BY j.entryDate DESC")
    Page<JournalEntry> findPostedByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT COUNT(j) FROM JournalEntry j WHERE j.posted = true AND j.entryDate BETWEEN :from AND :to")
    long countPostedByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(l.debitAmount) FROM JournalLine l JOIN l.journalEntry j WHERE j.posted = true AND l.account.accountType = :accountType AND j.entryDate BETWEEN :from AND :to")
    BigDecimal sumDebitsByAccountTypeAndDateRange(@Param("accountType") com.kilivana.backend.finance.enums.LedgerAccountType accountType, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(l.creditAmount) FROM JournalLine l JOIN l.journalEntry j WHERE j.posted = true AND l.account.accountType = :accountType AND j.entryDate BETWEEN :from AND :to")
    BigDecimal sumCreditsByAccountTypeAndDateRange(@Param("accountType") com.kilivana.backend.finance.enums.LedgerAccountType accountType, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}