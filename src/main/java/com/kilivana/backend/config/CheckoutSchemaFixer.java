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

        // Multi-farmer fulfilment and shipment tables (V35).
        executeSafely("CREATE TABLE IF NOT EXISTS public.seller_fulfilments (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "order_id BIGINT NOT NULL, " +
                "seller_id BIGINT NOT NULL, " +
                "seller_subtotal NUMERIC(12,2) NOT NULL DEFAULT 0, " +
                "status VARCHAR(32) NOT NULL DEFAULT 'pending', " +
                "notes TEXT, " +
                "ready_at TIMESTAMP, " +
                "collected_at TIMESTAMP, " +
                "created_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "updated_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "CONSTRAINT fk_seller_fulfilment_order FOREIGN KEY (order_id) REFERENCES public.orders (id) ON DELETE CASCADE, " +
                "CONSTRAINT uk_seller_fulfilment_order_seller UNIQUE (order_id, seller_id))",
                "Ensured seller_fulfilments table exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_seller_fulfilment_order ON public.seller_fulfilments (order_id)",
                "Ensured idx_seller_fulfilment_order index exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_seller_fulfilment_seller ON public.seller_fulfilments (seller_id)",
                "Ensured idx_seller_fulfilment_seller index exists");

        executeSafely("CREATE TABLE IF NOT EXISTS public.shipments (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "order_id BIGINT NOT NULL, " +
                "code VARCHAR(32) NOT NULL UNIQUE, " +
                "status VARCHAR(32) NOT NULL DEFAULT 'pending', " +
                "driver_id BIGINT, " +
                "vehicle_type VARCHAR(32), " +
                "vehicle_number VARCHAR(32), " +
                "estimated_weight_kg DOUBLE PRECISION, " +
                "estimated_volume_m3 DOUBLE PRECISION, " +
                "driver_instructions TEXT, " +
                "distance_km DOUBLE PRECISION, " +
                "estimated_minutes INTEGER, " +
                "assigned_at TIMESTAMP, " +
                "accepted_at TIMESTAMP, " +
                "started_at TIMESTAMP, " +
                "completed_at TIMESTAMP, " +
                "cancelled_at TIMESTAMP, " +
                "cancellation_reason TEXT, " +
                "created_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "updated_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES public.orders (id) ON DELETE CASCADE)",
                "Ensured shipments table exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_shipment_order ON public.shipments (order_id)",
                "Ensured idx_shipment_order index exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_shipment_driver ON public.shipments (driver_id)",
                "Ensured idx_shipment_driver index exists");

        executeSafely("CREATE TABLE IF NOT EXISTS public.shipment_items (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "shipment_id BIGINT NOT NULL, " +
                "order_item_id BIGINT NOT NULL, " +
                "allocated_quantity INTEGER NOT NULL, " +
                "unit_price NUMERIC(12,2) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "CONSTRAINT fk_shipment_item_shipment FOREIGN KEY (shipment_id) REFERENCES public.shipments (id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_shipment_item_order_item FOREIGN KEY (order_item_id) REFERENCES public.order_items (id) ON DELETE CASCADE)",
                "Ensured shipment_items table exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_shipment_item_shipment ON public.shipment_items (shipment_id)",
                "Ensured idx_shipment_item_shipment index exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_shipment_item_order_item ON public.shipment_items (order_item_id)",
                "Ensured idx_shipment_item_order_item index exists");

        executeSafely("CREATE TABLE IF NOT EXISTS public.shipment_stops (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "shipment_id BIGINT NOT NULL, " +
                "stop_type VARCHAR(16) NOT NULL, " +
                "party_id BIGINT, " +
                "address TEXT NOT NULL, " +
                "latitude DOUBLE PRECISION, " +
                "longitude DOUBLE PRECISION, " +
                "stop_sequence INTEGER NOT NULL, " +
                "status VARCHAR(16) NOT NULL DEFAULT 'pending', " +
                "instructions TEXT, " +
                "arrived_at TIMESTAMP, " +
                "completed_at TIMESTAMP, " +
                "created_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "updated_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "CONSTRAINT fk_shipment_stop_shipment FOREIGN KEY (shipment_id) REFERENCES public.shipments (id) ON DELETE CASCADE)",
                "Ensured shipment_stops table exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_shipment_stop_shipment ON public.shipment_stops (shipment_id)",
                "Ensured idx_shipment_stop_shipment index exists");
        executeSafely("CREATE INDEX IF NOT EXISTS idx_shipment_stop_sequence ON public.shipment_stops (shipment_id, stop_sequence)",
                "Ensured idx_shipment_stop_sequence index exists");
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
