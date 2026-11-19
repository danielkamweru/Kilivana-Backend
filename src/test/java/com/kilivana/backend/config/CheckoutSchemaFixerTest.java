package com.kilivana.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckoutSchemaFixerTest {

    @Mock private JdbcTemplate jdbc;

    @InjectMocks
    private CheckoutSchemaFixer fixer;

    @Test
    void appliesAllThreeSchemaStatements() {
        fixer.fix();

        verify(jdbc, times(3)).execute(org.mockito.ArgumentMatchers.anyString());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(3)).execute(captor.capture());
        List<String> statements = captor.getAllValues();

        assertThat(statements).contains(
                "ALTER TABLE public.payments ALTER COLUMN order_id DROP NOT NULL",
                "ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS quote_id VARCHAR(36)",
                "CREATE INDEX IF NOT EXISTS idx_payments_quote_id ON public.payments (quote_id) WHERE quote_id IS NOT NULL"
        );
    }

    @Test
    void doesNotThrowWhenSqlFails() {
        org.mockito.Mockito.doThrow(new RuntimeException("simulated database error"))
                .when(jdbc).execute(org.mockito.ArgumentMatchers.anyString());

        // Should not throw — each statement is try/catch wrapped.
        fixer.fix();

        verify(jdbc, times(3)).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
