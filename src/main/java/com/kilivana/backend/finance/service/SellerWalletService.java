package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.WalletTransactionStatus;
import com.kilivana.backend.finance.enums.WalletTransactionType;
import com.kilivana.backend.finance.entity.SellerWallet;
import com.kilivana.backend.finance.entity.WalletTransaction;
import com.kilivana.backend.finance.repository.SellerWalletRepository;
import com.kilivana.backend.finance.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing seller wallets and wallet transactions.
 * All balance changes go through this service to maintain consistency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerWalletService {

    private final SellerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinancialLedgerService ledgerService;
    private final FinancialAuditLogService auditLogService;

    /**
     * Gets or creates a wallet for a seller (farmer or supplier).
     */
    @Transactional
    public SellerWallet getOrCreateWallet(Long sellerId) {
        return walletRepository.findBySellerId(sellerId)
                .orElseGet(() -> createWallet(sellerId));
    }

    private SellerWallet createWallet(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", sellerId));

        if (seller.getRole() != UserRole.FARMER
                && seller.getRole() != UserRole.SUPPLIER
                && seller.getRole() != UserRole.TRANSPORT_PROVIDER) {
            throw new BadRequestException("User " + sellerId + " is not a farmer, supplier, or transport provider");
        }

        // Get or create appropriate payable ledger account based on role
        com.kilivana.backend.finance.enums.LedgerAccountType accountType;
        String accountName;
        if (seller.getRole() == UserRole.TRANSPORT_PROVIDER) {
            accountType = com.kilivana.backend.finance.enums.LedgerAccountType.TRANSPORT_PAYABLES;
            accountName = "Transport Payable - " + seller.getName();
        } else {
            accountType = com.kilivana.backend.finance.enums.LedgerAccountType.SELLER_PAYABLES;
            accountName = "Payable - " + seller.getName();
        }
        ledgerService.getOrCreateSellerAccount(sellerId, accountType, accountName);

        SellerWallet wallet = SellerWallet.builder()
                .sellerId(sellerId)
                .sellerType(seller.getRole())
                .pendingBalance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .processingBalance(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        SellerWallet saved = walletRepository.save(wallet);
        log.info("Created wallet {} for seller {} ({})", saved.getId(), sellerId, seller.getRole().name());
        return saved;
    }

    /**
     * Credits a seller's wallet from a sale (after commission deduction).
     * Adds to pending balance until delivery is confirmed.
     */
    @Transactional
    public WalletTransaction creditSale(
            Long sellerId,
            Long orderId,
            Long sellerFulfilmentId,
            BigDecimal grossAmount,
            BigDecimal commissionAmount,
            BigDecimal netAmount,
            String reference,
            String description
    ) {
        SellerWallet wallet = getOrCreateWallet(sellerId);

        // Validate no duplicate credit for this fulfilment
        if (transactionRepository.findByFulfilmentIdAndTypeAndStatus(sellerFulfilmentId, WalletTransactionType.SALE_CREDIT, WalletTransactionStatus.COMPLETED).size() > 0) {
            throw new BadRequestException("Sale already credited for fulfilment " + sellerFulfilmentId);
        }

        // Create wallet transaction
        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .orderId(orderId)
                .sellerFulfilmentId(sellerFulfilmentId)
                .transactionType(WalletTransactionType.SALE_CREDIT)
                .amount(netAmount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description(description)
                .balanceAfter(wallet.getTotalBalance().add(netAmount))
                .pendingBalanceAfter(wallet.getPendingBalance().add(netAmount))
                .availableBalanceAfter(wallet.getAvailableBalance())
                .processingBalanceAfter(wallet.getProcessingBalance())
                .build();
        transactionRepository.save(txn);

        // Update wallet balances
        wallet.setPendingBalance(wallet.getPendingBalance().add(netAmount));
        wallet.setTotalEarned(wallet.getTotalEarned().add(netAmount));
        walletRepository.save(wallet);

        // Create ledger entry: Debit Payment Clearing, Credit Seller Payable
        ledgerService.postSimpleJournalEntry(
                "SALE_CREDIT",
                orderId,
                "Sale credit for order " + orderId + " seller " + sellerId,
                sellerId, // system/actor
                ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.PAYMENT_CLEARING).getId(),
                ledgerService.getOrCreateSellerAccount(sellerId, com.kilivana.backend.finance.enums.LedgerAccountType.SELLER_PAYABLES, "Payable").getId(),
                netAmount,
                "Sale credit: " + description
        );

        // Commission ledger entry: Debit Payment Clearing, Credit Platform Commission Revenue
        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.postSimpleJournalEntry(
                    "COMMISSION",
                    orderId,
                    "Platform commission for order " + orderId + " seller " + sellerId,
                    sellerId,
                    ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.PAYMENT_CLEARING).getId(),
                    ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.PLATFORM_COMMISSION_REVENUE).getId(),
                    commissionAmount,
                    "Commission: " + description
            );
        }

        auditLogService.log(
                sellerId,
                "WALLET_CREDIT_SALE",
                "SellerWallet",
                wallet.getId(),
                String.format("order_id=%d, fulfilment_id=%d, gross=%s, commission=%s, net=%s",
                        orderId, sellerFulfilmentId, grossAmount, commissionAmount, netAmount)
        );

        log.info("Credited wallet {} for seller {}: net={}, pending now={}", wallet.getId(), sellerId, netAmount, wallet.getPendingBalance());
        return txn;
    }

    /**
     * Records commission deduction (separate transaction for audit trail).
     */
    @Transactional
    public WalletTransaction recordCommission(
            Long sellerId,
            Long orderId,
            Long sellerFulfilmentId,
            BigDecimal commissionAmount,
            String reference,
            String description
    ) {
        SellerWallet wallet = getOrCreateWallet(sellerId);

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .orderId(orderId)
                .sellerFulfilmentId(sellerFulfilmentId)
                .transactionType(WalletTransactionType.COMMISSION_DEDUCTION)
                .amount(commissionAmount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description(description)
                .balanceAfter(wallet.getTotalBalance())
                .pendingBalanceAfter(wallet.getPendingBalance())
                .availableBalanceAfter(wallet.getAvailableBalance())
                .processingBalanceAfter(wallet.getProcessingBalance())
                .build();
        transactionRepository.save(txn);

        auditLogService.log(
                sellerId,
                "WALLET_COMMISSION",
                "SellerWallet",
                wallet.getId(),
                String.format("order_id=%d, fulfilment_id=%d, commission=%s", orderId, sellerFulfilmentId, commissionAmount)
        );

        return txn;
    }

    /**
     * Moves earnings from pending to available after delivery confirmation.
     */
    @Transactional
    public WalletTransaction releasePendingToAvailable(Long sellerId, Long sellerFulfilmentId, BigDecimal amount, Long actorId) {
        SellerWallet wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        if (wallet.getPendingBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient pending balance to release: " + wallet.getPendingBalance() + " < " + amount);
        }

        String reference = "REL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .sellerFulfilmentId(sellerFulfilmentId)
                .transactionType(WalletTransactionType.PENDING_TO_AVAILABLE)
                .amount(amount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description("Earnings released after delivery confirmation for fulfilment " + sellerFulfilmentId)
                .balanceAfter(wallet.getTotalBalance())
                .pendingBalanceAfter(wallet.getPendingBalance().subtract(amount))
                .availableBalanceAfter(wallet.getAvailableBalance().add(amount))
                .processingBalanceAfter(wallet.getProcessingBalance())
                .build();
        transactionRepository.save(txn);

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);

        auditLogService.log(
                actorId,
                "WALLET_RELEASE_PENDING",
                "SellerWallet",
                wallet.getId(),
                String.format("fulfilment_id=%d, amount=%s, pending=%s, available=%s",
                        sellerFulfilmentId, amount, wallet.getPendingBalance(), wallet.getAvailableBalance())
        );

        log.info("Released pending to available for wallet {}: amount={}, pending={}, available={}",
                wallet.getId(), amount, wallet.getPendingBalance(), wallet.getAvailableBalance());
        return txn;
    }

    /**
     * Reserves available balance for a withdrawal request.
     */
    @Transactional
    public WalletTransaction reserveForWithdrawal(Long sellerId, BigDecimal amount, Long payoutId, Long actorId) {
        SellerWallet wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        if (!wallet.canWithdraw(amount)) {
            throw new BadRequestException("Insufficient available balance: " + wallet.getAvailableBalance() + " < " + amount);
        }

        String reference = "WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .transactionType(WalletTransactionType.WITHDRAWAL_REQUEST)
                .amount(amount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description("Withdrawal request " + payoutId + " - funds reserved")
                .balanceAfter(wallet.getTotalBalance())
                .pendingBalanceAfter(wallet.getPendingBalance())
                .availableBalanceAfter(wallet.getAvailableBalance().subtract(amount))
                .processingBalanceAfter(wallet.getProcessingBalance().add(amount))
                .build();
        transactionRepository.save(txn);

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setProcessingBalance(wallet.getProcessingBalance().add(amount));
        walletRepository.save(wallet);

        auditLogService.log(
                actorId,
                "WALLET_RESERVE_WITHDRAWAL",
                "SellerWallet",
                wallet.getId(),
                String.format("payout_id=%d, amount=%s, available=%s, processing=%s",
                        payoutId, amount, wallet.getAvailableBalance(), wallet.getProcessingBalance())
        );

        return txn;
    }

    /**
     * Confirms a successful withdrawal (funds leave processing balance).
     */
    @Transactional
    public WalletTransaction confirmWithdrawal(Long sellerId, BigDecimal amount, Long payoutId, String providerReference, Long actorId) {
        SellerWallet wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        if (wallet.getProcessingBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient processing balance: " + wallet.getProcessingBalance() + " < " + amount);
        }

        String reference = "WDC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .transactionType(WalletTransactionType.WITHDRAWAL_SUCCESS)
                .amount(amount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description("Withdrawal " + payoutId + " paid via " + providerReference)
                .balanceAfter(wallet.getTotalBalance().subtract(amount))
                .pendingBalanceAfter(wallet.getPendingBalance())
                .availableBalanceAfter(wallet.getAvailableBalance())
                .processingBalanceAfter(wallet.getProcessingBalance().subtract(amount))
                .build();
        transactionRepository.save(txn);

        wallet.setProcessingBalance(wallet.getProcessingBalance().subtract(amount));
        wallet.setTotalPaid(wallet.getTotalPaid().add(amount));
        walletRepository.save(wallet);

        // Ledger: Debit Seller Payable, Credit Settled Funds (cash out)
        ledgerService.postSimpleJournalEntry(
                "PAYOUT",
                payoutId,
                "Payout to seller " + sellerId + " via " + providerReference,
                actorId,
                ledgerService.getOrCreateSellerAccount(sellerId, com.kilivana.backend.finance.enums.LedgerAccountType.SELLER_PAYABLES, "Payable").getId(),
                ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.SETTLED_FUNDS).getId(),
                amount,
                "Payout: " + providerReference
        );

        auditLogService.log(
                actorId,
                "WALLET_CONFIRM_WITHDRAWAL",
                "SellerWallet",
                wallet.getId(),
                String.format("payout_id=%d, amount=%s, provider_ref=%s, total_paid=%s",
                        payoutId, amount, providerReference, wallet.getTotalPaid())
        );

        return txn;
    }

    /**
     * Reverses a failed/withdrawn reservation (returns funds from processing to available).
     */
    @Transactional
    public WalletTransaction reverseWithdrawalReservation(Long sellerId, BigDecimal amount, Long payoutId, String reason, Long actorId) {
        SellerWallet wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        if (wallet.getProcessingBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient processing balance to reverse: " + wallet.getProcessingBalance() + " < " + amount);
        }

        String reference = "WDRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .transactionType(WalletTransactionType.WITHDRAWAL_REVERSAL)
                .amount(amount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description("Withdrawal " + payoutId + " reversed: " + reason)
                .balanceAfter(wallet.getTotalBalance())
                .pendingBalanceAfter(wallet.getPendingBalance())
                .availableBalanceAfter(wallet.getAvailableBalance().add(amount))
                .processingBalanceAfter(wallet.getProcessingBalance().subtract(amount))
                .build();
        transactionRepository.save(txn);

        wallet.setProcessingBalance(wallet.getProcessingBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);

        auditLogService.log(
                actorId,
                "WALLET_REVERSE_WITHDRAWAL",
                "SellerWallet",
                wallet.getId(),
                String.format("payout_id=%d, amount=%s, reason=%s", payoutId, amount, reason)
        );

        return txn;
    }

    /**
     * Records a refund deduction from seller earnings.
     */
    @Transactional
    public WalletTransaction recordRefund(Long sellerId, Long orderId, Long sellerFulfilmentId, BigDecimal amount, String reference, String description, Long actorId) {
        SellerWallet wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        // Determine which balance to deduct from (prefer available, then pending)
        BigDecimal fromAvailable = wallet.getAvailableBalance().min(amount);
        BigDecimal remaining = amount.subtract(fromAvailable);
        BigDecimal fromPending = wallet.getPendingBalance().min(remaining);

        if (fromAvailable.add(fromPending).compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance for refund: available=" + wallet.getAvailableBalance() + ", pending=" + wallet.getPendingBalance() + ", needed=" + amount);
        }

        String txnReference = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .orderId(orderId)
                .sellerFulfilmentId(sellerFulfilmentId)
                .transactionType(WalletTransactionType.REFUND)
                .amount(amount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(txnReference)
                .description(description)
                .balanceAfter(wallet.getTotalBalance().subtract(amount))
                .pendingBalanceAfter(wallet.getPendingBalance().subtract(fromPending))
                .availableBalanceAfter(wallet.getAvailableBalance().subtract(fromAvailable))
                .processingBalanceAfter(wallet.getProcessingBalance())
                .build();
        transactionRepository.save(txn);

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(fromPending));
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(fromAvailable));
        walletRepository.save(wallet);

        // Ledger: Debit Seller Payable, Credit Refund Payable
        ledgerService.postSimpleJournalEntry(
                "REFUND",
                orderId,
                "Refund for order " + orderId + " seller " + sellerId + ": " + description,
                actorId,
                ledgerService.getOrCreateSellerAccount(sellerId, com.kilivana.backend.finance.enums.LedgerAccountType.SELLER_PAYABLES, "Payable").getId(),
                ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.REFUND_PAYABLE).getId(),
                amount,
                "Refund: " + description
        );

        auditLogService.log(
                actorId,
                "WALLET_REFUND",
                "SellerWallet",
                wallet.getId(),
                String.format("order_id=%d, fulfilment_id=%d, amount=%s, from_available=%s, from_pending=%s",
                        orderId, sellerFulfilmentId, amount, fromAvailable, fromPending)
        );

        return txn;
    }

    @Transactional(readOnly = true)
    public boolean hasReleaseTransaction(Long sellerFulfilmentId) {
        return transactionRepository.findByFulfilmentIdAndTypeAndStatus(
                sellerFulfilmentId, WalletTransactionType.PENDING_TO_AVAILABLE, WalletTransactionStatus.COMPLETED).size() > 0;
    }

    @Transactional(readOnly = true)
    public BigDecimal getPendingAmountForFulfilment(Long sellerFulfilmentId) {
        List<WalletTransaction> credits = transactionRepository.findByFulfilmentIdAndTypeAndStatus(
                sellerFulfilmentId, WalletTransactionType.SALE_CREDIT, WalletTransactionStatus.COMPLETED);
        List<WalletTransaction> releases = transactionRepository.findByFulfilmentIdAndTypeAndStatus(
                sellerFulfilmentId, WalletTransactionType.PENDING_TO_AVAILABLE, WalletTransactionStatus.COMPLETED);

        BigDecimal totalCredited = credits.stream()
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReleased = releases.stream()
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCredited.subtract(totalReleased);
    }

    @Transactional
    public WalletTransaction reverseSaleCredit(Long sellerId, Long sellerFulfilmentId, BigDecimal amount, Long actorId, String reason) {
        SellerWallet wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        if (wallet.getPendingBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient pending balance to reverse: " + wallet.getPendingBalance() + " < " + amount);
        }

        String reference = "REV-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .sellerId(sellerId)
                .sellerFulfilmentId(sellerFulfilmentId)
                .transactionType(WalletTransactionType.REFUND) // Use REFUND type for reversal
                .amount(amount)
                .status(WalletTransactionStatus.COMPLETED)
                .reference(reference)
                .description("Sale credit reversed for cancelled fulfilment " + sellerFulfilmentId + ": " + reason)
                .balanceAfter(wallet.getTotalBalance().subtract(amount))
                .pendingBalanceAfter(wallet.getPendingBalance().subtract(amount))
                .availableBalanceAfter(wallet.getAvailableBalance())
                .processingBalanceAfter(wallet.getProcessingBalance())
                .build();
        transactionRepository.save(txn);

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));
        wallet.setTotalEarned(wallet.getTotalEarned().subtract(amount));
        walletRepository.save(wallet);

        // Ledger reversal: Debit Seller Payable, Credit Payment Clearing
        ledgerService.postSimpleJournalEntry(
                "SALE_REVERSAL",
                sellerFulfilmentId,
                "Reversal of sale credit for cancelled fulfilment " + sellerFulfilmentId + ": " + reason,
                actorId,
                ledgerService.getOrCreateSellerAccount(sellerId, com.kilivana.backend.finance.enums.LedgerAccountType.SELLER_PAYABLES, "Payable").getId(),
                ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.PAYMENT_CLEARING).getId(),
                amount,
                "Sale reversal: " + reason
        );

        auditLogService.log(
                actorId,
                "WALLET_REVERSE_SALE",
                "SellerWallet",
                wallet.getId(),
                String.format("fulfilment_id=%d, amount=%s, reason=%s", sellerFulfilmentId, amount, reason)
        );

        return txn;
    }

    @Transactional(readOnly = true)
    public boolean hasSaleCredit(Long sellerFulfilmentId) {
        return transactionRepository.findByFulfilmentIdAndTypeAndStatus(
                sellerFulfilmentId, WalletTransactionType.SALE_CREDIT, WalletTransactionStatus.COMPLETED).size() > 0;
    }

    @Transactional(readOnly = true)
    public FinanceDtos.SellerWalletResponse getWalletBySellerId(Long sellerId) {
        SellerWallet wallet = walletRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));
        return FinanceDtos.SellerWalletResponse.fromEntity(wallet);
    }

    @Transactional(readOnly = true)
    public FinanceDtos.WalletSummaryResponse getWalletSummary(Long sellerId) {
        SellerWallet wallet = walletRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        long txnCount = transactionRepository.countByWalletId(wallet.getId());

        return FinanceDtos.WalletSummaryResponse.builder()
                .pendingBalance(wallet.getPendingBalance())
                .availableBalance(wallet.getAvailableBalance())
                .processingBalance(wallet.getProcessingBalance())
                .totalBalance(wallet.getTotalBalance())
                .totalPaid(wallet.getTotalPaid())
                .totalEarned(wallet.getTotalEarned())
                .status(wallet.getStatus())
                .transactionCount(txnCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.WalletTransactionResponse> getTransactions(Long sellerId) {
        SellerWallet wallet = walletRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));
        return transactionRepository.findByWalletId(wallet.getId()).stream()
                .map(FinanceDtos.WalletTransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinanceDtos.SellerWalletAdminResponse getWalletAdminView(Long sellerId) {
        SellerWallet wallet = walletRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));
        User seller = userRepository.findById(sellerId).orElse(null);

        return FinanceDtos.SellerWalletAdminResponse.builder()
                .id(wallet.getId())
                .uuid(wallet.getUuid())
                .sellerId(sellerId)
                .sellerName(seller != null ? seller.getName() : null)
                .sellerPhone(seller != null ? seller.getPhone() : null)
                .sellerType(wallet.getSellerType() != null ? wallet.getSellerType().name() : null)
                .pendingBalance(wallet.getPendingBalance())
                .availableBalance(wallet.getAvailableBalance())
                .processingBalance(wallet.getProcessingBalance())
                .totalPaid(wallet.getTotalPaid())
                .totalEarned(wallet.getTotalEarned())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerWalletAdminResponse> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(w -> {
                    User seller = userRepository.findById(w.getSellerId()).orElse(null);
                    return FinanceDtos.SellerWalletAdminResponse.builder()
                            .id(w.getId())
                            .uuid(w.getUuid())
                            .sellerId(w.getSellerId())
                            .sellerName(seller != null ? seller.getName() : null)
                            .sellerPhone(seller != null ? seller.getPhone() : null)
                            .sellerType(w.getSellerType() != null ? w.getSellerType().name() : null)
                            .pendingBalance(w.getPendingBalance())
                            .availableBalance(w.getAvailableBalance())
                            .processingBalance(w.getProcessingBalance())
                            .totalPaid(w.getTotalPaid())
                            .totalEarned(w.getTotalEarned())
                            .status(w.getStatus())
                            .createdAt(w.getCreatedAt())
                            .updatedAt(w.getUpdatedAt())
                            .build();
                })
                .toList();
    }
}