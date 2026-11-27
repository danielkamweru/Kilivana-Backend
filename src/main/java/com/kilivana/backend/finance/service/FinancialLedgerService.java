package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.LedgerAccountType;
import com.kilivana.backend.finance.entity.JournalEntry;
import com.kilivana.backend.finance.entity.JournalLine;
import com.kilivana.backend.finance.entity.LedgerAccount;
import com.kilivana.backend.finance.repository.JournalEntryRepository;
import com.kilivana.backend.finance.repository.JournalLineRepository;
import com.kilivana.backend.finance.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core double-entry financial ledger service.
 * Ensures every journal entry balances (debits = credits) and maintains account balances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialLedgerService {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final UserRepository userRepository;
    private final FinancialAuditLogService auditLogService;

    private static final AtomicLong journalSequence = new AtomicLong(0);

    /**
     * Posts a balanced journal entry with multiple debit and credit lines.
     * Validates that total debits equal total credits before posting.
     *
     * @param request the journal entry to post
     * @return the posted journal entry
     */
    @Transactional
    public JournalEntry postJournalEntry(FinanceDtos.PostJournalRequest request) {
        validateJournalRequest(request);

        // Generate reference if not provided
        String reference = request.getReference();
        if (reference == null || reference.isBlank()) {
            reference = generateJournalReference();
        }

        // Check for duplicate reference
        if (journalEntryRepository.findByReference(reference).isPresent()) {
            throw new BadRequestException("Journal entry with reference " + reference + " already exists");
        }

        // Create journal entry
        JournalEntry entry = JournalEntry.builder()
                .reference(reference)
                .entryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDateTime.now())
                .description(request.getDescription())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .createdBy(request.getCreatedBy())
                .posted(true)
                .postedAt(LocalDateTime.now())
                .build();
        entry = journalEntryRepository.save(entry);

        // Create journal lines and update account balances
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (FinanceDtos.JournalLineRequest lineRequest : request.getLines()) {
            JournalLine line = createJournalLine(entry, lineRequest);
            journalLineRepository.save(line);

            if (line.isDebit()) {
                totalDebits = totalDebits.add(line.getDebitAmount());
            } else {
                totalCredits = totalCredits.add(line.getCreditAmount());
            }

            // Update account running balance
            updateAccountBalance(lineRequest.getAccountId(), line.getDebitAmount(), line.getCreditAmount());
        }

        // Final validation: debits must equal credits
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BadRequestException("Journal entry does not balance: debits=" + totalDebits + ", credits=" + totalCredits);
        }

        log.info("Posted journal entry {}: debits={}, credits={}", reference, totalDebits, totalCredits);

        // Audit log
        auditLogService.log(
                request.getCreatedBy(),
                "JOURNAL_POSTED",
                "JournalEntry",
                entry.getId(),
                String.format("reference=%s, debits=%s, credits=%s, lines=%d",
                        reference, totalDebits, totalCredits, request.getLines().size())
        );

        return entry;
    }

    /**
     * Creates a simple two-line journal entry (one debit, one credit).
     */
    @Transactional
    public JournalEntry postSimpleJournalEntry(
            String sourceType,
            Long sourceId,
            String description,
            Long createdBy,
            Long debitAccountId,
            Long creditAccountId,
            BigDecimal amount,
            String lineDescription
    ) {
        var debitLine = FinanceDtos.JournalLineRequest.builder()
                .accountId(debitAccountId)
                .debitAmount(amount)
                .description(lineDescription)
                .build();

        var creditLine = FinanceDtos.JournalLineRequest.builder()
                .accountId(creditAccountId)
                .creditAmount(amount)
                .description(lineDescription)
                .build();

        return postJournalEntry(FinanceDtos.PostJournalRequest.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .description(description)
                .createdBy(createdBy)
                .lines(List.of(debitLine, creditLine))
                .build());
    }

    /**
     * Reverses a posted journal entry by creating an equal and opposite entry.
     */
    @Transactional
    public JournalEntry reverseJournalEntry(Long entryId, Long reversedBy, String reason) {
        JournalEntry original = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry", entryId));

        if (!original.isPosted()) {
            throw new BadRequestException("Cannot reverse an unposted journal entry");
        }

        // Check if already reversed
        if (journalEntryRepository.existsByReversalOf(entryId)) {
            throw new BadRequestException("Journal entry " + entryId + " has already been reversed");
        }

        // Build reversal lines (swap debits and credits)
        List<JournalLine> originalLines = journalLineRepository.findByJournalEntryId(entryId);
        var reversalLines = new java.util.ArrayList<FinanceDtos.JournalLineRequest>();

        for (JournalLine line : originalLines) {
            if (line.isDebit()) {
                reversalLines.add(FinanceDtos.JournalLineRequest.builder()
                        .accountId(line.getAccountId())
                        .accountType(line.getAccountType())
                        .sellerId(line.getSellerId())
                        .creditAmount(line.getDebitAmount())
                        .description("Reversal: " + (line.getDescription() != null ? line.getDescription() : reason))
                        .build());
            } else {
                reversalLines.add(FinanceDtos.JournalLineRequest.builder()
                        .accountId(line.getAccountId())
                        .accountType(line.getAccountType())
                        .sellerId(line.getSellerId())
                        .debitAmount(line.getCreditAmount())
                        .description("Reversal: " + (line.getDescription() != null ? line.getDescription() : reason))
                        .build());
            }
        }

        JournalEntry reversal = postJournalEntry(FinanceDtos.PostJournalRequest.builder()
                .sourceType("REVERSAL")
                .sourceId(entryId)
                .description("Reversal of " + original.getReference() + ": " + reason)
                .createdBy(reversedBy)
                .lines(reversalLines)
                .build());

        // Link reversal to original
        original.setReversalOf(reversal.getId());
        journalEntryRepository.save(original);

        auditLogService.log(
                reversedBy,
                "JOURNAL_REVERSED",
                "JournalEntry",
                entryId,
                String.format("reversed_by=%s, reversal_id=%s, reason=%s", reversedBy, reversal.getId(), reason)
        );

        return reversal;
    }

    /**
     * Gets or creates a seller-specific ledger account.
     */
    @Transactional
    public LedgerAccount getOrCreateSellerAccount(Long sellerId, LedgerAccountType accountType, String name) {
        return ledgerAccountRepository.findByAccountTypeAndSellerId(accountType, sellerId)
                .orElseGet(() -> {
                    LedgerAccount account = LedgerAccount.builder()
                            .accountType(accountType)
                            .sellerId(sellerId)
                            .name(name)
                            .balance(BigDecimal.ZERO)
                            .active(true)
                            .build();
                    return ledgerAccountRepository.save(account);
                });
    }

    /**
     * Gets or creates a platform-level ledger account.
     */
    @Transactional
    public LedgerAccount getOrCreatePlatformAccount(LedgerAccountType accountType) {
        return ledgerAccountRepository.findByAccountTypeAndSellerIdIsNull(accountType)
                .orElseGet(() -> {
                    LedgerAccount account = LedgerAccount.builder()
                            .accountType(accountType)
                            .name(accountType.name().replace("_", " "))
                            .balance(BigDecimal.ZERO)
                            .active(true)
                            .build();
                    return ledgerAccountRepository.save(account);
                });
    }

    /**
     * Updates a ledger account's running balance.
     * Debits increase balance for asset/expense accounts; credits increase for liability/revenue.
     */
    private void updateAccountBalance(Long accountId, BigDecimal debitAmount, BigDecimal creditAmount) {
        LedgerAccount account = ledgerAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerAccount", accountId));

        BigDecimal debit = debitAmount == null ? BigDecimal.ZERO : debitAmount;
        BigDecimal credit = creditAmount == null ? BigDecimal.ZERO : creditAmount;

        // For normal debit accounts (assets, expenses): debit increases, credit decreases
        // For normal credit accounts (liabilities, revenue): credit increases, debit decreases
        if (account.getAccountType().isDebitNormal()) {
            account.setBalance(account.getBalance().add(debit).subtract(credit));
        } else {
            account.setBalance(account.getBalance().add(credit).subtract(debit));
        }

        ledgerAccountRepository.save(account);
    }

    private void validateJournalRequest(FinanceDtos.PostJournalRequest request) {
        if (request.getLines() == null || request.getLines().size() < 2) {
            throw new BadRequestException("Journal entry must have at least two lines");
        }

        boolean hasDebit = false;
        boolean hasCredit = false;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (FinanceDtos.JournalLineRequest line : request.getLines()) {
            if (line.getDebitAmount() != null && line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                    throw new BadRequestException("Journal line cannot have both debit and credit amounts");
                }
                hasDebit = true;
                totalDebits = totalDebits.add(line.getDebitAmount());
            } else if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                hasCredit = true;
                totalCredits = totalCredits.add(line.getCreditAmount());
            } else {
                throw new BadRequestException("Journal line must have either debit or credit amount");
            }

            if (line.getAccountId() == null) {
                throw new BadRequestException("Journal line must have an account ID");
            }
        }

        if (!hasDebit || !hasCredit) {
            throw new BadRequestException("Journal entry must have at least one debit and one credit line");
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BadRequestException("Journal entry does not balance: debits=" + totalDebits + ", credits=" + totalCredits);
        }

        if (request.getCreatedBy() == null) {
            throw new BadRequestException("Journal entry must have a creator (createdBy)");
        }

        if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            throw new BadRequestException("Journal entry must have a source type");
        }
    }

    private JournalLine createJournalLine(JournalEntry entry, FinanceDtos.JournalLineRequest lineRequest) {
        LedgerAccount account = ledgerAccountRepository.findById(lineRequest.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("LedgerAccount", lineRequest.getAccountId()));

        return JournalLine.builder()
                .journalEntry(entry)
                .account(account)
                .accountType(lineRequest.getAccountType() != null ? lineRequest.getAccountType() : account.getAccountType())
                .sellerId(lineRequest.getSellerId())
                .debitAmount(lineRequest.getDebitAmount() == null ? BigDecimal.ZERO : lineRequest.getDebitAmount())
                .creditAmount(lineRequest.getCreditAmount() == null ? BigDecimal.ZERO : lineRequest.getCreditAmount())
                .description(lineRequest.getDescription())
                .build();
    }

    private String generateJournalReference() {
        long seq = journalSequence.incrementAndGet();
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("JE-%s-%04d", datePart, seq);
    }

    @Transactional(readOnly = true)
    public FinanceDtos.JournalEntryResponse getJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry", id));
        List<JournalLine> lines = journalLineRepository.findByJournalEntryId(id);
        return FinanceDtos.JournalEntryResponse.fromEntity(entry, lines);
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.LedgerAccountResponse> getAllAccounts() {
        return ledgerAccountRepository.findAll().stream()
                .map(FinanceDtos.LedgerAccountResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinanceDtos.LedgerAccountResponse getAccount(Long id) {
        LedgerAccount account = ledgerAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerAccount", id));
        return FinanceDtos.LedgerAccountResponse.fromEntity(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        return journalLineRepository.getAccountBalance(accountId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getSellerPayableBalance(Long sellerId) {
        BigDecimal balance = journalLineRepository.getSellerAccountBalance(sellerId, LedgerAccountType.SELLER_PAYABLES);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    @Transactional(readOnly = true)
    public BigDecimal getPlatformCommissionRevenue(LocalDateTime from, LocalDateTime to) {
        BigDecimal credits = journalEntryRepository.sumCreditsByAccountTypeAndDateRange(LedgerAccountType.PLATFORM_COMMISSION_REVENUE, from, to);
        BigDecimal debits = journalEntryRepository.sumDebitsByAccountTypeAndDateRange(LedgerAccountType.PLATFORM_COMMISSION_REVENUE, from, to);
        BigDecimal revenue = credits == null ? BigDecimal.ZERO : credits;
        revenue = revenue.subtract(debits == null ? BigDecimal.ZERO : debits);
        return revenue;
    }
}