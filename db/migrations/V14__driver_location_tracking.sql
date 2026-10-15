-- Driver location fixes: the high-frequency GPS stream for active deliveries.
-- Append-only. Retention is handled by TrackingRetentionService, which prunes rows
-- older than TRACKING_HISTORY_RETENTION_DAYS; the delivery record itself is never deleted.

CREATE TABLE IF NOT EXISTS driver_locations (
    id                  BIGSERIAL PRIMARY KEY,
    logistics_job_id    BIGINT       NOT NULL,
    driver_id           BIGINT       NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    speed_kmh           DOUBLE PRECISION,
    bearing             DOUBLE PRECISION,
    accuracy_metres     DOUBLE PRECISION,
    client_timestamp    TIMESTAMPTZ,
    recorded_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_driver_locations_job_recorded
    ON driver_locations (logistics_job_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_driver_locations_driver_recorded
    ON driver_locations (driver_id, recorded_at);

-- One row per delivery job: the driver's most recent fix, so a "where is the driver now"
-- read is a single row lookup instead of a history scan. A denormalised cache of the
-- latest driver_locations row, kept in sync inside the same transaction.
CREATE TABLE IF NOT EXISTS driver_latest_location (
    logistics_job_id   BIGINT       PRIMARY KEY,
    driver_id          BIGINT       NOT NULL,
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    speed_kmh          DOUBLE PRECISION,
    bearing            DOUBLE PRECISION,
    accuracy_metres    DOUBLE PRECISION,
    location_id        BIGINT,
    recorded_at        TIMESTAMPTZ,
    client_timestamp   TIMESTAMPTZ
);