package com.kilivana.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs every HTTP request the application receives, including traffic that arrives through
 * ngrok, and correlates it with error logs through an MDC trace id.
 *
 * <p>It runs once per request, wraps the response so the final status code is captured
 * after the filter chain has executed, and is placed before the Spring Security filter so
 * authentication failures are visible too. The trace id is generated here unless the
 * caller already sent one in {@code X-Request-ID}, and is echoed back on the response
 * header so clients can correlate their own requests with the server side.
 *
 * <p>Passwords, JWTs and Authorization headers are never logged: only the method, URI,
 * status and duration are recorded, and the body is never inspected.
 *
 * <p>This class is not a Spring bean on its own — it is wired as a bean in
 * {@code SecurityConfig} and inserted before the JWT filter, so a single instance runs
 * per request rather than being auto-registered twice.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** Header the client may set to carry a trace id forward; when present it is reused. */
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    /** MDC key under which the trace id is exposed to every log statement. */
    private static final String MDC_REQUEST_ID = "requestId";

    /** Paths whose volume would drown the useful traffic in noise. */
    private static final String HEALTH_PATH = "/actuator/health";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        response.setHeader(REQUEST_ID_HEADER, traceId);
        MDC.put(MDC_REQUEST_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            logRequest(request, status, durationMs);
            MDC.clear();
        }
    }

    /**
     * Emits a single structured line per request. Health checks are skipped entirely so a
     * load balancer's probe does not produce a line per heartbeat.
     */
    private void logRequest(HttpServletRequest request, int status, long durationMs) {
        if (request.getRequestURI().equals(HEALTH_PATH)) {
            return;
        }
        log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                status, durationMs);
    }
}