package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a farmer entry in a list of farmers.
 */
@Data
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

    // Static factory method to replace @Builder
    public static FarmerListItem of(Long id, String referenceCode, String name, String email, String phone,
                                    String county, UserStatus accountStatus, VerificationStatus verificationStatus,
                                    String statusReason, LocalDateTime statusChangedAt, Long statusChangedBy,
                                    List<String> cropNames, int farmCount, Double rating, int ratingCount,
                                    Double totalRevenue, LocalDateTime createdAt) {
        FarmerListItem item = new FarmerListItem();
        item.id = id;
        item.referenceCode = referenceCode;
        item.name = name;
        item.email = email;
        item.phone = phone;
        item.county = county;
        item.accountStatus = accountStatus;
        item.verificationStatus = verificationStatus;
        item.statusReason = statusReason;
        item.statusChangedAt = statusChangedAt;
        item.statusChangedBy = statusChangedBy;
        item.cropNames = cropNames;
        item.farmCount = farmCount;
        item.rating = rating;
        item.ratingCount = ratingCount;
        item.totalRevenue = totalRevenue;
        item.createdAt = createdAt;
        return item;
    }

    public static FarmerListItemBuilder builder() {
        return new FarmerListItemBuilder();
    }

    public static class FarmerListItemBuilder {
        private Long id;
        private String referenceCode;
        private String name;
        private String email;
        private String phone;
        private String county;
        private UserStatus accountStatus;
        private VerificationStatus verificationStatus;
        private String statusReason;
        private LocalDateTime statusChangedAt;
        private Long statusChangedBy;
        private List<String> cropNames;
        private int farmCount;
        private Double rating;
        private int ratingCount;
        private Double totalRevenue;
        private LocalDateTime createdAt;

        public FarmerListItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public FarmerListItemBuilder referenceCode(String referenceCode) {
            this.referenceCode = referenceCode;
            return this;
        }

        public FarmerListItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FarmerListItemBuilder email(String email) {
            this.email = email;
            return this;
        }

        public FarmerListItemBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public FarmerListItemBuilder county(String county) {
            this.county = county;
            return this;
        }

        public FarmerListItemBuilder accountStatus(UserStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public FarmerListItemBuilder verificationStatus(VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public FarmerListItemBuilder statusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public FarmerListItemBuilder statusChangedAt(LocalDateTime statusChangedAt) {
            this.statusChangedAt = statusChangedAt;
            return this;
        }

        public FarmerListItemBuilder statusChangedBy(Long statusChangedBy) {
            this.statusChangedBy = statusChangedBy;
            return this;
        }

        public FarmerListItemBuilder cropNames(List<String> cropNames) {
            this.cropNames = cropNames;
            return this;
        }

        public FarmerListItemBuilder farmCount(int farmCount) {
            this.farmCount = farmCount;
            return this;
        }

        public FarmerListItemBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public FarmerListItemBuilder ratingCount(int ratingCount) {
            this.ratingCount = ratingCount;
            return this;
        }

        public FarmerListItemBuilder totalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public FarmerListItemBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public FarmerListItem build() {
            FarmerListItem item = new FarmerListItem();
            item.id = this.id;
            item.referenceCode = this.referenceCode;
            item.name = this.name;
            item.email = this.email;
            item.phone = this.phone;
            item.county = this.county;
            item.accountStatus = this.accountStatus;
            item.verificationStatus = this.verificationStatus;
            item.statusReason = this.statusReason;
            item.statusChangedAt = this.statusChangedAt;
            item.statusChangedBy = this.statusChangedBy;
            item.cropNames = this.cropNames;
            item.farmCount = this.farmCount;
            item.rating = this.rating;
            item.ratingCount = this.ratingCount;
            item.totalRevenue = this.totalRevenue;
            item.createdAt = this.createdAt;
            return item;
        }
    }
}
