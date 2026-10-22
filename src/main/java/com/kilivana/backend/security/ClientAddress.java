package com.kilivana.backend.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the address of the client that sent a request.
 *
 * <p>The backend is reached through a tunnel in development and behind a reverse proxy
 * in production, so {@code getRemoteAddr()} is the proxy's address rather than the
 * client's. The forwarded header is preferred for that reason.
 *
 * <p>The last entry of {@code X-Forwarded-For} is used, because that is the entry the
 * closest proxy appended itself; earlier entries were supplied by the client and can be
 * written freely by it. A client that talks to the backend directly can still forge the
 * header, so this value is trusted for grouping and throttling, never for identity.
 */
public final class ClientAddress {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN = "unknown";

    private ClientAddress() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwarded != null && !forwarded.isBlank()) {
            String last = null;
            for (String part : forwarded.split(",")) {
                String candidate = part.trim();
                if (!candidate.isEmpty()) {
                    last = candidate;
                }
            }
            if (last != null) {
                return last;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? UNKNOWN : remote;
    }
}
