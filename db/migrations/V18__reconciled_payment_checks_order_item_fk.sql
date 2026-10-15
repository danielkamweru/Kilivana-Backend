-- REVIEW PROPOSAL ONLY. Not executed. Use the reviewed single-transaction psql driver.
-- Do not execute historical V14/V15 in addition to this local-baseline plan.
DO $$ BEGIN
    IF current_setting('kilivana.schema_review_run', true) IS DISTINCT FROM '8f9dded-forward-v1' THEN
        RAISE EXCEPTION 'Use apply-reviewed-schema.psql after explicit approval';
    END IF;
END $$;

-- Expand existing five-state checks to match current PaymentStatus, including FAILED.
-- Preserve every old value. No UPDATE/backfill or product status transition.
-- The driver verifies the exact pre-migration schema before any DDL.
ALTER TABLE public.orders DROP CONSTRAINT orders_payment_status_check;
ALTER TABLE public.orders ADD CONSTRAINT orders_payment_status_check
    CHECK (payment_status IN ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED'));
ALTER TABLE public.payments DROP CONSTRAINT payments_status_check;
ALTER TABLE public.payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED'));

-- Match OrderItem.order while retaining writable order_id. Existing NOT NULL is retained.
-- Fail rather than fabricating parent orders or deleting orphan lines.
ALTER TABLE public.order_items ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w
    FOREIGN KEY (order_id) REFERENCES public.orders(id)
    ON UPDATE NO ACTION ON DELETE NO ACTION;
-- Ordinary constraint validation is intentional: zero current order lines.
-- Reassess lock/validation strategy for larger environments. Never CASCADE.
