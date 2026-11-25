-- V37: Platform settings and notification preferences.
-- These tables are also auto-created by Hibernate ddl-auto=update, but this
-- migration ensures the schema is explicit and seeds defaults idempotently.

CREATE TABLE IF NOT EXISTS public.platform_settings (
    id BIGSERIAL PRIMARY KEY,
    transaction_fee NUMERIC(5, 2) NOT NULL DEFAULT 3.00,
    inspection_fee NUMERIC(10, 2) NOT NULL DEFAULT 50.00,
    payout_delay_days INTEGER NOT NULL DEFAULT 3,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(64) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Seed default platform settings (only if none exist)
INSERT INTO public.platform_settings (transaction_fee, inspection_fee, payout_delay_days)
SELECT 3.00, 50.00, 3
WHERE NOT EXISTS (SELECT 1 FROM public.platform_settings);

-- Seed default notification preferences (only if none exist for each event)
INSERT INTO public.notification_preferences (event_key, enabled)
SELECT 'NEW_FARMER_REGISTRATION', true
WHERE NOT EXISTS (SELECT 1 FROM public.notification_preferences WHERE event_key = 'NEW_FARMER_REGISTRATION');

INSERT INTO public.notification_preferences (event_key, enabled)
SELECT 'KYC_SUBMITTED', true
WHERE NOT EXISTS (SELECT 1 FROM public.notification_preferences WHERE event_key = 'KYC_SUBMITTED');

INSERT INTO public.notification_preferences (event_key, enabled)
SELECT 'DISPUTE_OPENED', true
WHERE NOT EXISTS (SELECT 1 FROM public.notification_preferences WHERE event_key = 'DISPUTE_OPENED');

INSERT INTO public.notification_preferences (event_key, enabled)
SELECT 'ORDER_PLACED', false
WHERE NOT EXISTS (SELECT 1 FROM public.notification_preferences WHERE event_key = 'ORDER_PLACED');

INSERT INTO public.notification_preferences (event_key, enabled)
SELECT 'PAYOUT_PROCESSED', true
WHERE NOT EXISTS (SELECT 1 FROM public.notification_preferences WHERE event_key = 'PAYOUT_PROCESSED');