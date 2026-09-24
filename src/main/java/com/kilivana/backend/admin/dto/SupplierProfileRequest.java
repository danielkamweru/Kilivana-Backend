package com.kilivana.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProfileRequest {

    @NotBlank
    private String businessName;

    private String businessDetails;

    @NotBlank
    private String location;

    private String verificationInfo;
}