package com.kilivana.backend.finance.dto;

import com.kilivana.backend.finance.enums.LedgerAccountType;
import com.kilivana.backend.finance.enums.PayoutMethod;
import com.kilivana.backend.finance.enums.PayoutStatus;
import com.kilivana.backend.finance.enums.WalletTransactionStatus;
import com.kilivana.backend.finance.enums.WalletTransactionType;
import com.kilivana.backend.finance.entity.FinancialAuditLog;
import com.kilivana.backend.finance.entity.JournalEntry;
import com.kilivana.backend.finance.entity.JournalLine;
import com.kilivana.backend.finance.entity.LedgerAccount;
import com.kilivana.backend.finance.entity.RefundTransaction;
import com.kilivana.backend.finance.entity.SellerPayout;
import com.kilivana.backend.finance.entity.SellerPayoutAccount;
import com.kilivana.backend.finance.entity.SellerWallet;
import com.kilivana.backend.finance.entity.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FinanceDtos {

    // ==================== LEDGER DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerAccountResponse {
        private Long id;
        private UUID uuid;
        private LedgerAccountType accountType;
        private Long sellerId;
        private String name;
        private BigDecimal balance;
        private boolean active;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static LedgerAccountResponse fromEntity(LedgerAccount account) {
            return LedgerAccountResponse.builder()
                    .id(account.getId())
                    .uuid(account.getUuid())
                    .accountType(account.getAccountType())
                    .sellerId(account.getSellerId())
                    .name(account.getName())
                    .balance(account.getBalance())
                    .active(account.isActive())
                    .description(account.getDescription())
                    .createdAt(account.getCreatedAt())
                    .updatedAt(account.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLineResponse {
        private Long id;
        private Long journalEntryId;
        private Long accountId;
        private LedgerAccountType accountType;
        private Long sellerId;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String description;

        public static JournalLineResponse fromEntity(JournalLine line) {
            return JournalLineResponse.builder()
                    .id(line.getId())
                    .journalEntryId(line.getJournalEntryId())
                    .accountId(line.getAccountId())
                    .accountType(line.getAccountType())
                    .sellerId(line.getSellerId())
                    .debitAmount(line.getDebitAmount())
                    .creditAmount(line.getCreditAmount())
                    .description(line.getDescription())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalEntryResponse {
        private Long id;
        private UUID uuid;
        private String reference;
        private LocalDateTime entryDate;
        private String description;
        private String sourceType;
        private Long sourceId;
        private Long createdBy;
        private boolean posted;
        private LocalDateTime postedAt;
        private Long reversalOf;
        private LocalDateTime createdAt;
        private List<JournalLineResponse> lines;

        public static JournalEntryResponse fromEntity(JournalEntry entry, List<JournalLine> lines) {
            return JournalEntryResponse.builder()
                    .id(entry.getId())
                    .uuid(entry.getUuid())
                    .reference(entry.getReference())
                    .entryDate(entry.getEntryDate())
                    .description(entry.getDescription())
                    .sourceType(entry.getSourceType())
                    .sourceId(entry.getSourceId())
                    .createdBy(entry.getCreatedBy())
                    .posted(entry.isPosted())
                    .postedAt(entry.getPostedAt())
                    .reversalOf(entry.getReversalOf())
                    .createdAt(entry.getCreatedAt())
                    .lines(lines.stream().map(JournalLineResponse::fromEntity).toList())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostJournalRequest {
        private String reference;
        private LocalDateTime entryDate;
        private String description;
        private String sourceType;
        private Long sourceId;
        private Long createdBy;
        private List<JournalLineRequest> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLineRequest {
        private Long accountId;
        private LedgerAccountType accountType;
        private Long sellerId;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String description;
    }

    // ==================== SELLER WALLET DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerWalletResponse {
        private Long id;
        private UUID uuid;
        private Long sellerId;
        private String sellerType;
        private BigDecimal pendingBalance;
        private BigDecimal availableBalance;
        private BigDecimal processingBalance;
        private BigDecimal totalPaid;
        private BigDecimal totalEarned;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static SellerWalletResponse fromEntity(SellerWallet wallet) {
            return SellerWalletResponse.builder()
                    .id(wallet.getId())
                    .uuid(wallet.getUuid())
                    .sellerId(wallet.getSellerId())
                    .sellerType(wallet.getSellerType() != null ? wallet.getSellerType().wire() : null)
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletTransactionResponse {
        private Long id;
        private UUID uuid;
        private Long walletId;
        private Long sellerId;
        private Long orderId;
        private Long sellerFulfilmentId;
        private WalletTransactionType transactionType;
        private BigDecimal amount;
        private WalletTransactionStatus status;
        private String reference;
        private String description;
        private BigDecimal balanceAfter;
        private BigDecimal pendingBalanceAfter;
        private BigDecimal availableBalanceAfter;
        private BigDecimal processingBalanceAfter;
        private LocalDateTime createdAt;

        public static WalletTransactionResponse fromEntity(WalletTransaction txn) {
            return WalletTransactionResponse.builder()
                    .id(txn.getId())
                    .uuid(txn.getUuid())
                    .walletId(txn.getWalletId())
                    .sellerId(txn.getSellerId())
                    .orderId(txn.getOrderId())
                    .sellerFulfilmentId(txn.getSellerFulfilmentId())
                    .transactionType(txn.getTransactionType())
                    .amount(txn.getAmount())
                    .status(txn.getStatus())
                    .reference(txn.getReference())
                    .description(txn.getDescription())
                    .balanceAfter(txn.getBalanceAfter())
                    .pendingBalanceAfter(txn.getPendingBalanceAfter())
                    .availableBalanceAfter(txn.getAvailableBalanceAfter())
                    .processingBalanceAfter(txn.getProcessingBalanceAfter())
                    .createdAt(txn.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletSummaryResponse {
        private BigDecimal pendingBalance;
        private BigDecimal availableBalance;
        private BigDecimal processingBalance;
        private BigDecimal totalBalance;
        private BigDecimal totalPaid;
        private BigDecimal totalEarned;
        private String status;
        private Long transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerWalletAdminResponse {
        private Long id;
        private UUID uuid;
        private Long sellerId;
        private String sellerName;
        private String sellerPhone;
        private String sellerType;
        private BigDecimal pendingBalance;
        private BigDecimal availableBalance;
        private BigDecimal processingBalance;
        private BigDecimal totalPaid;
        private BigDecimal totalEarned;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ==================== PAYOUT ACCOUNT DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerPayoutAccountResponse {
        private Long id;
        private UUID uuid;
        private Long sellerId;
        private PayoutMethod payoutMethod;
        private String accountName;
        private String mpesaPhone;
        private String maskedMpesaPhone;
        private String bankName;
        private String bankAccountNumber;
        private String maskedBankAccount;
        private boolean isDefault;
        private String verificationStatus;
        private LocalDateTime verifiedAt;
        private String verificationReference;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static SellerPayoutAccountResponse fromEntity(SellerPayoutAccount account) {
            return SellerPayoutAccountResponse.builder()
                    .id(account.getId())
                    .uuid(account.getUuid())
                    .sellerId(account.getSellerId())
                    .payoutMethod(account.getPayoutMethod())
                    .accountName(account.getAccountName())
                    .mpesaPhone(account.getMpesaPhone())
                    .maskedMpesaPhone(account.getMaskedMpesaPhone())
                    .bankName(account.getBankName())
                    .bankAccountNumber(account.getBankAccountNumber())
                    .maskedBankAccount(account.getMaskedBankAccount())
                    .isDefault(account.isDefault())
                    .verificationStatus(account.getVerificationStatus())
                    .verifiedAt(account.getVerifiedAt())
                    .verificationReference(account.getVerificationReference())
                    .notes(account.getNotes())
                    .createdAt(account.getCreatedAt())
                    .updatedAt(account.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerPayoutAccountRequest {
        private PayoutMethod payoutMethod;
        private String accountName;
        private String mpesaPhone;
        private String bankName;
        private String bankAccountNumber;
        private String bankBranchCode;
        private boolean isDefault;
    }

    // ==================== SELLER PAYOUT DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerPayoutResponse {
        private Long id;
        private UUID uuid;
        private Long sellerId;
        private Long walletId;
        private Long payoutAccountId;
        private BigDecimal amount;
        private PayoutMethod payoutMethod;
        private String destinationSnapshot;
        private PayoutStatus status;
        private String reference;
        private Long approvedBy;
        private LocalDateTime approvedAt;
        private String rejectionReason;
        private String providerReference;
        private Integer providerResultCode;
        private String providerResultDesc;
        private String mpesaReceiptNumber;
        private LocalDateTime paidAt;
        private int attemptCount;
        private LocalDateTime lastAttemptAt;
        private String idempotencyKey;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static SellerPayoutResponse fromEntity(SellerPayout payout) {
            return SellerPayoutResponse.builder()
                    .id(payout.getId())
                    .uuid(payout.getUuid())
                    .sellerId(payout.getSellerId())
                    .walletId(payout.getWalletId())
                    .payoutAccountId(payout.getPayoutAccountId())
                    .amount(payout.getAmount())
                    .payoutMethod(payout.getPayoutMethod())
                    .destinationSnapshot(payout.getDestinationSnapshot())
                    .status(payout.getStatus())
                    .reference(payout.getReference())
                    .approvedBy(payout.getApprovedBy())
                    .approvedAt(payout.getApprovedAt())
                    .rejectionReason(payout.getRejectionReason())
                    .providerReference(payout.getProviderReference())
                    .providerResultCode(payout.getProviderResultCode())
                    .providerResultDesc(payout.getProviderResultDesc())
                    .mpesaReceiptNumber(payout.getMpesaReceiptNumber())
                    .paidAt(payout.getPaidAt())
                    .attemptCount(payout.getAttemptCount())
                    .lastAttemptAt(payout.getLastAttemptAt())
                    .idempotencyKey(payout.getIdempotencyKey())
                    .notes(payout.getNotes())
                    .createdAt(payout.getCreatedAt())
                    .updatedAt(payout.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerPayoutRequest {
        private BigDecimal amount;
        private Long payoutAccountId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminPayoutApprovalRequest {
        private boolean approve;
        private String rejectionReason;
    }

    // ==================== REFUND DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundTransactionResponse {
        private Long id;
        private UUID uuid;
        private Long paymentId;
        private Long orderId;
        private Long sellerFulfilmentId;
        private Long sellerId;
        private Long buyerId;
        private BigDecimal amount;
        private String reason;
        private String status;
        private String reference;
        private String providerReference;
        private Integer providerResultCode;
        private String providerResultDesc;
        private Long approvedBy;
        private LocalDateTime approvedAt;
        private String rejectionReason;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static RefundTransactionResponse fromEntity(RefundTransaction refund) {
            return RefundTransactionResponse.builder()
                    .id(refund.getId())
                    .uuid(refund.getUuid())
                    .paymentId(refund.getPaymentId())
                    .orderId(refund.getOrderId())
                    .sellerFulfilmentId(refund.getSellerFulfilmentId())
                    .sellerId(refund.getSellerId())
                    .buyerId(refund.getBuyerId())
                    .amount(refund.getAmount())
                    .reason(refund.getReason())
                    .status(refund.getStatus())
                    .reference(refund.getReference())
                    .providerReference(refund.getProviderReference())
                    .providerResultCode(refund.getProviderResultCode())
                    .providerResultDesc(refund.getProviderResultDesc())
                    .approvedBy(refund.getApprovedBy())
                    .approvedAt(refund.getApprovedAt())
                    .rejectionReason(refund.getRejectionReason())
                    .completedAt(refund.getCompletedAt())
                    .createdAt(refund.getCreatedAt())
                    .updatedAt(refund.getUpdatedAt())
                    .build();
        }
    }

    // ==================== AUDIT LOG DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialAuditLogResponse {
        private Long id;
        private UUID uuid;
        private Long actorId;
        private String action;
        private String entityType;
        private Long entityId;
        private String description;
        private String parameters;
        private String clientIp;
        private String userAgent;
        private LocalDateTime createdAt;

        public static FinancialAuditLogResponse fromEntity(FinancialAuditLog log) {
            return FinancialAuditLogResponse.builder()
                    .id(log.getId())
                    .uuid(log.getUuid())
                    .actorId(log.getActorId())
                    .action(log.getAction())
                    .entityType(log.getEntityType())
                    .entityId(log.getEntityId())
                    .description(log.getDescription())
                    .parameters(log.getParameters())
                    .clientIp(log.getClientIp())
                    .userAgent(log.getUserAgent())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }

    // ==================== ADMIN DASHBOARD DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminFinanceDashboardResponse {
        private BigDecimal platformRevenue;
        private BigDecimal grossOrderVolume;
        private BigDecimal sellerPayables;
        private BigDecimal pendingPayouts;
        private BigDecimal settledFunds;
        private BigDecimal openRefunds;
        private BigDecimal totalCollections;
        private BigDecimal totalPayouts;
        private BigDecimal totalRefunds;
    }
}