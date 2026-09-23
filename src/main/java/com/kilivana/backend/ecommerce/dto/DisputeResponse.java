package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {

    private Long id;
    private Long orderId;
    private Long raisedBy;
    private String reason;
    private String description;
    private DisputeStatus status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
