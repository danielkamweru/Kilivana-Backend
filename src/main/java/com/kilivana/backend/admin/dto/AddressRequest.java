package com.kilivana.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Address text is required")
    private String addressText;

    private Double latitude;
    private Double longitude;
    private String label;

    @NotNull(message = "User id is required")
    private Long userId;
}
