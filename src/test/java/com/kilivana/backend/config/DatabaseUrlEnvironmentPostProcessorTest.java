package com.kilivana.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Guards the {@code DATABASE_URL} translation. Render hands the
 * application a bare {@code postgres://} URL while the JDBC driver
 * requires the {@code jdbc:postgresql://} prefix; a missing prefix
 * fails at startup with "Driver claims to not accept jdbcUrl".
 * Credentials are moved out of the URL because the driver cannot
 * parse a percent-encoded password.
 */
class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor =
            new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsPostgresSchemeToJdbcUrl() {
        ConfigurableEnvironment environment = runProcessor(
                "postgres://user:pass@db:5432/app");

        assertThat(url(environment))
                .isEqualTo("jdbc:postgresql://db:5432/app");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("pass");
    }

    @Test
    void decodesCredentialsAndStripsThemFromTheUrl() {
        ConfigurableEnvironment environment = runProcessor(
                "postgresql://user:p%40ss%3Aword@db:5432/app");

        assertThat(url(environment))
                .isEqualTo("jdbc:postgresql://db:5432/app");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("p@ss:word");
    }

    @Test
    void preservesUrlWithoutCredentials() {
        ConfigurableEnvironment environment = runProcessor(
                "postgresql://db:5432/app");

        assertThat(url(environment))
                .isEqualTo("jdbc:postgresql://db:5432/app");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isNull();
    }

    @Test
    void leavesJdbcUrlsUntouched() {
        ConfigurableEnvironment environment = runProcessor(
                "jdbc:postgresql://user:pass@db:5432/app");

        assertThat(url(environment))
                .isEqualTo("jdbc:postgresql://db:5432/app");
    }

    @Test
    void ignoresBlankDatabaseUrl() {
        MockEnvironment environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }

    private ConfigurableEnvironment runProcessor(String databaseUrl) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", databaseUrl);
        processor.postProcessEnvironment(environment, mock(SpringApplication.class));
        return environment;
    }

    private static String url(ConfigurableEnvironment environment) {
        PropertySource<?> source = environment.getPropertySources().stream()
                .filter(candidate -> candidate.getName().equals("databaseUrl"))
                .findFirst()
                .orElseThrow();
        return ((MapPropertySource) source).getProperty("spring.datasource.url")
                .toString();
    }
}
