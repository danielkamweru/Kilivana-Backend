package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a farmer entry in a list of farmers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing a farmer entry in a farmer listing")
public class FarmerListItem {
    @Schema(description = "Unique identifier of the farmer", example = "12")
    private Long id;

    @Schema(description = "Reference code assigned to the farmer", example = "FARM-0012")
    private String referenceCode;

    @Schema(description = "Full name of the farmer", example = "John Mutua")
    private String name;

    @Schema(description = "Email address of the farmer", example = "john.mutua@example.com")
    private String email;

    @Schema(description = "Phone number of the farmer", example = "+254700123456")
    private String phone;

    @Schema(description = "County where the farmer is located", example = "Nairobi")
    private String county;

    @Schema(description = "Current account status of the farmer", example = "ACTIVE")
    private UserStatus accountStatus;

    @Schema(description = "Verification status of the farmer", example = "VERIFIED")
    private VerificationStatus verificationStatus;

    @Schema(description = "Reason for the current status, if applicable", example = "Pending document verification")
    private String statusReason;

    @Schema(description = "Timestamp when the status last changed", example = "2024-04-01T08:00:00")
    private LocalDateTime statusChangedAt;

    @Schema(description = "Identifier of the user who last changed the status", example = "1")
    private Long statusChangedBy;

    @Schema(description = "List of crop names grown by the farmer", example = "[\"Maize\", \"Beans\"]")
    private List<String> cropNames;

    @Schema(description = "Number of farms owned by the farmer", example = "3")
    private int farmCount;

    @Schema(description = "Average rating of the farmer", example = "4.5")
    private Double rating;

    @Schema(description = "Number of ratings received", example = "10")
    private int ratingCount;

    @Schema(description = "Total revenue associated with the farmer", example = "150000.00")
    private Double totalRevenue;

    @Schema(description = "Timestamp when the farmer record was created", example = "2024-01-20T09:15:00")
    private LocalDateTime createdAt;
}
