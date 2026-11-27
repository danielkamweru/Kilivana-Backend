package com.kilivana.backend.finance.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.LedgerAccountType;
import com.kilivana.backend.finance.enums.PayoutStatus;
import com.kilivana.backend.finance.enums.SettlementStatus;
import com.kilivana.backend.finance.entity.Settlement;
import com.kilivana.backend.finance.service.FinancialAuditLogService;
import com.kilivana.backend.finance.service.FinancialLedgerService;
import com.kilivana.backend.finance.service.RefundService;
import com.kilivana.backend.finance.service.SellerPayoutAccountService;
import com.kilivana.backend.finance.service.SellerPayoutService;
import com.kilivana.backend.finance.service.SellerWalletService;
import com.kilivana.backend.finance.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin finance management endpoints.
 * Provides dashboard, transaction management, payout management, ledger, and reconciliation.
 */
@RestController
@RequestMapping("/api/v1/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceController {

    private final SellerWalletService walletService;
    private final SellerPayoutService payoutService;
    private final SellerPayoutAccountService payoutAccountService;
    private final RefundService refundService;
    private final FinancialLedgerService ledgerService;
    private final FinancialAuditLogService auditLogService;
    private final SettlementService settlementService;

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.AdminFinanceDashboardResponse>> getDashboard() {
        // Platform revenue = commission revenue
        BigDecimal platformRevenue = ledgerService.getPlatformCommissionRevenue(
                LocalDateTime.now().minusMonths(1), LocalDateTime.now());

        // Gross order volume = total successful payments in period
        // Seller payables = sum of all seller payable balances
        BigDecimal sellerPayables = BigDecimal.ZERO; // Would query ledger
        BigDecimal pendingPayouts = BigDecimal.ZERO;
        BigDecimal settledFunds = BigDecimal.ZERO;
        BigDecimal openRefunds = BigDecimal.ZERO;
        BigDecimal totalCollections = BigDecimal.ZERO;
        BigDecimal totalPayouts = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;

        // Get from payout service
        List<FinanceDtos.SellerPayoutResponse> pendingPayoutList = payoutService.getPendingPayouts();
        pendingPayouts = pendingPayoutList.stream()
                .map(FinanceDtos.SellerPayoutResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinanceDtos.AdminFinanceDashboardResponse response = FinanceDtos.AdminFinanceDashboardResponse.builder()
                .platformRevenue(platformRevenue)
                .grossOrderVolume(totalCollections)
                .sellerPayables(sellerPayables)
                .pendingPayouts(pendingPayouts)
                .settledFunds(settledFunds)
                .openRefunds(openRefunds)
                .totalCollections(totalCollections)
                .totalPayouts(totalPayouts)
                .totalRefunds(totalRefunds)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== SETTLEMENTS ====================

    @GetMapping("/settlements")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SettlementResponse>>> getAllSettlements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long transportProviderId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        List<Settlement> settlements;
        if (status != null) {
            settlements = settlementService.getSettlementsByStatus(SettlementStatus.from(status));
        } else if (sellerId != null) {
            settlements = settlementService.getSettlementsBySellerId(sellerId);
        } else if (transportProviderId != null) {
            settlements = settlementService.getSettlementsByTransportProviderId(transportProviderId);
        } else if (from != null && to != null) {
            settlements = settlementService.getSettlementsByDateRange(from, to);
        } else {
            settlements = settlementService.getAllSettlements();
        }

        // Apply pagination manually (or use repository with Pageable)
        List<FinanceDtos.SettlementResponse> response = settlements.stream()
                .map(FinanceDtos.SettlementResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/settlements/{settlementId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SettlementResponse>> getSettlement(@PathVariable Long settlementId) {
        Settlement settlement = settlementService.getSettlement(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", settlementId));
        return ResponseEntity.ok(ApiResponse.success(FinanceDtos.SettlementResponse.fromEntity(settlement)));
    }

    @GetMapping("/settlements/shipment/{shipmentId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SettlementResponse>> getSettlementByShipment(@PathVariable Long shipmentId) {
        Settlement settlement = settlementService.getSettlementByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement for shipment", shipmentId));
        return ResponseEntity.ok(ApiResponse.success(FinanceDtos.SettlementResponse.fromEntity(settlement)));
    }

    @PostMapping("/settlements/{settlementId}/process")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SettlementResponse>> processSettlement(@PathVariable Long settlementId) {
        Settlement settlement = settlementService.processSettlement(settlementId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(FinanceDtos.SettlementResponse.fromEntity(settlement)));
    }

    // ==================== SELLER WALLETS ====================

    @GetMapping("/wallets")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerWalletAdminResponse>>> getAllWallets() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getAllWallets()));
    }

    @GetMapping("/wallets/{sellerId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerWalletAdminResponse>> getWallet(@PathVariable Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletAdminView(sellerId)));
    }

    @GetMapping("/wallets/{sellerId}/transactions")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.WalletTransactionResponse>>> getWalletTransactions(
            @PathVariable Long sellerId,
            Pageable pageable) {
        List<FinanceDtos.WalletTransactionResponse> transactions = walletService.getTransactions(sellerId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    // ==================== PAYOUTS ====================

    @GetMapping("/payouts")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerPayoutResponse>>> getAllPayouts(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        List<FinanceDtos.SellerPayoutResponse> payouts;
        if (status != null) {
            payouts = payoutService.getPayoutsByStatus(PayoutStatus.from(status));
        } else {
            payouts = payoutService.getAllPayouts();
        }
        return ResponseEntity.ok(ApiResponse.success(payouts));
    }

    @GetMapping("/payouts/pending")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerPayoutResponse>>> getPendingPayouts() {
        return ResponseEntity.ok(ApiResponse.success(payoutService.getPendingPayouts()));
    }

    @GetMapping("/payouts/{payoutId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutResponse>> getPayout(@PathVariable Long payoutId) {
        // Admin can view any payout
        FinanceDtos.SellerPayoutResponse payout = payoutService.getAllPayouts().stream()
                .filter(p -> p.getId().equals(payoutId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        return ResponseEntity.ok(ApiResponse.success(payout));
    }

    @PostMapping("/payouts/{payoutId}/approve")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutResponse>> approvePayout(
            @PathVariable Long payoutId,
            @RequestBody FinanceDtos.AdminPayoutApprovalRequest request) {
        // Get current admin ID from security context
        Long adminId = getCurrentUserId();
        FinanceDtos.SellerPayoutResponse payout = FinanceDtos.SellerPayoutResponse.fromEntity(
                payoutService.processAdminDecision(payoutId, request, adminId));
        return ResponseEntity.ok(ApiResponse.success(payout));
    }

    @PostMapping("/payouts/{payoutId}/initiate")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.SellerPayoutResponse>> initiatePayout(@PathVariable Long payoutId) {
        Long adminId = getCurrentUserId();
        FinanceDtos.SellerPayoutResponse payout = FinanceDtos.SellerPayoutResponse.fromEntity(
                payoutService.initiatePayout(payoutId, adminId));
        return ResponseEntity.ok(ApiResponse.success(payout));
    }

    // ==================== REFUNDS ====================

    @GetMapping("/refunds")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.RefundTransactionResponse>>> getAllRefunds(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        List<FinanceDtos.RefundTransactionResponse> refunds;
        if (status != null) {
            refunds = refundService.getRefundsByStatus(com.kilivana.backend.common.enums.PaymentStatus.from(status));
        } else {
            refunds = refundService.getAllRefunds();
        }
        return ResponseEntity.ok(ApiResponse.success(refunds));
    }

    @GetMapping("/refunds/{refundId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.RefundTransactionResponse>> getRefund(@PathVariable Long refundId) {
        return ResponseEntity.ok(ApiResponse.success(refundService.getRefund(refundId)));
    }

    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.RefundTransactionResponse>> approveRefund(@PathVariable Long refundId) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                FinanceDtos.RefundTransactionResponse.fromEntity(refundService.approveRefund(refundId, adminId))));
    }

    @PostMapping("/refunds/{refundId}/reject")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.RefundTransactionResponse>> rejectRefund(
            @PathVariable Long refundId,
            @RequestParam String reason) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                FinanceDtos.RefundTransactionResponse.fromEntity(refundService.rejectRefund(refundId, reason, adminId))));
    }

    // ==================== FINANCIAL LEDGER ====================

    @GetMapping("/ledger/accounts")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.LedgerAccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getAllAccounts()));
    }

    @GetMapping("/ledger/accounts/{accountId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.LedgerAccountResponse>> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getAccount(accountId)));
    }

    @GetMapping("/ledger/entries")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<Page<FinanceDtos.JournalEntryResponse>>> getJournalEntries(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        // Would need additional repository methods for filtering
        return ResponseEntity.ok(ApiResponse.success(Page.empty()));
    }

    @GetMapping("/ledger/entries/{entryId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<FinanceDtos.JournalEntryResponse>> getJournalEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getJournalEntry(entryId)));
    }

    @GetMapping("/ledger/seller-payable/{sellerId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<BigDecimal>> getSellerPayableBalance(@PathVariable Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getSellerPayableBalance(sellerId)));
    }

    // ==================== RECONCILIATION ====================

    @GetMapping("/reconciliation/summary")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReconciliationSummary(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        // Get actual data from services
        List<FinanceDtos.SellerPayoutResponse> allPayouts = payoutService.getAllPayouts();
        List<FinanceDtos.RefundTransactionResponse> allRefunds = refundService.getAllRefunds();
        
        BigDecimal totalPayouts = allPayouts.stream()
                .filter(p -> p.getStatus() == PayoutStatus.PAID)
                .map(FinanceDtos.SellerPayoutResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRefunds = allRefunds.stream()
                .filter(r -> "REFUNDED".equals(r.getStatus()))
                .map(FinanceDtos.RefundTransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformRevenue = ledgerService.getPlatformCommissionRevenue(from, to);

        Map<String, Object> summary = Map.of(
                "period", Map.of("from", from, "to", to),
                "totalCollections", platformRevenue.add(totalPayouts).add(totalRefunds),
                "totalPayouts", totalPayouts,
                "totalRefunds", totalRefunds,
                "pendingPayouts", allPayouts.stream()
                        .filter(p -> p.getStatus() == PayoutStatus.PENDING || p.getStatus() == PayoutStatus.APPROVED || p.getStatus() == PayoutStatus.PROCESSING)
                        .map(FinanceDtos.SellerPayoutResponse::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                "platformRevenue", platformRevenue,
                "discrepancies", 0
        );
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/reconciliation/run")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runReconciliation(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        // Would implement reconciliation job - for now return summary
        Map<String, Object> result = Map.of(
                "status", "COMPLETED",
                "matched", 0,
                "unmatched", 0,
                "discrepancies", List.of()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit-logs")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.FinancialAuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        List<FinanceDtos.FinancialAuditLogResponse> logs;
        if (actorId != null) {
            logs = auditLogService.getByActorId(actorId);
        } else if (action != null) {
            logs = auditLogService.getByAction(action);
        } else if (entityType != null && entityId != null) {
            logs = auditLogService.getByEntity(entityType, entityId);
        } else if (from != null && to != null) {
            logs = auditLogService.getByDateRange(from, to);
        } else {
            logs = List.of();
        }
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ==================== PAYOUT ACCOUNTS ====================

    @GetMapping("/payout-accounts")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerPayoutAccountResponse>>> getAllPayoutAccounts() {
        return ResponseEntity.ok(ApiResponse.success(payoutAccountService.getAllAccounts()));
    }

    @GetMapping("/payout-accounts/seller/{sellerId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<FinanceDtos.SellerPayoutAccountResponse>>> getSellerPayoutAccounts(@PathVariable Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(payoutAccountService.getAccountsForSeller(sellerId)));
    }

    // Helper to get current user ID from security context
    private Long getCurrentUserId() {
        // In real implementation, extract from SecurityContextHolder
        // For now, return a placeholder - actual implementation would use @AuthenticationPrincipal
        return 1L; // System/admin user
    }
}