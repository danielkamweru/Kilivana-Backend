-- Create pickup stations and user saved pickup stations tables
BEGIN;

-- Pickup stations table (platform-managed locations)
CREATE TABLE pickup_stations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    region VARCHAR(100) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    opening_hours VARCHAR(255),
    contact_phone VARCHAR(20),
    pickup_instructions TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for region-based queries
CREATE INDEX pickup_stations_region_idx ON pickup_stations(region);
CREATE INDEX pickup_stations_available_idx ON pickup_stations(is_available);

-- User saved pickup stations table (user's saved stations for quick access)
CREATE TABLE user_pickup_stations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pickup_station_id BIGINT NOT NULL,
    label VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for user lookups
CREATE INDEX user_pickup_stations_user_id_idx ON user_pickup_stations(user_id);
CREATE INDEX user_pickup_stations_user_default_idx ON user_pickup_stations(user_id, is_default);

-- Ensure one default per user
CREATE UNIQUE INDEX user_pickup_stations_user_default_unique_idx
    ON user_pickup_stations(user_id)
    WHERE is_default = TRUE;

-- Ensure user doesn't save the same station twice
CREATE UNIQUE INDEX user_pickup_stations_user_station_unique_idx
    ON user_pickup_stations(user_id, pickup_station_id);

COMMIT;
