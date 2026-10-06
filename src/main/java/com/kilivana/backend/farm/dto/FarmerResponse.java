package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerResponse {
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
}
