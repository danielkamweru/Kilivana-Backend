package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A buyer as the administrator's buyer roster reads them: the account, profile
 * and order/dispute summary in one row.
 *
 * <p>The admin panel shows all of these together. Fetching them separately meant the
 * list had to be stitched together client-side from a user list, a profile per user,
 * and a job list, which meant a buyer without a profile either vanished or threw.
 */
@Data
@Builder
public class AdminBuyerResponse {

    private Long userId;
    private Long profileId;
    /** Buyer code such as B-005, from the user row. */
    private String code;
    private String fullName;
    private String email;
    private String phone;
    private String region;
    private String address;

    /**
     * Maps the backend user status to what the admin panel reads:
     * VERIFIED for active, PENDING for pending-verification, SUSPENDED for suspended.
     */
    private String status;

    // Buyer type is not tracked in the backend; null until the schema carries it.
    private String type;

    // Order history
    /** Number of orders this buyer has placed. */
    private long ordersCount;
    /** Sum of all order totals (cents as a whole number in the panel's display). */
    private BigDecimal totalSpend;

    // Dispute history
    /** Number of disputes this buyer has raised. */
    private long disputesCount;

    private LocalDateTime createdAt;
}
