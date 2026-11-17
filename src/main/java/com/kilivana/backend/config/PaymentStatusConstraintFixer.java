package com.kilivana.backend.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Makes sure the database accepts every PaymentStatus value (including CANCELLED and FAILED)
 * on both the payments and the orders table. The SQL files under db/migrations are not run
 * automatically, so an older database can still carry the narrower check constraints, which
 * makes the Daraja callback fail at commit and leaves the payment stuck on PENDING.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusConstraintFixer {

    private static final String ALLOWED =
            "'PENDING','PAID','HELD','SETTLED','FAILED','CANCELLED','REFUNDED'";

    private final DataSource dataSource;

    @PostConstruct
    public void fix() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        widen(jdbc, "payments", "payments_status_check", "status");
        widen(jdbc, "orders", "orders_payment_status_check", "payment_status");
    }

    /**
     * One ALTER TABLE statement, so the old rule is only removed if the new one is accepted.
     * NOT VALID enforces the rule for new and updated rows without re-checking old rows,
     * so a stale legacy value in an old row cannot block start-up.
     */
    private void widen(JdbcTemplate jdbc, String table, String constraint, String column) {
        try {
            jdbc.execute("ALTER TABLE " + table
                    + " DROP CONSTRAINT IF EXISTS " + constraint
                    + ", ADD CONSTRAINT " + constraint
                    + " CHECK (" + column + " IN (" + ALLOWED + ")) NOT VALID");
            log.info("Ensured {} allows PENDING, PAID, HELD, SETTLED, FAILED, CANCELLED, REFUNDED", constraint);
        } catch (Exception e) {
            log.warn("Could not update {} on table {}: {}", constraint, table, e.getMessage());
        }
    }
}
