package com.kilivana.backend.config;

import jakarta.annotation.PostConstruct;
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
 * <p>All statements are idempotent and safe to re-run on every startup. Each
 * statement is wrapped in try/catch so a failure on one statement does not
 * prevent the application from starting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSchemaFixer {

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void fix() {
        // Allow a pre-order payment to exist without an order yet.
        executeSafely("ALTER TABLE public.payments ALTER COLUMN order_id DROP NOT NULL",
                "Ensured payments.order_id allows NULL");

        // Link a pre-order payment to its checkout quote.
        executeSafely("ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS quote_id VARCHAR(36)",
                "Ensured payments.quote_id column exists");

        // Index for callback and timeout reconciliation by checkout quote.
        executeSafely("CREATE INDEX IF NOT EXISTS idx_payments_quote_id " +
                        "ON public.payments (quote_id) WHERE quote_id IS NOT NULL",
                "Ensured idx_payments_quote_id index exists");
    }

    private void executeSafely(String sql, String description) {
        try {
            jdbc.execute(sql);
            log.info(description);
        } catch (Exception e) {
            log.warn("Could not {}: {}", description, e.getMessage());
        }
    }
}
