package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.PayoutStatus;
import com.kilivana.backend.finance.entity.SellerPayout;
import com.kilivana.backend.finance.entity.SellerPayoutAccount;
import com.kilivana.backend.finance.repository.SellerPayoutRepository;
import com.kilivana.backend.finance.repository.SellerPayoutAccountRepository;
import com.kilivana.backend.finance.repository.SellerWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing seller withdrawal/payout requests.
 * Handles the full lifecycle: request -> admin approval -> B2C processing -> completion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerPayoutService {

    private final SellerPayoutRepository payoutRepository;
    private final SellerPayoutAccountRepository accountRepository;
    private final SellerWalletRepository walletRepository;
    private final UserRepository userRepository;
    private final SellerWalletService walletService;
    private final MpesaB2CService b2cService;
    private final FinancialAuditLogService auditLogService;

    private static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("70000.00"); // M-Pesa B2C limit

    /**
     * Creates a new withdrawal request from a seller.
     */
    @Transactional
    public SellerPayout requestWithdrawal(Long sellerId, FinanceDtos.SellerPayoutRequest request, Long actorId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", sellerId));

        if (seller.getRole() != UserRole.FARMER && seller.getRole() != UserRole.SUPPLIER) {
            throw new BadRequestException("Only farmers and suppliers can request withdrawals");
        }

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Withdrawal amount must be positive");
        }
        if (request.getAmount().compareTo(MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new BadRequestException("Minimum withdrawal amount is " + MIN_WITHDRAWAL_AMOUNT);
        }
        if (request.getAmount().compareTo(MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new BadRequestException("Maximum withdrawal amount is " + MAX_WITHDRAWAL_AMOUNT + " (M-Pesa B2C limit)");
        }

        // Get seller's wallet
        var wallet = walletRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerWallet for seller", sellerId));

        if (!wallet.canWithdraw(request.getAmount())) {
            throw new BadRequestException("Insufficient available balance: " + wallet.getAvailableBalance() + " < " + request.getAmount());
        }

        // Get payout account
        SellerPayoutAccount account;
        if (request.getPayoutAccountId() != null) {
            account = accountRepository.findById(request.getPayoutAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("SellerPayoutAccount", request.getPayoutAccountId()));
            if (!account.getSellerId().equals(sellerId)) {
                throw new BadRequestException("Payout account does not belong to this seller");
            }
        } else {
            // Use default account
            account = accountRepository.findBySellerIdAndIsDefaultTrue(sellerId)
                    .orElseThrow(() -> new BadRequestException("No default payout account set. Please add a payout account first."));
        }

        if (!"VERIFIED".equals(account.getVerificationStatus())) {
            throw new BadRequestException("Payout account is not verified. Please verify your account before withdrawing.");
        }

        // Check for duplicate pending request
        List<SellerPayout> pending = payoutRepository.findBySellerId(sellerId).stream()
                .filter(p -> p.getStatus() == PayoutStatus.PENDING || p.getStatus() == PayoutStatus.APPROVED || p.getStatus() == PayoutStatus.PROCESSING)
                .toList();
        if (!pending.isEmpty()) {
            throw new BadRequestException("You already have a pending withdrawal request. Please wait for it to complete.");
        }

        // Reserve funds in wallet
        walletService.reserveForWithdrawal(sellerId, request.getAmount(), null, actorId);

        // Create payout request
        String reference = "PO-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String idempotencyKey = "IDEMP-" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();

        String destinationSnapshot = buildDestinationSnapshot(account);

        SellerPayout payout = SellerPayout.builder()
                .sellerId(sellerId)
                .walletId(wallet.getId())
                .payoutAccountId(account.getId())
                .amount(request.getAmount())
                .payoutMethod(account.getPayoutMethod())
                .destinationSnapshot(destinationSnapshot)
                .status(PayoutStatus.PENDING)
                .reference(reference)
                .idempotencyKey(idempotencyKey)
                .attemptCount(0)
                .build();

        SellerPayout saved = payoutRepository.save(payout);

        // Update the wallet transaction with the payout ID
        // (The wallet transaction was created with null payoutId, we could update it but it's not critical)

        auditLogService.log(
                actorId,
                "PAYOUT_REQUESTED",
                "SellerPayout",
                saved.getId(),
                String.format("seller_id=%d, amount=%s, account_id=%d, reference=%s",
                        sellerId, request.getAmount(), account.getId(), reference)
        );

        log.info("Withdrawal request {} created for seller {}: amount={}, account={}",
                saved.getId(), sellerId, request.getAmount(), account.getId());
        return saved;
    }

    /**
     * Admin approves or rejects a withdrawal request.
     */
    @Transactional
    public SellerPayout processAdminDecision(Long payoutId, FinanceDtos.AdminPayoutApprovalRequest request, Long adminId) {
        SellerPayout payout = payoutRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayout", payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new BadRequestException("Payout " + payoutId + " is not in PENDING status: " + payout.getStatus());
        }

        if (request.isApprove()) {
            payout.setStatus(PayoutStatus.APPROVED);
            payout.setApprovedBy(adminId);
            payout.setApprovedAt(LocalDateTime.now());

            auditLogService.log(
                    adminId,
                    "PAYOUT_APPROVED",
                    "SellerPayout",
                    payoutId,
                    String.format("seller_id=%d, amount=%s", payout.getSellerId(), payout.getAmount())
            );
        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Rejection reason is required");
            }

            payout.setStatus(PayoutStatus.REJECTED);
            payout.setApprovedBy(adminId);
            payout.setApprovedAt(LocalDateTime.now());
            payout.setRejectionReason(request.getRejectionReason());

            // Release reserved funds back to available
            walletService.reverseWithdrawalReservation(
                    payout.getSellerId(),
                    payout.getAmount(),
                    payoutId,
                    request.getRejectionReason(),
                    adminId
            );

            auditLogService.log(
                    adminId,
                    "PAYOUT_REJECTED",
                    "SellerPayout",
                    payoutId,
                    String.format("seller_id=%d, amount=%s, reason=%s",
                            payout.getSellerId(), payout.getAmount(), request.getRejectionReason())
            );
        }

        SellerPayout saved = payoutRepository.save(payout);
        log.info("Payout {} {} by admin {}", payoutId, request.isApprove() ? "approved" : "rejected", adminId);
        return saved;
    }

    /**
     * Initiates the B2C payout for an approved withdrawal.
     * Called by admin or scheduled job.
     */
    @Transactional
    public SellerPayout initiatePayout(Long payoutId, Long actorId) {
        SellerPayout payout = payoutRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayout", payoutId));

        if (payout.getStatus() != PayoutStatus.APPROVED && payout.getStatus() != PayoutStatus.FAILED) {
            throw new BadRequestException("Payout " + payoutId + " cannot be initiated in status: " + payout.getStatus());
        }

        if (payout.getAttemptCount() >= 3) {
            throw new BadRequestException("Maximum payout attempts (3) exceeded for payout " + payoutId);
        }

        // Get payout account details
        SellerPayoutAccount account = accountRepository.findById(payout.getPayoutAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayoutAccount", payout.getPayoutAccountId()));

        // Initiate B2C payment
        payout.setStatus(PayoutStatus.PROCESSING);
        payout.setAttemptCount(payout.getAttemptCount() + 1);
        payout.setLastAttemptAt(LocalDateTime.now());
        payoutRepository.save(payout);

        try {
            String providerRef = b2cService.initiateB2CPayment(
                    account.getMpesaPhone(),
                    payout.getAmount(),
                    payout.getReference(),
                    payout.getIdempotencyKey()
            );

            payout.setProviderReference(providerRef);
            payout.setStatus(PayoutStatus.PROCESSING); // Will be updated to PAID/FAILED by callback
            payoutRepository.save(payout);

            auditLogService.log(
                    actorId,
                    "PAYOUT_INITIATED",
                    "SellerPayout",
                    payoutId,
                    String.format("seller_id=%d, amount=%s, provider_ref=%s, attempt=%d",
                            payout.getSellerId(), payout.getAmount(), providerRef, payout.getAttemptCount())
            );

            log.info("B2C payout initiated for {}: provider_ref={}", payoutId, providerRef);
            return payoutRepository.save(payout);

        } catch (Exception e) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setProviderResultDesc(e.getMessage());
            payoutRepository.save(payout);

            // Release reservation on failure
            walletService.reverseWithdrawalReservation(
                    payout.getSellerId(),
                    payout.getAmount(),
                    payoutId,
                    "B2C initiation failed: " + e.getMessage(),
                    actorId
            );

            auditLogService.log(
                    actorId,
                    "PAYOUT_INITIATION_FAILED",
                    "SellerPayout",
                    payoutId,
                    String.format("seller_id=%d, amount=%s, error=%s",
                            payout.getSellerId(), payout.getAmount(), e.getMessage())
            );

            throw new RuntimeException("B2C payout initiation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Processes B2C callback (success/failure).
     */
    @Transactional
    public void processB2CCallback(String providerReference, int resultCode, String resultDesc,
                                   String mpesaReceiptNumber, Long actorId) {
        SellerPayout payout = payoutRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayout by provider reference", providerReference));

        payout.setProviderReference(providerReference);
        payout.setProviderResultCode(resultCode);
        payout.setProviderResultDesc(resultDesc);
        payout.setMpesaReceiptNumber(mpesaReceiptNumber);

        if (resultCode == 0) {
            // Success
            payout.setStatus(PayoutStatus.PAID);
            payout.setPaidAt(LocalDateTime.now());

            walletService.confirmWithdrawal(
                    payout.getSellerId(),
                    payout.getAmount(),
                    payout.getId(),
                    mpesaReceiptNumber,
                    actorId
            );

            auditLogService.log(
                    actorId,
                    "PAYOUT_COMPLETED",
                    "SellerPayout",
                    payout.getId(),
                    String.format("seller_id=%d, amount=%s, receipt=%s",
                            payout.getSellerId(), payout.getAmount(), mpesaReceiptNumber)
            );

            log.info("B2C payout {} completed: receipt={}", payout.getId(), mpesaReceiptNumber);
        } else {
            // Failure
            payout.setStatus(PayoutStatus.FAILED);

            // Release reservation
            walletService.reverseWithdrawalReservation(
                    payout.getSellerId(),
                    payout.getAmount(),
                    payout.getId(),
                    "B2C failed: " + resultDesc,
                    actorId
            );

            auditLogService.log(
                    actorId,
                    "PAYOUT_FAILED",
                    "SellerPayout",
                    payout.getId(),
                    String.format("seller_id=%d, amount=%s, result_code=%d, result_desc=%s",
                            payout.getSellerId(), payout.getAmount(), resultCode, resultDesc)
            );

            log.warn("B2C payout {} failed: code={}, desc={}", payout.getId(), resultCode, resultDesc);
        }

        payoutRepository.save(payout);
    }

    @Transactional(readOnly = true)
    public FinanceDtos.SellerPayoutResponse getPayout(Long payoutId, Long sellerId) {
        SellerPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayout", payoutId));

        if (!payout.getSellerId().equals(sellerId)) {
            throw new ResourceNotFoundException("SellerPayout", payoutId);
        }

        return FinanceDtos.SellerPayoutResponse.fromEntity(payout);
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutResponse> getPayoutsForSeller(Long sellerId) {
        return payoutRepository.findBySellerId(sellerId).stream()
                .map(FinanceDtos.SellerPayoutResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutResponse> getAllPayouts() {
        return payoutRepository.findAll().stream()
                .map(FinanceDtos.SellerPayoutResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutResponse> getPayoutsByStatus(PayoutStatus status) {
        return payoutRepository.findByStatus(status).stream()
                .map(FinanceDtos.SellerPayoutResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutResponse> getPendingPayouts() {
        return payoutRepository.findByStatusInOrderByCreatedAtAsc(List.of(
                PayoutStatus.PENDING, PayoutStatus.APPROVED, PayoutStatus.PROCESSING
        )).stream()
                .map(FinanceDtos.SellerPayoutResponse::fromEntity)
                .toList();
    }

    private String buildDestinationSnapshot(SellerPayoutAccount account) {
        if (account.getPayoutMethod() == com.kilivana.backend.finance.enums.PayoutMethod.MPESA) {
            return "M-Pesa to " + account.getMaskedMpesaPhone() + " (" + account.getAccountName() + ")";
        } else {
            return "Bank: " + account.getBankName() + " " + account.getMaskedBankAccount() + " (" + account.getAccountName() + ")";
        }
    }
}