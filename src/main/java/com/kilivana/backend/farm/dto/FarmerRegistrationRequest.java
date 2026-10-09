package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for registering a new farmer along with their account, KYC, and farms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for registering a new farmer with account, KYC, and farms")
public class FarmerRegistrationRequest {

    /**
     * Nested account details for the farmer registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Account details for the farmer registration")
    public static class Account {
        @NotBlank @Schema(description = "Full name of the farmer", example = "John Mutua")
        private String name;

        @NotBlank @Schema(description = "Email address of the farmer", example = "john.mutua@example.com")
        private String email;

        @NotBlank @Schema(description = "Phone number of the farmer", example = "+254700123456")
        private String phone;

        @Schema(description = "Username for the account", example = "johnmutua")
        private String username;

        @NotBlank @Size(min = 8) @Schema(description = "Password for the account", example = "securePassword123")
        private String password;

        @NotBlank @Schema(description = "County where the farmer is located", example = "Nairobi")
        private String county;
    }

    /**
     * Nested KYC details for the farmer registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "KYC details for the farmer registration")
    public static class Kyc {
        @Schema(description = "National ID number of the farmer", example = "12345678")
        private String nationalIdNumber;

        @Schema(description = "KRA PIN of the farmer", example = "P000123456")
        private String kraPin;
    }

    /**
     * Nested crop details for a farm in the registration request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Crop details for a farm in the registration request")
    public static class Crop {
        @NotNull @Schema(description = "Identifier of the crop type", example = "3")
        private Long cropTypeId;

        @Schema(description = "Variety of the crop", example = "BH540")
        private String variety;

        @NotNull @Schema(description = "Area allocated to the crop in acres", example = "2.5")
        private Double areaAcres;

        @Schema(description = "Status of the crop", example = "PLANTED")
        private String status;

        @Schema(description = "Date when the crop was planted", example = "2024-03-15")
        private LocalDate plantingDate;

        @Schema(description = "Expected date of harvest", example = "2024-09-15")
        private LocalDate expectedHarvestDate;

        @Schema(description = "Expected yield of the crop in kilograms", example = "1500")
        private Integer expectedYieldKg;
    }

    /**
     * Nested farm details for the farmer registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Farm details for the farmer registration")
    public static class Farm {
        @NotBlank @Schema(description = "Name of the farm", example = "Green Valley Farm")
        private String name;

        @NotBlank @Schema(description = "County where the farm is located", example = "Nairobi")
        private String county;

        @Schema(description = "Sub-county where the farm is located", example = "Westlands")
        private String subCounty;

        @Schema(description = "Postal address of the farm", example = "P.O. Box 123, Nairobi")
        private String address;

        @Schema(description = "Latitude coordinate of the farm", example = "-1.2921")
        private Double latitude;

        @Schema(description = "Longitude coordinate of the farm", example = "36.8219")
        private Double longitude;

        @NotNull @Schema(description = "Size of the farm in acres", example = "10.0")
        private Double sizeAcres;

        @Schema(description = "Ownership type of the farm", example = "LEASED")
        private String ownershipType;

        @Schema(description = "Description of the farm", example = "A productive farm growing maize and beans")
        private String description;

        @Size(min = 1) @Schema(description = "List of crops in the farm")
        private List<Crop> crops;
    }

    @Valid @Schema(description = "Account details for the farmer")
    private Account account;

    @Schema(description = "KYC details for the farmer")
    private Kyc kyc;

    @Valid @Size(min = 1) @Schema(description = "List of farms to register for the farmer")
    private List<Farm> farms;
}
