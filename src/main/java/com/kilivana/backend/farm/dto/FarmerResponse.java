package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a farmer and their associated farms.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO representing a farmer with their associated farms")
public class FarmerResponse {
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

    @Schema(description = "Username for the farmer account", example = "johnmutua")
    private String username;

    @Schema(description = "County where the farmer is located", example = "Nairobi")
    private String county;

    @Schema(description = "Verification status of the farmer", example = "VERIFIED")
    private VerificationStatus verificationStatus;

    @Schema(description = "Current account status of the farmer", example = "ACTIVE")
    private UserStatus accountStatus;

    @Schema(description = "Timestamp when the farmer record was created", example = "2024-01-20T09:15:00")
    private LocalDateTime createdAt;

    @Schema(description = "List of farms owned by the farmer")
    private List<FarmResponse> farms;

    // Static factory method to replace @Builder
    public static FarmerResponse of(Long id, String referenceCode, String name, String email, String phone,
                                    String username, String county, VerificationStatus verificationStatus,
                                    UserStatus accountStatus, LocalDateTime createdAt, List<FarmResponse> farms) {
        FarmerResponse response = new FarmerResponse();
        response.id = id;
        response.referenceCode = referenceCode;
        response.name = name;
        response.email = email;
        response.phone = phone;
        response.username = username;
        response.county = county;
        response.verificationStatus = verificationStatus;
        response.accountStatus = accountStatus;
        response.createdAt = createdAt;
        response.farms = farms;
        return response;
    }

    public static FarmerResponseBuilder builder() {
        return new FarmerResponseBuilder();
    }

    public static class FarmerResponseBuilder {
        private Long id;
        private String referenceCode;
        private String name;
        private String email;
        private String phone;
        private String username;
        private String county;
        private VerificationStatus verificationStatus;
        private UserStatus accountStatus;
        private LocalDateTime createdAt;
        private List<FarmResponse> farms;

        public FarmerResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public FarmerResponseBuilder referenceCode(String referenceCode) {
            this.referenceCode = referenceCode;
            return this;
        }

        public FarmerResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FarmerResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public FarmerResponseBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public FarmerResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public FarmerResponseBuilder county(String county) {
            this.county = county;
            return this;
        }

        public FarmerResponseBuilder verificationStatus(VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public FarmerResponseBuilder accountStatus(UserStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public FarmerResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public FarmerResponseBuilder farms(List<FarmResponse> farms) {
            this.farms = farms;
            return this;
        }

        public FarmerResponse build() {
            FarmerResponse response = new FarmerResponse();
            response.id = this.id;
            response.referenceCode = this.referenceCode;
            response.name = this.name;
            response.email = this.email;
            response.phone = this.phone;
            response.username = this.username;
            response.county = this.county;
            response.verificationStatus = this.verificationStatus;
            response.accountStatus = this.accountStatus;
            response.createdAt = this.createdAt;
            response.farms = this.farms;
            return response;
        }
    }
}
