-- V35: Multi-farmer order fulfilment and shipment management.
--
-- Adds tables for seller fulfilment records, shipments, shipment items and
-- shipment stops. These operate after the existing order has been created
-- and paid for; they do not modify any existing table or payment flow.
--
-- No existing columns are dropped or altered. Every new column is nullable or
-- has a sensible default so previously deployed rows remain valid.

-- 1. Seller fulfilment: one farmer's portion of a customer order.
CREATE TABLE IF NOT EXISTS public.seller_fulfilments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    seller_id           BIGINT NOT NULL,
    seller_subtotal     NUMERIC(12,2) NOT NULL DEFAULT 0,
    status              VARCHAR(32) NOT NULL DEFAULT 'pending',
    notes               TEXT,
    ready_at            TIMESTAMP,
    collected_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_seller_fulfilment_order
        FOREIGN KEY (order_id) REFERENCES public.orders (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_seller_fulfilment_order_seller
        UNIQUE (order_id, seller_id)
);

CREATE INDEX IF NOT EXISTS idx_seller_fulfilment_order
    ON public.seller_fulfilments (order_id);
CREATE INDEX IF NOT EXISTS idx_seller_fulfilment_seller
    ON public.seller_fulfilments (seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_fulfilment_status
    ON public.seller_fulfilments (status);

-- 2. Shipment: groups one or more farmer pickups and one buyer delivery.
CREATE TABLE IF NOT EXISTS public.shipments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    code                VARCHAR(32) NOT NULL UNIQUE,
    status              VARCHAR(32) NOT NULL DEFAULT 'pending',
    driver_id           BIGINT,
    vehicle_type        VARCHAR(32),
    vehicle_number      VARCHAR(32),
    estimated_weight_kg DOUBLE PRECISION,
    estimated_volume_m3 DOUBLE PRECISION,
    driver_instructions TEXT,
    distance_km         DOUBLE PRECISION,
    estimated_minutes   INTEGER,
    assigned_at         TIMESTAMP,
    accepted_at         TIMESTAMP,
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    cancelled_at        TIMESTAMP,
    cancellation_reason TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_shipment_order
        FOREIGN KEY (order_id) REFERENCES public.orders (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shipment_order
    ON public.shipments (order_id);
CREATE INDEX IF NOT EXISTS idx_shipment_driver
    ON public.shipments (driver_id);
CREATE INDEX IF NOT EXISTS idx_shipment_status
    ON public.shipments (status);

-- 3. Shipment item: links a shipment to an original order item with allocated qty.
CREATE TABLE IF NOT EXISTS public.shipment_items (
    id                 BIGSERIAL PRIMARY KEY,
    shipment_id        BIGINT NOT NULL,
    order_item_id      BIGINT NOT NULL,
    allocated_quantity INTEGER NOT NULL,
    unit_price         NUMERIC(12,2) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_shipment_item_shipment
        FOREIGN KEY (shipment_id) REFERENCES public.shipments (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_shipment_item_order_item
        FOREIGN KEY (order_item_id) REFERENCES public.order_items (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shipment_item_shipment
    ON public.shipment_items (shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_item_order_item
    ON public.shipment_items (order_item_id);

-- 4. Shipment stop: one stop along a shipment route (pickup or delivery).
CREATE TABLE IF NOT EXISTS public.shipment_stops (
    id                BIGSERIAL PRIMARY KEY,
    shipment_id       BIGINT NOT NULL,
    stop_type         VARCHAR(16) NOT NULL,
    party_id          BIGINT,
    address           TEXT NOT NULL,
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    stop_sequence     INTEGER NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'pending',
    instructions      TEXT,
    arrived_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_shipment_stop_shipment
        FOREIGN KEY (shipment_id) REFERENCES public.shipments (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shipment_stop_shipment
    ON public.shipment_stops (shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_stop_sequence
    ON public.shipment_stops (shipment_id, stop_sequence);
