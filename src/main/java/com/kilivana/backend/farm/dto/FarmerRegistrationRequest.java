package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerRegistrationRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Account {
        @NotBlank private String name;
        @NotBlank private String email;
        @NotBlank private String phone;
        private String username;
        @NotBlank @Size(min = 8) private String password;
        @NotBlank private String county;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kyc {
        private String nationalIdNumber;
        private String kraPin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Crop {
        @NotNull private Long cropTypeId;
        private String variety;
        @NotNull private Double areaAcres;
        private String status;
        private LocalDate plantingDate;
        private LocalDate expectedHarvestDate;
        private Integer expectedYieldKg;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Farm {
        @NotBlank private String name;
        @NotBlank private String county;
        private String subCounty;
        private String address;
        private Double latitude;
        private Double longitude;
        @NotNull private Double sizeAcres;
        private String ownershipType;
        private String description;
        @Size(min = 1) private List<Crop> crops;
    }

    @Valid private Account account;
    private Kyc kyc;
    @Valid @Size(min = 1) private List<Farm> farms;
}
