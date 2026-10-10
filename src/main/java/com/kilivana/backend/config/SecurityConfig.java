package com.kilivana.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.config.RequestLoggingFilter;
import com.kilivana.backend.security.JwtAuthenticationFilter;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import com.kilivana.backend.security.RestAuthenticationErrorHandler;
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

    /** Endpoints reachable without a token. Everything else requires a valid access token. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "GET", "/",
            "GET", "/favicon.ico",
            "GET", "/actuator/health",
            "GET", "/actuator/info",
            // Render's health check hits the API path directly.
            "GET", "/api/v1/health",
            // Uploaded images are referenced by URL from <img> tags and mobile clients,
            // which cannot attach a bearer token. Filenames are random, so the files are
            // safe to read without a token.
            "GET", "/uploads/**",
            // Same reason as /uploads/**: images kept in PostgreSQL are served from here and
            // are referenced from <img> tags and mobile payloads that carry no bearer token.
            "GET", "/api/v1/images/**",
            // Product catalogue browsing is public: categories, product listing, search, etc.
            "GET", "/api/v1/products/**",
            // Live tracking WebSocket. The bearer token arrives in the query string and is
            // validated by the handshake handler, so the endpoint itself is public while
            // the channel interceptor rejects messages from unauthenticated sessions.
            "GET", "/ws",
            "GET", "/ws/**",
            "GET", "/api/v1/categories/**",
            // Crop types for farmer registration dropdown
            "GET", "/api/v1/crop-types/**",
            // Reference data: counties and currency
            "GET", "/api/v1/regions",
            "GET", "/api/v1/regions/**",
            "POST", "/api/v1/auth/register",
            "POST", "/api/v1/auth/login",
            "POST", "/api/v1/auth/refresh",
            "POST", "/api/v1/auth/forgot-password",
            "POST", "/api/v1/auth/reset-password",
            "POST", "/api/auth/register",
            "POST", "/api/auth/login",
            "POST", "/api/auth/refresh",
            "POST", "/api/auth/forgot-password",
            "POST", "/api/auth/reset-password",
            "POST", "/api/v1/users",
            "POST", "/api/users",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/api-docs",
            "/api-docs/**",
            "/error"
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
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public RestAuthenticationErrorHandler restAuthenticationErrorHandler(ObjectMapper objectMapper) {
        return new RestAuthenticationErrorHandler(objectMapper);
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
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                // CORS preflight requests carry no credentials, so they need no token.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
                .requestMatchers(HttpMethod.PUT, "/api/v1/logistics/jobs/{id}/status").hasRole("ADMIN")
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
