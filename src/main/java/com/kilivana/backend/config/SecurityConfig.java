package com.kilivana.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            // Uploaded images are referenced by URL from <img> tags and mobile clients,
            // which cannot attach a bearer token. Filenames are random, so the files are
            // safe to read without a token.
            "GET", "/uploads/**",
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public RestAuthenticationErrorHandler restAuthenticationErrorHandler(ObjectMapper objectMapper) {
        return new RestAuthenticationErrorHandler(objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationErrorHandler authenticationErrorHandler)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> { })
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationErrorHandler)
                .accessDeniedHandler(authenticationErrorHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
