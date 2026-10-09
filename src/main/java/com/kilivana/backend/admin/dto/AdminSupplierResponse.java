package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Admin view of a supplier with profile and product summary")
public class AdminSupplierResponse {

    @Schema(description = "User account ID", example = "30")
    private Long userId;

    @Schema(description = "Supplier profile ID", example = "12")
    private Long profileId;

    @Schema(description = "Supplier code such as S-001", example = "S-001")
    private String code;

    @Schema(description = "Company name", example = "Green Valley Farms Ltd")
    private String companyName;

    @Schema(description = "Contact person name", example = "Mary Wanjiku")
    private String contactPerson;

    @Schema(description = "Username", example = "greenvalley")
    private String username;

    @Schema(description = "Email address", example = "info@greenvalley.co.ke")
    private String email;

    @Schema(description = "Phone number", example = "+254722000003")
    private String phone;

    @Schema(description = "Region or county", example = "Nakuru")
    private String region;

    @Schema(description = "Physical address", example = "Plot 45, Nakuru-Eldoret Road")
    private String address;

    @Schema(description = "Product category", example = "Vegetables")
    private String category;

    @Schema(description = "Contract end date", example = "2026-12-31")
    private LocalDate contractEndDate;

    @Schema(description = "Status for admin panel: active, pending, or suspended", example = "active")
    private String status;

    @Schema(description = "Reason if supplier is suspended", example = "Contract expired")
    private String suspensionReason;

    @Schema(description = "Number of products this supplier has listed", example = "15")
    private int productsCount;

    @Schema(description = "Average rating, or null if none exists yet", example = "4.3")
    private Double rating;

    @Schema(description = "Timestamp when the supplier account was created", example = "2026-01-15T10:30:00")
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
