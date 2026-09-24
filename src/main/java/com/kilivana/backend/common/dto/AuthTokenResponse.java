package com.kilivana.backend.common.dto;

import com.kilivana.backend.admin.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserResponse user;
}
