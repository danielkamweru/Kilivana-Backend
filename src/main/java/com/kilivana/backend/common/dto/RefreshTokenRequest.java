package com.kilivana.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of POST /api/v1/auth/refresh.
 *
 * <p>Kept separate from {@link AuthTokenResponse} so a client does not have to send an
 * access token or user object it does not have in order to refresh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    private String refreshToken;
}
