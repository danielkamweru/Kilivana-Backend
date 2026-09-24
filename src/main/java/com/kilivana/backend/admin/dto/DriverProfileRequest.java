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
public class DriverProfileRequest {

    @NotBlank
    private String licenseNumber;

    @NotBlank
    private String vehicleType;

    @NotBlank
    private String vehicleNumber;

    private String vehicleDetails;

    @NotBlank
    private String availabilityStatus;
}