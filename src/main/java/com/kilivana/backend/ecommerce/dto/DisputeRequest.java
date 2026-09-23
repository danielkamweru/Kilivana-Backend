package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Raised by is required")
    private Long raisedBy;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;

    private DisputeStatus status;
}
