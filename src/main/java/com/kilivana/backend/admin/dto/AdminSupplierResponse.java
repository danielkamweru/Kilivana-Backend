package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A supplier as the administration roster reads them.
 *
 * <p>Mirrors {@link AdminBuyerResponse} and {@link AdminDriverResponse}: one flat record
 * per supplier with the account, profile and derived facts the panel needs on the
 * list and detail screens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSupplierResponse {

    private Long userId;
    private Long profileId;
    /** Supplier code such as S-001, from the user row. */
    private String code;
    private String companyName;
    private String contactPerson;
    private String username;
    private String email;
    private String phone;
    private String region;
    private String address;
    private String category;
    private LocalDate contractEndDate;

    /** active, pending, or suspended. */
    private String status;
    private String suspensionReason;

    /** Number of products this supplier has listed. */
    private int productsCount;

    /** Average rating, or null if none exists yet. */
    private Double rating;

    private LocalDateTime createdAt;

    /** Maps backend user status to the panel vocabulary. */
    public static String mapStatus(com.kilivana.backend.common.enums.UserStatus status, VerificationStatus verificationStatus) {
        if (status == null) return null;
        return switch (status) {
            case ACTIVE -> "active";
            case SUSPENDED -> "suspended";
            case INACTIVE, PENDING_VERIFICATION -> "pending";
        };
    }
}
