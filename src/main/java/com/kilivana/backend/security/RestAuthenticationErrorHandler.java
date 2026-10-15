package com.kilivana.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Translates Spring Security's authentication and authorization failures into
 * the project's {@link com.kilivana.backend.common.dto.ApiResponse} shape.
 * A failed authentication (no/invalid token) yields 401, while a valid token
 * without the required role yields 403; both are emitted as JSON so API
 * clients never receive Spring's default HTML error page.
 */
@RequiredArgsConstructor
public class RestAuthenticationErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Authentication is required to access this resource");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to access this resource");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .error(ApiResponse.ErrorDetail.builder()
                        .code(code)
                        .details(message)
                        .build())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
