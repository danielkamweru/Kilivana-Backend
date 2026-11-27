package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.PayoutMethod;
import com.kilivana.backend.finance.entity.SellerPayoutAccount;
import com.kilivana.backend.finance.repository.SellerPayoutAccountRepository;
import com.kilivana.backend.finance.repository.SellerWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing seller payout accounts (M-Pesa, bank accounts).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerPayoutAccountService {

    private final SellerPayoutAccountRepository accountRepository;
    private final SellerWalletRepository walletRepository;
    private final UserRepository userRepository;
    private final FinancialAuditLogService auditLogService;

    /**
     * Adds a new payout account for a seller.
     */
    @Transactional
    public SellerPayoutAccount addAccount(Long sellerId, FinanceDtos.SellerPayoutAccountRequest request, Long actorId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", sellerId));

        if (seller.getRole() != UserRole.FARMER && seller.getRole() != UserRole.SUPPLIER) {
            throw new BadRequestException("User " + sellerId + " is not a farmer or supplier");
        }

        // Validate M-Pesa phone format
        if (request.getPayoutMethod() == PayoutMethod.MPESA) {
            if (request.getMpesaPhone() == null || request.getMpesaPhone().isBlank()) {
                throw new BadRequestException("M-Pesa phone number is required for MPESA payout method");
            }
            if (!request.getMpesaPhone().matches("^254(7\\d{8}|1\\d{8}|11\\d{7})$")) {
                throw new BadRequestException("Phone number must be a valid Safaricom number in 2547XXXXXXXX, 2541XXXXXXXX, or 25411XXXXXXX format");
            }
        }

        // If this is the first account or isDefault=true, make it default
        boolean isFirstAccount = accountRepository.findBySellerId(sellerId).isEmpty();
        boolean makeDefault = request.isDefault() || isFirstAccount;

        // If making default, unset any existing default
        if (makeDefault) {
            accountRepository.findBySellerIdAndIsDefaultTrue(sellerId)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        accountRepository.save(existing);
                    });
        }

        SellerPayoutAccount account = SellerPayoutAccount.builder()
                .sellerId(sellerId)
                .payoutMethod(request.getPayoutMethod())
                .accountName(request.getAccountName())
                .mpesaPhone(request.getMpesaPhone())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankBranchCode(request.getBankBranchCode())
                .isDefault(makeDefault)
                .verificationStatus("UNVERIFIED")
                .build();

        SellerPayoutAccount saved = accountRepository.save(account);

        auditLogService.log(
                actorId,
                "PAYOUT_ACCOUNT_ADDED",
                "SellerPayoutAccount",
                saved.getId(),
                String.format("seller_id=%d, method=%s, is_default=%s", sellerId, request.getPayoutMethod(), makeDefault)
        );

        log.info("Added payout account {} for seller {}: method={}, default={}", saved.getId(), sellerId, request.getPayoutMethod(), makeDefault);
        return saved;
    }

    /**
     * Updates an existing payout account.
     * Requires re-verification for sensitive changes.
     */
    @Transactional
    public SellerPayoutAccount updateAccount(Long accountId, FinanceDtos.SellerPayoutAccountRequest request, Long actorId) {
        SellerPayoutAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayoutAccount", accountId));

        // Verify ownership
        if (!account.getSellerId().equals(request.getSellerId())) {
            throw new BadRequestException("Account does not belong to this seller");
        }

        // If changing M-Pesa phone, require re-verification
        if (request.getPayoutMethod() == PayoutMethod.MPESA && request.getMpesaPhone() != null
                && !request.getMpesaPhone().equals(account.getMpesaPhone())) {
            if (!request.getMpesaPhone().matches("^254(7\\d{8}|1\\d{8}|11\\d{7})$")) {
                throw new BadRequestException("Phone number must be a valid Safaricom number in 2547XXXXXXXX, 2541XXXXXXXX, or 25411XXXXXXX format");
            }
            account.setMpesaPhone(request.getMpesaPhone());
            account.setVerificationStatus("UNVERIFIED");
            account.setVerifiedAt(null);
            account.setVerificationReference(null);
        }

        // If changing bank details, require re-verification
        if (request.getPayoutMethod() == PayoutMethod.BANK_TRANSFER) {
            boolean bankDetailsChanged = (request.getBankAccountNumber() != null && !request.getBankAccountNumber().equals(account.getBankAccountNumber()))
                    || (request.getBankName() != null && !request.getBankName().equals(account.getBankName()));
            if (bankDetailsChanged) {
                account.setVerificationStatus("UNVERIFIED");
                account.setVerifiedAt(null);
                account.setVerificationReference(null);
            }
            if (request.getBankName() != null) account.setBankName(request.getBankName());
            if (request.getBankAccountNumber() != null) account.setBankAccountNumber(request.getBankAccountNumber());
            if (request.getBankBranchCode() != null) account.setBankBranchCode(request.getBankBranchCode());
        }

        if (request.getAccountName() != null) account.setAccountName(request.getAccountName());

        // Handle default flag
        if (request.isDefault() && !account.isDefault()) {
            accountRepository.findBySellerIdAndIsDefaultTrue(account.getSellerId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(accountId)) {
                            existing.setDefault(false);
                            accountRepository.save(existing);
                        }
                    });
            account.setDefault(true);
        }

        SellerPayoutAccount saved = accountRepository.save(account);

        auditLogService.log(
                actorId,
                "PAYOUT_ACCOUNT_UPDATED",
                "SellerPayoutAccount",
                accountId,
                String.format("seller_id=%d, verification_status=%s", account.getSellerId(), account.getVerificationStatus())
        );

        return saved;
    }

    /**
     * Marks a payout account as verified (e.g., after M-Pesa B2C validation or bank verification).
     */
    @Transactional
    public SellerPayoutAccount verifyAccount(Long accountId, String verificationReference, Long actorId) {
        SellerPayoutAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayoutAccount", accountId));

        account.setVerificationStatus("VERIFIED");
        account.setVerifiedAt(java.time.LocalDateTime.now());
        account.setVerificationReference(verificationReference);

        SellerPayoutAccount saved = accountRepository.save(account);

        auditLogService.log(
                actorId,
                "PAYOUT_ACCOUNT_VERIFIED",
                "SellerPayoutAccount",
                accountId,
                String.format("seller_id=%d, reference=%s", account.getSellerId(), verificationReference)
        );

        return saved;
    }

    /**
     * Gets a seller's payout accounts.
     */
    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutAccountResponse> getAccounts(Long sellerId) {
        return accountRepository.findBySellerId(sellerId).stream()
                .map(FinanceDtos.SellerPayoutAccountResponse::fromEntity)
                .toList();
    }

    /**
     * Gets a seller's default payout account.
     */
    @Transactional(readOnly = true)
    public FinanceDtos.SellerPayoutAccountResponse getDefaultAccount(Long sellerId) {
        SellerPayoutAccount account = accountRepository.findBySellerIdAndIsDefaultTrue(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("No default payout account for seller", sellerId));
        return FinanceDtos.SellerPayoutAccountResponse.fromEntity(account);
    }

    /**
     * Gets a specific payout account (with ownership check).
     */
    @Transactional(readOnly = true)
    public FinanceDtos.SellerPayoutAccountResponse getAccount(Long accountId, Long sellerId) {
        SellerPayoutAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerPayoutAccount", accountId));

        if (!account.getSellerId().equals(sellerId)) {
            throw new ResourceNotFoundException("SellerPayoutAccount", accountId);
        }

        return FinanceDtos.SellerPayoutAccountResponse.fromEntity(account);
    }

    /**
     * Admin: gets all payout accounts with seller info.
     */
    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutAccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(FinanceDtos.SellerPayoutAccountResponse::fromEntity)
                .toList();
    }

    /**
     * Admin: gets payout accounts for a specific seller.
     */
    @Transactional(readOnly = true)
    public List<FinanceDtos.SellerPayoutAccountResponse> getAccountsForSeller(Long sellerId) {
        return accountRepository.findBySellerId(sellerId).stream()
                .map(FinanceDtos.SellerPayoutAccountResponse::fromEntity)
                .toList();
    }
}