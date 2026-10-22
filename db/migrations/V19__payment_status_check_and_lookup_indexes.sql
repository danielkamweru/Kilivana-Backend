-- Align the payments table with the current PaymentStatus enum.
--
-- Background: the schema was created when the enum still had PROCESSING, COMPLETED and
-- PARTIALLY_REFUNDED, and `spring.jpa.hibernate.ddl-auto=update` does not rewrite an
-- existing CHECK constraint when an enum grows. V18 proposed the same correction for
-- this table but was never executed here, so the database still rejected the values the
-- application writes. Every successful M-PESA callback therefore failed with
-- "violates check constraint payments_status_check" and no payment could ever be
-- recorded as paid.
--
-- This is the payments half of V18, applied on its own so the payment flow works.
-- It only widens the accepted set: every value the old constraint allowed stays
-- valid, and no row is rewritten, backfilled or deleted.
--
-- The lookup indexes are the ones V17 intended and that were never created here.
-- The callback service looks a payment up by checkout request id on every callback
-- Safaricom delivers, and the receipt index keeps one M-PESA receipt from being
-- recorded twice.

ALTER TABLE public.payments DROP CONSTRAINT payments_status_check;
ALTER TABLE public.payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED'));

CREATE INDEX IF NOT EXISTS idx_payments_checkout_request_id
    ON public.payments (checkout_request_id);
CREATE INDEX IF NOT EXISTS idx_payments_merchant_request_id
    ON public.payments (merchant_request_id);
CREATE INDEX IF NOT EXISTS idx_payments_phone_number
    ON public.payments (phone_number);

-- One receipt, one payment. NULL receipts stay legal, so a pending attempt is not
-- blocked by the constraint.
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_mpesa_receipt_number
    ON public.payments (mpesa_receipt_number)
    WHERE mpesa_receipt_number IS NOT NULL;
