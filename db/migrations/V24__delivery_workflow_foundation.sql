BEGIN;

CREATE TABLE supplier_warehouses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    county VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    contact_name VARCHAR(255),
    contact_phone VARCHAR(32),
    operating_hours VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX supplier_warehouses_user_active_idx ON supplier_warehouses(user_id, is_active);
CREATE UNIQUE INDEX supplier_warehouses_one_default_idx
    ON supplier_warehouses(user_id) WHERE is_default = TRUE;

ALTER TABLE products ADD COLUMN supplier_warehouse_id BIGINT
    REFERENCES supplier_warehouses(id);
CREATE INDEX products_supplier_warehouse_idx ON products(supplier_warehouse_id);

ALTER TABLE order_items
    ADD COLUMN dispatch_location_id_snapshot BIGINT,
    ADD COLUMN dispatch_location_type_snapshot VARCHAR(32),
    ADD COLUMN dispatch_location_name_snapshot VARCHAR(255),
    ADD COLUMN dispatch_address_snapshot TEXT,
    ADD COLUMN dispatch_county_snapshot VARCHAR(100),
    ADD COLUMN dispatch_latitude_snapshot DOUBLE PRECISION,
    ADD COLUMN dispatch_longitude_snapshot DOUBLE PRECISION;

ALTER TABLE orders ADD COLUMN pickup_station_id BIGINT
    REFERENCES pickup_stations(id);
ALTER TABLE orders
    ADD COLUMN pickup_station_name_snapshot VARCHAR(255),
    ADD COLUMN pickup_station_address_snapshot VARCHAR(500),
    ADD COLUMN pickup_station_latitude_snapshot DOUBLE PRECISION,
    ADD COLUMN pickup_station_longitude_snapshot DOUBLE PRECISION;
CREATE INDEX orders_pickup_station_idx ON orders(pickup_station_id);

ALTER TABLE logistics_jobs
    ADD COLUMN seller_id BIGINT REFERENCES users(id),
    ADD COLUMN buyer_id BIGINT REFERENCES users(id),
    ADD COLUMN pickup_farm_id BIGINT REFERENCES farms(id),
    ADD COLUMN pickup_warehouse_id BIGINT REFERENCES supplier_warehouses(id),
    ADD COLUMN pickup_station_id BIGINT REFERENCES pickup_stations(id),
    ADD COLUMN destination_type VARCHAR(32),
    ADD COLUMN delivery_instructions TEXT,
    ADD COLUMN assigned_at TIMESTAMPTZ,
    ADD COLUMN accepted_at TIMESTAMPTZ,
    ADD COLUMN picked_up_at TIMESTAMPTZ,
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN cancelled_at TIMESTAMPTZ;

CREATE INDEX logistics_jobs_seller_status_idx ON logistics_jobs(seller_id, status);
CREATE INDEX logistics_jobs_buyer_created_idx ON logistics_jobs(buyer_id, created_at DESC);
CREATE UNIQUE INDEX logistics_jobs_order_seller_origin_unique_idx
    ON logistics_jobs(order_id, seller_id,
        COALESCE(pickup_farm_id, 0), COALESCE(pickup_warehouse_id, 0))
    WHERE seller_id IS NOT NULL
      AND status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED');

CREATE TABLE delivery_trips (
    id BIGSERIAL PRIMARY KEY,
    delivery_job_id BIGINT NOT NULL REFERENCES logistics_jobs(id),
    driver_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ,
    picked_up_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_latitude DOUBLE PRECISION CHECK (last_latitude BETWEEN -90 AND 90),
    last_longitude DOUBLE PRECISION CHECK (last_longitude BETWEEN -180 AND 180),
    last_location_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX delivery_trips_job_created_idx ON delivery_trips(delivery_job_id, created_at DESC);
CREATE INDEX delivery_trips_driver_status_idx ON delivery_trips(driver_id, status);
CREATE UNIQUE INDEX delivery_trips_one_active_per_job_idx
    ON delivery_trips(delivery_job_id)
    WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED');

CREATE TABLE delivery_events (
    id BIGSERIAL PRIMARY KEY,
    delivery_job_id BIGINT NOT NULL REFERENCES logistics_jobs(id),
    trip_id BIGINT REFERENCES delivery_trips(id),
    event_type VARCHAR(64) NOT NULL,
    actor_id BIGINT REFERENCES users(id),
    actor_role VARCHAR(32),
    reason TEXT,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX delivery_events_job_created_idx ON delivery_events(delivery_job_id, created_at);

ALTER TABLE driver_locations ADD COLUMN trip_id BIGINT REFERENCES delivery_trips(id);
CREATE INDEX driver_locations_trip_recorded_idx ON driver_locations(trip_id, recorded_at);

COMMIT;
