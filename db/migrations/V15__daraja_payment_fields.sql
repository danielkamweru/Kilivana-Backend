-- Daraja (M-PESA STK Push) fields for payment reconciliation
-- Added to track STK Push initiation and callback data for audit and idempotency

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS merchant_request_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS checkout_request_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS mpesa_receipt_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS callback_status VARCHAR(20) DEFAULT 'PENDING';

-- Index for fast callback lookup by checkout request ID
CREATE INDEX IF NOT EXISTS idx_payments_checkout_request_id
    ON payments (checkout_request_id);

-- Index for merchant request ID lookups
CREATE INDEX IF NOT EXISTS idx_payments_merchant_request_id
    ON payments (merchant_request_id);

-- Index for phone number lookups
CREATE INDEX IF NOT EXISTS idx_payments_phone_number
    ON payments (phone_number);

-- Unique constraint on M-PESA receipt number to prevent duplicate successful callbacks
-- Only applies when receipt number is not null (i.e., on successful payments)
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_mpesa_receipt
    ON payments (mpesa_receipt_number)
    WHERE mpesa_receipt_number IS NOT NULL;