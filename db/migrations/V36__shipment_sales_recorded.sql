-- V36: Add sales_recorded column to shipments table.
-- Tracks whether sales have been recorded for a shipment's items to prevent duplicate recording.

ALTER TABLE public.shipments
    ADD COLUMN IF NOT EXISTS sales_recorded BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.shipments.sales_recorded IS 'Whether sales have been recorded for this shipment''s items';