package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerListItem {
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
}
