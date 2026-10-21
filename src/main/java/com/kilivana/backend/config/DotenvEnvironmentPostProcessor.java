package com.kilivana.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads a project-local {@code .env} file into the Spring environment.
 *
 * <p>Spring Boot does not read a {@code .env} file by itself. Local development
 * therefore had to export variables by hand, and anything that regenerated the
 * example file or reset the working tree silently emptied the local secrets.
 * This post-processor reads {@code .env} from the working directory (or from the
 * {@code ENV_FILE} override) and exposes its entries as a property source.
 *
 * <p><b>Precedence is the point:</b> the source is added with
 * {@link org.springframework.core.env.EnvironmentPostProcessor} ordering that
 * places it <em>below</em> {@code application.properties}, command-line
 * arguments and the real OS environment. A value set in the live environment or
 * on the command line always wins, so a developer who exports
 * {@code DARAJA_CONSUMER_KEY=...} for one run cannot be surprised by a stale
 * value in the file. The file is a fallback, never an override.
 *
 * <p>Malformed lines and unreadable files are skipped; the application fails
 * later on the specific missing property rather than at startup.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Lowest precedence among post-processors so it cannot shadow real config. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path file = resolveEnvFile();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        try (InputStream in = Files.newInputStream(file)) {
            Properties props = new Properties();
            props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String key = String.valueOf(entry.getKey()).trim();
                String value = String.valueOf(entry.getValue());
                if (key.isEmpty()) {
                    continue;
                }
                properties.put(key, value);
            }
        } catch (IOException e) {
            // Never fatal: a broken .env must not prevent the app from starting
            // with real environment variables supplied another way.
            return;
        }

        if (properties.isEmpty()) {
            return;
        }

        // Insert at the back so it loses to application.properties, command-line
        // args, the OS environment and the DATABASE_URL post-processor.
        environment.getPropertySources()
                .addLast(new MapPropertySource("dotenv", properties));
    }

    /** Resolves the .env file: explicit override, then the working directory. */
    protected Path resolveEnvFile() {
        String override = System.getenv("ENV_FILE");
        if (override != null && !override.isBlank()) {
            Path p = Paths.get(override);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return Paths.get(".env").toAbsolutePath().normalize();
    }
}