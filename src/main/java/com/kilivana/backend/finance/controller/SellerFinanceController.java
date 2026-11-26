package com.kilivana.backend.finance.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.PayoutMethod;
import com.kilivana.backend.finance.service.SellerPayoutAccountService;
import com.kilivana.backend.finance.service.SellerPayoutService;
import com.kilivana.backend.finance.service.SellerWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seller (farmer/supplier) finance endpoints.
 * Provides wallet, transactions, payout accounts, and withdrawal functionality.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class SellerFinanceController {

    private final SellerWalletService walletService;
    private final SellerPayoutAccountService payoutAccountService;
    private final SellerPayoutService payoutService;

    // ==================== WALLET ====================

    @GetMapping("/me")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerWalletResponse>> getMyWallet(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletBySellerId(userId)));
    }

    @GetMapping("/me/summary")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.WalletSummaryResponse>> getWalletSummary(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletSummary(userId)));
    }

    @GetMapping("/me/transactions")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<List<FinanceDtos.WalletTransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal Long userId,
            Pageable pageable) {
        List<FinanceDtos.WalletTransactionResponse> transactions = walletService.getTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/me/earnings")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyEarnings(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String period) {
        FinanceDtos.SellerWalletResponse wallet = walletService.getWalletBySellerId(userId);

        // Calculate earnings for different periods
        Map<String, Object> earnings = Map.of(
                "pending", wallet.getPendingBalance(),
                "available", wallet.getAvailableBalance(),
                "processing", wallet.getProcessingBalance(),
                "total_earned", wallet.getTotalEarned(),
                "total_paid", wallet.getTotalPaid(),
                "period", period != null ? period : "all_time"
        );

        return ResponseEntity.ok(ApiResponse.success(earnings));
    }

    // ==================== PAYOUT ACCOUNTS ====================

    @GetMapping("/me/payout-accounts")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerPayoutAccountResponse>>> getMyPayoutAccounts(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(payoutAccountService.getAccounts(userId)));
    }

    @GetMapping("/me/payout-accounts/default")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutAccountResponse>> getDefaultPayoutAccount(
            @AuthenticationPrincipal Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(payoutAccountService.getDefaultAccount(userId)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/me/payout-accounts/{accountId}")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutAccountResponse>> getPayoutAccount(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(payoutAccountService.getAccount(accountId, userId)));
    }

    @PostMapping("/me/payout-accounts")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutAccountResponse>> addPayoutAccount(
            @AuthenticationPrincipal Long userId,
            @RequestBody FinanceDtos.SellerPayoutAccountRequest request) {
        FinanceDtos.SellerPayoutAccountResponse account = FinanceDtos.SellerPayoutAccountResponse.fromEntity(
                payoutAccountService.addAccount(userId, request, userId));
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PutMapping("/me/payout-accounts/{accountId}")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutAccountResponse>> updatePayoutAccount(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId,
            @RequestBody FinanceDtos.SellerPayoutAccountRequest request) {
        // Ensure the request has the correct seller ID
        request = FinanceDtos.SellerPayoutAccountRequest.builder()
                .payoutMethod(request.getPayoutMethod())
                .accountName(request.getAccountName())
                .mpesaPhone(request.getMpesaPhone())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankBranchCode(request.getBankBranchCode())
                .isDefault(request.isDefault())
                .build();

        FinanceDtos.SellerPayoutAccountResponse account = FinanceDtos.SellerPayoutAccountResponse.fromEntity(
                payoutAccountService.updateAccount(accountId, request, userId));
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    // ==================== WITHDRAWALS ====================

    @GetMapping("/me/withdrawals")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerPayoutResponse>>> getMyWithdrawals(
            @AuthenticationPrincipal Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(payoutService.getPayoutsForSeller(userId)));
    }

    @GetMapping("/me/withdrawals/{payoutId}")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutResponse>> getWithdrawal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long payoutId) {
        return ResponseEntity.ok(ApiResponse.success(payoutService.getPayout(payoutId, userId)));
    }

    @PostMapping("/me/withdrawals")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutResponse>> requestWithdrawal(
            @AuthenticationPrincipal Long userId,
            @RequestBody FinanceDtos.SellerPayoutRequest request) {
        FinanceDtos.SellerPayoutResponse payout = FinanceDtos.SellerPayoutResponse.fromEntity(
                payoutService.requestWithdrawal(userId, request, userId));
        return ResponseEntity.ok(ApiResponse.success(payout));
    }

    @GetMapping("/me/withdrawals/eligible")
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWithdrawalEligibility(
            @AuthenticationPrincipal Long userId) {
        FinanceDtos.SellerWalletResponse wallet = walletService.getWalletBySellerId(userId);
        boolean hasVerifiedAccount = false;
        try {
            payoutAccountService.getDefaultAccount(userId);
            hasVerifiedAccount = true;
        } catch (Exception e) {
            // No default account
        }

        Map<String, Object> eligibility = Map.of(
                "available_balance", wallet.getAvailableBalance(),
                "min_withdrawal", 100.00,
                "max_withdrawal", 70000.00,
                "has_verified_account", hasVerifiedAccount,
                "can_withdraw", wallet.getAvailableBalance().compareTo(new BigDecimal("100")) >= 0 && hasVerifiedAccount
        );

        return ResponseEntity.ok(ApiResponse.success(eligibility));
    }
}