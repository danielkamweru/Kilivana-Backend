package com.kilivana.backend.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies the payment-first checkout schema changes automatically on startup.
 *
 * <p>The SQL files under {@code db/migrations/} are not run automatically by the
 * application, so this component ensures the {@code payments} table allows a
 * {@code NULL} {@code order_id} (needed for pre-order payments created before
 * the order exists) and adds the {@code quote_id} column used to link a
 * pre-order payment to its checkout quote.
 *
 * <p>All statements are idempotent and safe to re-run on every startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSchemaFixer {

    private final DataSource dataSource;

    @PostConstruct
    public void fix() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Allow a pre-order payment to exist without an order yet.
        jdbc.execute("ALTER TABLE public.payments ALTER COLUMN order_id DROP NOT NULL");
        log.info("Ensured payments.order_id allows NULL");

        // Link a pre-order payment to its checkout quote.
        jdbc.execute("ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS quote_id VARCHAR(36)");
        log.info("Ensured payments.quote_id column exists");

        // Index for callback and timeout reconciliation by checkout quote.
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_payments_quote_id " +
                "ON public.payments (quote_id) WHERE quote_id IS NOT NULL");
        log.info("Ensured idx_payments_quote_id index exists");
    }
}
