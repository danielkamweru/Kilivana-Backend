package com.kilivana.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    /**
     * Swagger renders tags in alphabetical order, configured via
     * {@code springdoc.swagger-ui.tags-sorter=alpha}. Tag names are chosen so that
     * alphabetical order groups the documentation the way the API is organised:
     * Administration first, then E-Commerce, then Logistics.
     */

    @Bean
    public OpenAPI kilivanaOpenAPI() {
        Contact contact = new Contact();
        contact.setName("Kilivana Team");
        contact.setEmail("info@kilivana.com");

        Info info = new Info()
                .title("Kilivana Backend API")
                .version("1.0.0")
                .description("""
                        Kilivana marketplace backend covering administration, e-commerce and logistics.

                        Endpoints are grouped Administration, E-Commerce, then Logistics.

                        **Authentication** — call `POST /api/v1/auth/login` and use the returned \
                        `accessToken` in the Authorize dialog as a raw token (the `Bearer` prefix is \
                        added for you). Endpoints grouped under Authentication and Health & System \
                        are public and need no token.

                        Tokens are stateless JWTs. Development configuration issues tokens \
                        with no expiry so they stay valid until the signing secret changes. \
                        Set JWT_EXPIRATION to a duration in milliseconds to make them expire, \
                        and use `POST /api/v1/auth/refresh` with the refresh token to obtain a \
                        new pair.
                        """)
                .contact(contact);

        Components components = new Components().addSecuritySchemes(BEARER_SCHEME,
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste the raw accessToken returned by /api/v1/auth/login"));

        return new OpenAPI()
                .info(info)
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .servers(buildServers());
    }

    /**
     * Advertises the deployed origin when one is configured so that Swagger
     * "Try it out" calls the public URL instead of the host the documentation
     * happens to be served from. When no origin is configured no server is
     * listed, and the UI calls whatever host the browser is on - which is
     * exactly what a deployment on Render wants. Never hard-codes a tunnel
     * hostname, because free tunnels are reassigned on every restart.
     */
    private List<Server> buildServers() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return List.of();
        }

        Server remote = new Server()
                .url(publicBaseUrl)
                .description("Deployed API");
        return List.of(remote);
    }
}
