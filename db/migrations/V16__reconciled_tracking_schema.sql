-- REVIEW PROPOSAL ONLY. Not executed. Use the reviewed single-transaction psql driver.
-- Do not execute historical V14/V15 in addition to this local-baseline plan.
DO $$ BEGIN
    IF current_setting('kilivana.schema_review_run', true) IS DISTINCT FROM '8f9dded-forward-v1' THEN
        RAISE EXCEPTION 'Use apply-reviewed-schema.psql after explicit approval';
    END IF;
END $$;

-- LocalDateTime mappings require timestamp WITHOUT time zone (unlike historical V14).
-- Both tables are new and empty. No existing timestamps are converted or inferred.
CREATE TABLE public.driver_locations (
    id BIGSERIAL PRIMARY KEY,
    logistics_job_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION,
    bearing DOUBLE PRECISION,
    accuracy_metres DOUBLE PRECISION,
    client_timestamp TIMESTAMP(6) WITHOUT TIME ZONE,
    recorded_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX idx_driver_locations_job_recorded
    ON public.driver_locations (logistics_job_id, recorded_at);
CREATE INDEX idx_driver_locations_driver_recorded
    ON public.driver_locations (driver_id, recorded_at);

CREATE TABLE public.driver_latest_location (
    logistics_job_id BIGINT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION,
    bearing DOUBLE PRECISION,
    accuracy_metres DOUBLE PRECISION,
    location_id BIGINT,
    recorded_at TIMESTAMP(6) WITHOUT TIME ZONE,
    client_timestamp TIMESTAMP(6) WITHOUT TIME ZONE
);
-- Canonical entities use scalar IDs here: no new FK, cascade, geographic check,
-- business rule, tracking backfill, or timestamp default is invented.
