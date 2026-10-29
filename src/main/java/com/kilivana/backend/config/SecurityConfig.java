package com.kilivana.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.config.RequestLoggingFilter;
import com.kilivana.backend.security.AuthRateLimiter;
import com.kilivana.backend.security.JwtAuthenticationFilter;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import com.kilivana.backend.security.RestAuthenticationErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Duration;
import java.util.Map;

/**
 * Declares the whole authentication graph here rather than as component-scanned
 * services, so that importing this configuration is enough to build a working
 * filter chain. That keeps {@code @WebMvcTest} slices working, which do not
 * scan {@code @Service} beans.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    // Methods must be passed to requestMatchers, not embedded in a path array.
    private static final String[] PUBLIC_GET_ENDPOINTS = {
        "/", "/favicon.ico", "/actuator/health", "/actuator/info", "/api/v1/health",
        "/uploads/**", "/api/v1/images/**", "/api/v1/products/**", "/ws", "/ws/**",
        "/api/v1/users/farmers/**", "/api/v1/categories/**", "/api/v1/crop-types/**", "/api/v1/regions", "/api/v1/regions/**",
        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
        "/api-docs", "/api-docs/**", "/reset-password"
    };
    private static final String[] PUBLIC_POST_ENDPOINTS = {
        "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
        "/api/v1/auth/email-verification/request", "/api/v1/auth/email-verification/resend", "/api/v1/auth/email-verification/verify",
        "/api/auth/email-verification/request", "/api/auth/email-verification/resend", "/api/auth/email-verification/verify",
        "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
        "/api/auth/forgot-password", "/api/auth/reset-password", "/api/v1/users", "/api/users",
        // Safaricom posts the STK Push result here and cannot present a token, so the
        // endpoint is public. It is defended in the callback service instead: the
        // callback must match a payment that was really initiated (checkout request
        // and merchant request ids) and is applied idempotently.
        "/api/v1/payments/daraja/callback"
    };

    @Bean
    /** BCrypt is the standard adaptive password hash; bcrypt's built-in salt
     *  means the stored hash never needs a separate salt column. */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    /** Declared here (rather than @Component-scanned) so the full auth graph is
     *  wired by importing this config alone, keeping @WebMvcTest slices working. */
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
            com.kilivana.backend.security.AccountAccessValidator accountAccessValidator) {
        return new JwtAuthenticationFilter(jwtService, accountAccessValidator);
    }

    @Bean
    public RestAuthenticationErrorHandler restAuthenticationErrorHandler(ObjectMapper objectMapper) {
        return new RestAuthenticationErrorHandler(objectMapper);
    }

    @Bean
    /**
     * Throttles the credential endpoints per client address, so a single caller cannot
     * guess passwords or flood a mailbox with recovery mail. Declared here, rather than
     * component-scanned, so importing this configuration is enough to build the graph.
     */
    public AuthRateLimiter authRateLimiter(
            @Value("${app.auth.rate-limit.window-minutes:1}") long windowMinutes,
            @Value("${app.auth.rate-limit.login-max-attempts:10}") int loginMaxAttempts,
            @Value("${app.auth.rate-limit.forgot-password-max-attempts:5}") int forgotPasswordMaxAttempts,
            @Value("${app.auth.rate-limit.reset-password-max-attempts:10}") int resetPasswordMaxAttempts) {
        Map<String, Integer> limits = Map.of(
                AuthRateLimiter.LOGIN, loginMaxAttempts,
                AuthRateLimiter.FORGOT_PASSWORD, forgotPasswordMaxAttempts,
                AuthRateLimiter.RESET_PASSWORD, resetPasswordMaxAttempts);
        return new AuthRateLimiter(Duration.ofMinutes(windowMinutes), limits);
    }

    /**
     * Assembles the stateless JWT security filter chain. CSRF is disabled
     * because the API is consumed by non-browser clients that manage their
     * own token storage; the JWT filter handles authentication instead.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationErrorHandler authenticationErrorHandler,
                                                   RequestLoggingFilter requestLoggingFilter)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> { })
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.HEAD, PUBLIC_GET_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                // CORS preflight requests carry no credentials, so they need no token.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Ordering and the order lifecycle are administrator powers.
                .requestMatchers(HttpMethod.POST, "/api/v1/logistics/jobs").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/logistics/jobs/{id}/assign").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/logistics/jobs/{id}/assign").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/logistics/jobs/{id}").hasRole("ADMIN")
                // Live tracking: only the assigned driver may push a fix; the buyer, the
                // sellers on the order and an administrator may read. The service layer
                // enforces the per-job relation, so the chain only gates the role.
                .requestMatchers(HttpMethod.POST, "/api/v1/tracking/{id}/location").hasRole("DRIVER")
                .requestMatchers(HttpMethod.GET, "/api/v1/tracking/{id}/location").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/tracking/{id}/status").authenticated()
                .requestMatchers("/api/v1/trips/**").hasRole("DRIVER")
                // The service authorizes status changes for the assigned driver or an admin.
                .requestMatchers(HttpMethod.PUT, "/api/v1/logistics/jobs/{id}/status").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/logistics/jobs/{id}/status").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/{id}/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/cancel").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/{id}").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationErrorHandler)
                .accessDeniedHandler(authenticationErrorHandler))
            // Runs before the JWT filter so a request that fails authentication is still
            // logged with its final 401 status and duration.
            .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
