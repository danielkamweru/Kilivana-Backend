package com.kilivana.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates Render's {@code DATABASE_URL} (a full
 * {@code postgres://user:password@host:port/database} URL) into the
 * datasource properties Spring Boot reads, so the service connects to
 * the attached PostgreSQL instance without any committed credentials.
 *
 * <p>{@code DATABASE_URL} takes precedence over {@code DB_URL}/
 * {@code DB_USERNAME}/{@code DB_PASSWORD} when both are present,
 * because it is the value the platform owns. The parsed values are
 * added as the highest-precedence property source, so they win over
 * anything in {@code application.properties}.
 *
 * <p>Credentials are moved out of the URL into the dedicated
 * {@code spring.datasource.username}/{@code password} properties: the
 * PostgreSQL driver cannot be relied upon to parse a password that
 * contains {@code @}, {@code :} or percent-encoding, and Render
 * percent-encodes whatever secret it generates.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        String jdbcUrl = toJdbcUrl(databaseUrl.trim());
        // URI parses "jdbc:postgresql://..." as an opaque URI,
        // so the prefix comes off before parsing.
        URI uri = URI.create(jdbcUrl.startsWith("jdbc:")
                ? jdbcUrl.substring("jdbc:".length())
                : jdbcUrl);

        Map<String, Object> datasourceProperties = new HashMap<>();
        datasourceProperties.put("spring.datasource.url", jdbcUrlWithoutCredentials(uri));
        if (uri.getUserInfo() != null) {
            String[] credentials = uri.getUserInfo().split(":", 2);
            datasourceProperties.put("spring.datasource.username", decode(credentials[0]));
            if (credentials.length > 1) {
                datasourceProperties.put("spring.datasource.password", decode(credentials[1]));
            }
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource("databaseUrl", datasourceProperties));
    }

    /**
     * Normalizes a platform database URL into the JDBC form Spring
     * Boot expects: {@code jdbc:postgresql://user:password@host:port/database}.
     * A value that already carries the {@code jdbc:} prefix is left
     * untouched, and {@code postgres://} becomes {@code jdbc:postgresql://}.
     */
    private static String toJdbcUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:")) {
            return databaseUrl;
        }
        if (databaseUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
        }
        if (databaseUrl.startsWith("postgresql://")) {
            return "jdbc:" + databaseUrl;
        }
        return databaseUrl;
    }

    /**
     * Rebuilds the URL without its userinfo, so the driver never has
     * to parse a password that may contain reserved characters.
     */
    private static String jdbcUrlWithoutCredentials(URI uri) {
        StringBuilder url = new StringBuilder("jdbc:")
                .append(uri.getScheme()).append("://");
        url.append(uri.getHost());
        if (uri.getPort() != -1) {
            url.append(':').append(uri.getPort());
        }
        if (uri.getPath() != null) {
            url.append(uri.getPath());
        }
        if (uri.getQuery() != null) {
            url.append('?').append(uri.getQuery());
        }
        return url.toString();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
