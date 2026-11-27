package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.PayoutMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A seller's registered payout account for withdrawals.
 * Initially supports M-Pesa; designed for future bank account support.
 */
@Entity
@Table(name = "seller_payout_accounts",
        indexes = {
                @Index(name = "idx_payout_account_seller", columnList = "seller_id"),
                @Index(name = "idx_payout_account_default", columnList = "seller_id, is_default"),
                @Index(name = "idx_payout_account_verified", columnList = "verification_status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPayoutAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false)
    private PayoutMethod payoutMethod;

    /** Account holder name (must match seller's verified identity). */
    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    /** M-Pesa phone number in 2547XXXXXXXX format. */
    @Column(name = "mpesa_phone", length = 15)
    private String mpesaPhone;

    /** Bank name (for future BANK_TRANSFER support). */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /** Bank account number (for future BANK_TRANSFER support). */
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    /** Bank branch/sort code (for future BANK_TRANSFER support). */
    @Column(name = "bank_branch_code", length = 20)
    private String bankBranchCode;

    /** Whether this is the seller's default payout account. */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /** Verification status of the account details. */
    @Column(name = "verification_status", nullable = false, length = 32)
    @Builder.Default
    private String verificationStatus = "UNVERIFIED";

    /** Last verification attempt timestamp. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** Verification reference from provider (e.g., M-Pesa B2C validation). */
    @Column(name = "verification_reference", length = 64)
    private String verificationReference;

    /** Notes from admin or verification process. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    /** Returns masked phone for display (e.g., 2547***1234). */
    public String getMaskedMpesaPhone() {
        if (mpesaPhone == null || mpesaPhone.length() < 6) {
            return "****";
        }
        return mpesaPhone.substring(0, 3) + "***" + mpesaPhone.substring(mpesaPhone.length() - 2);
    }

    /** Returns masked bank account for display. */
    public String getMaskedBankAccount() {
        if (bankAccountNumber == null || bankAccountNumber.length() < 4) {
            return "****";
        }
        return "****" + bankAccountNumber.substring(bankAccountNumber.length() - 4);
    }
}