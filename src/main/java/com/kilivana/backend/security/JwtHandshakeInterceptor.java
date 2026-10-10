package com.kilivana.backend.security;

import com.kilivana.backend.common.exception.UnauthorizedException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Validates the bearer token a WebSocket client sends in the query string and stashes the
 * resolved user id on the session so the channel interceptor can enforce authorization.
 * Browsers cannot reliably put an {@code Authorization} header on a WebSocket handshake in
 * every client, so the token travels as a query parameter instead; the same validation path
 * the HTTP filter uses is applied here.
 *
 * <p>The principal is the user id, matching {@link JwtAuthenticationFilter}, so the rest
 * of the security layer can treat WebSocket and HTTP callers identically.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    static final String TOKEN_QUERY_PARAM = "token";
    public static final String USER_ID_PROPERTY = "jwtUserId";

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @Nullable ServerHttpResponse response,
                                   @Nullable WebSocketHandler handler,
                                   @NonNull Map<String, Object> attributes) {
        String token = getQueryParam(request, TOKEN_QUERY_PARAM);
        if (token == null) {
            return false;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        try {
            if (!jwtService.isTokenType(token, JwtService.TYPE_ACCESS)) {
                throw new UnauthorizedException("WebSocket token is not an access token");
            }
            Long userId = jwtService.extractUserId(token);
            attributes.put(USER_ID_PROPERTY, userId);
            return true;
        } catch (JwtException | UnauthorizedException ex) {
            log.debug("Rejected WebSocket handshake token: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(@Nullable ServerHttpRequest request,
                               @Nullable ServerHttpResponse response,
                               @Nullable WebSocketHandler handler,
                               @Nullable Exception exception) {
        // Nothing to clean up: the user id is read per-message by the channel interceptor.
    }

    private static String getQueryParam(ServerHttpRequest request, String name) {
        String query = request.getURI().getQuery();
        if (query == null) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}