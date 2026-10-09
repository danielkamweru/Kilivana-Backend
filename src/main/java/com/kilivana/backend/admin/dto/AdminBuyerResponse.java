package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Admin view of a buyer with profile and order summary")
public class AdminBuyerResponse {

    @Schema(description = "User account ID", example = "10")
    private Long userId;

    @Schema(description = "Buyer profile ID", example = "5")
    private Long profileId;

    @Schema(description = "Buyer code such as B-005", example = "B-005")
    private String code;

    @Schema(description = "Full name of the buyer", example = "John Kamau")
    private String fullName;

    @Schema(description = "Email address", example = "john.kamau@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+254700000001")
    private String phone;

    @Schema(description = "Region or county", example = "Nairobi")
    private String region;

    @Schema(description = "Physical address", example = "123 Mombasa Road, Nairobi")
    private String address;

    @Schema(description = "Mapped status for admin panel: active, pending, or suspended", example = "active")
    private String status;

    @Schema(description = "Buyer type (not currently tracked in backend)", example = "retail")
    private String type;

    @Schema(description = "Number of orders this buyer has placed", example = "12")
    private long ordersCount;

    @Schema(description = "Sum of all order totals", example = "150000.00")
    private BigDecimal totalSpend;

    @Schema(description = "Number of disputes this buyer has raised", example = "1")
    private long disputesCount;

    @Schema(description = "Timestamp when the buyer account was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;
}
