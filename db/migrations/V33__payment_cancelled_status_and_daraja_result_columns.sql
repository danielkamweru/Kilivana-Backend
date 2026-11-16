-- V33: Add CANCELLED to payment status constraints and add missing Daraja result columns.
--
-- Root cause of payments remaining PENDING:
--   1. payments.status_check and orders.payment_status_check did not include 'CANCELLED'.
--      The DarajaCallbackService writes CANCELLED for customer-cancelled STK Pushes.
--      PostgreSQL rejected the INSERT/UPDATE with a check-constraint violation.
--      The exception was caught and swallowed, leaving callbackStatus='FAILED' and
--      payment status still PENDING.
--   2. daraja_result_code and daraja_result_desc columns were never added by a migration;
--      Hibernate ddl-auto=update may have added them on some instances but not others.
--
-- This migration:
--   - Widens payments.status_check to include CANCELLED.
--   - Widens orders.payment_status_check to include CANCELLED.
--   - Adds daraja_result_code and daraja_result_desc columns if missing.
--   - No row is rewritten; every previously valid value stays valid.

-- 1. Widen payments status check to include CANCELLED
ALTER TABLE public.payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE public.payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING','PAID','HELD','SETTLED','FAILED','CANCELLED','REFUNDED'));

-- 2. Widen orders payment_status check to include CANCELLED
ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;
ALTER TABLE public.orders ADD CONSTRAINT orders_payment_status_check
    CHECK (payment_status IN ('PENDING','PAID','HELD','SETTLED','FAILED','CANCELLED','REFUNDED'));

-- 3. Add Daraja result columns if they do not already exist
ALTER TABLE public.payments
    ADD COLUMN IF NOT EXISTS daraja_result_code INTEGER,
    ADD COLUMN IF NOT EXISTS daraja_result_desc  TEXT;

-- 4. Ensure the callback-status lookup index exists
CREATE INDEX IF NOT EXISTS idx_payments_callback_status
    ON public.payments (callback_status)
    WHERE callback_status IS NOT NULL;
