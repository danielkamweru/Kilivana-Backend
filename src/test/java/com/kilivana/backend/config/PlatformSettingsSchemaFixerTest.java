package com.kilivana.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlatformSettingsSchemaFixerTest {

    @Mock
    private JdbcTemplate jdbc;

    @Test
    void backfillsNullsBeforeEnforcingTheConstraintAndIsSafeToRerun() {
        PlatformSettingsSchemaFixer fixer = new PlatformSettingsSchemaFixer(jdbc);
        DefaultApplicationArguments args = new DefaultApplicationArguments();

        fixer.run(args);
        fixer.run(args);

        InOrder calls = inOrder(jdbc);
        calls.verify(jdbc, times(1)).update(contains("WHERE withdrawal_approval_required IS NULL"));
        calls.verify(jdbc, times(1)).execute(contains("ALTER COLUMN withdrawal_approval_required SET NOT NULL"));
        calls.verify(jdbc, times(1)).update(contains("WHERE withdrawal_approval_required IS NULL"));
        calls.verify(jdbc, times(1)).execute(contains("ALTER COLUMN withdrawal_approval_required SET NOT NULL"));
    }
}
