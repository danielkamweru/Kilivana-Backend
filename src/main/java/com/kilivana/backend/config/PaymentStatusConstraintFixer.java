package com.kilivana.backend.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusConstraintFixer {

    private final DataSource dataSource;

    @PostConstruct
    public void fix() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            jdbc.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check");
            jdbc.execute("ALTER TABLE payments ADD CONSTRAINT payments_status_check CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED','HELD','SETTLED','REFUNDED'))");
            log.info("Ensured payments_status_check includes CANCELLED");
        } catch (Exception e) {
            log.warn("Could not fix payments_status_check constraint: {}", e.getMessage());
        }
    }
}
