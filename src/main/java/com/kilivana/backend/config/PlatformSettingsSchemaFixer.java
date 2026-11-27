package com.kilivana.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repairs nullable withdrawal approval values left by older platform settings schemas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformSettingsSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        int repairedRows = jdbc.update("""
                UPDATE public.platform_settings
                SET withdrawal_approval_required = TRUE
                WHERE withdrawal_approval_required IS NULL
                """);

        jdbc.execute("""
                ALTER TABLE public.platform_settings
                    ALTER COLUMN withdrawal_approval_required SET DEFAULT TRUE,
                    ALTER COLUMN withdrawal_approval_required SET NOT NULL
                """);

        log.info("Ensured platform_settings.withdrawal_approval_required is non-null; repaired {} row(s)",
                repairedRows);
    }
}
