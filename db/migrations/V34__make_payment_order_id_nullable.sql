-- V34: Make payments.order_id nullable and add quote_id for payment-first checkout.
--
-- Root cause:
--   The checkout flow now initiates the M-PESA STK Push against a checkout quote
--   and creates the order only after the Daraja callback reports success. During
--   that window the payment row exists with order_id = NULL and quote_id set.
--   PostgreSQL rejected the INSERT with a NOT NULL violation on
--   payments.order_id, surfacing a raw database error at checkout.
--
-- This migration:
--   - Drops NOT NULL from payments.order_id so a pre-order payment can be stored
--     before an order exists.
--   - Adds payments.quote_id (VARCHAR(36)) linking the pre-order payment to its
--     checkout quote, plus an index for callback reconciliation lookups.
--   - No rows are rewritten; every previously valid value stays valid.

-- 1. Allow a pre-order payment to have no order yet.
ALTER TABLE public.payments ALTER COLUMN order_id DROP NOT NULL;

-- 2. Link a pre-order payment to its checkout quote.
ALTER TABLE public.payments
    ADD COLUMN IF NOT EXISTS quote_id VARCHAR(36);

-- 3. Index for looking up a payment by its checkout quote during callback
--    processing and timeout reconciliation.
CREATE INDEX IF NOT EXISTS idx_payments_quote_id
    ON public.payments (quote_id)
    WHERE quote_id IS NOT NULL;
