-- REVIEW PROPOSAL ONLY. Not executed. Use the reviewed single-transaction psql driver.
-- Do not execute historical V14/V15 in addition to this local-baseline plan.
DO $$ BEGIN
    IF current_setting('kilivana.schema_review_run', true) IS DISTINCT FROM '8f9dded-forward-v1' THEN
        RAISE EXCEPTION 'Use apply-reviewed-schema.psql after explicit approval';
    END IF;
END $$;

-- Match current unconstrained Java String mappings (255), not V15's narrower lengths.
-- All additions are nullable, without defaults: existing rows receive only NULL.
ALTER TABLE public.payments
    ADD COLUMN merchant_request_id VARCHAR(255),
    ADD COLUMN checkout_request_id VARCHAR(255),
    ADD COLUMN phone_number VARCHAR(255),
    ADD COLUMN mpesa_receipt_number VARCHAR(255),
    ADD COLUMN transaction_date TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN callback_status VARCHAR(255);
CREATE INDEX idx_payments_checkout_request_id ON public.payments (checkout_request_id);
CREATE INDEX idx_payments_merchant_request_id ON public.payments (merchant_request_id);
CREATE INDEX idx_payments_phone_number ON public.payments (phone_number);
CREATE UNIQUE INDEX uk_payments_mpesa_receipt ON public.payments (mpesa_receipt_number)
    WHERE mpesa_receipt_number IS NOT NULL;
-- These four indexes preserve the intent of upstream V15.
-- Do not label historical payments as PENDING callbacks.

ALTER TABLE public.products
    ADD COLUMN farm_id BIGINT,
    ADD COLUMN harvest_date DATE,
    ADD COLUMN last_inspection_id BIGINT,
    ADD COLUMN low_stock_threshold INTEGER,
    ADD COLUMN quality_grade VARCHAR(255),
    ADD COLUMN status_changed_at TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN status_changed_by BIGINT,
    ADD COLUMN status_reason TEXT,
    ADD COLUMN verified_at TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN verified_by BIGINT,
    ADD COLUMN verified_qty INTEGER;
ALTER TABLE public.products ADD CONSTRAINT products_quality_grade_check
    CHECK (quality_grade IN ('GRADE_A','GRADE_B','GRADE_C','EXPORT','ORGANIC'));
-- NULL passes this enum check. Existing product status, price, stock, seller,
-- image links, verification and moderation remain unchanged.
-- Individual product approval: PENDING TEAM DECISION.
