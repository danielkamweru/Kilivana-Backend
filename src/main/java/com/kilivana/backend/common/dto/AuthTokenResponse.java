package com.kilivana.backend.common.dto;

import com.kilivana.backend.admin.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The token pair issued by {@code login} and {@code refresh}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {

    /** Short-lived JWT; send it as {@code Authorization: Bearer <accessToken>}. */
    private String accessToken;

    /** Long-lived JWT exchanged for a fresh pair at {@code POST /api/v1/auth/refresh}. */
    private String refreshToken;

    /** Always {@code Bearer}. */
    private String tokenType;

    /** Access-token lifetime in seconds; {@code null} when the token has no expiry. */
    private Long expiresIn;

    /** Profile of the authenticated user, so the client needs no follow-up call. */
    private UserResponse user;
}
