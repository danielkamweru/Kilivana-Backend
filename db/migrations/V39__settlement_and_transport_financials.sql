-- V39: Settlement tracking, transport provider wallets, and extended platform settings.
-- Adds settlement per shipment, transport provider wallet type, and financial settings.

-- 1. Settlement: Tracks financial settlement per shipment
CREATE TABLE IF NOT EXISTS public.settlements (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    shipment_id                 BIGINT NOT NULL UNIQUE,
    order_id                    BIGINT NOT NULL,
    seller_id                   BIGINT,
    driver_id                   BIGINT,
    transport_provider_id       BIGINT,

    -- Status tracking
    status                      VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    -- PENDING_PAYMENT, PENDING_DELIVERY, DELIVERY_CONFIRMED, SETTLEMENT_PROCESSING, SETTLED, HELD, PARTIALLY_REFUNDED, REFUNDED, FAILED

    -- Financial amounts
    product_subtotal            NUMERIC(18,2) NOT NULL DEFAULT 0,
    transport_fee               NUMERIC(18,2) NOT NULL DEFAULT 0,
    inspection_fee              NUMERIC(18,2) NOT NULL DEFAULT 0,
    platform_fee                NUMERIC(18,2) NOT NULL DEFAULT 0,
    seller_net_earnings         NUMERIC(18,2) NOT NULL DEFAULT 0,
    transport_provider_payable  NUMERIC(18,2) NOT NULL DEFAULT 0,
    platform_revenue            NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_collected             NUMERIC(18,2) NOT NULL DEFAULT 0,

    -- Platform fee calculation details
    platform_fee_type           VARCHAR(16) NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE, FIXED
    platform_fee_value          NUMERIC(10,2) NOT NULL DEFAULT 0,
    platform_fee_charged_to     VARCHAR(16) NOT NULL DEFAULT 'SELLER', -- SELLER, BUYER

    -- Transport fee calculation details
    transport_fee_type          VARCHAR(16) NOT NULL DEFAULT 'FIXED', -- FIXED, PER_KM
    transport_fee_per_km        NUMERIC(10,2),
    transport_fee_min           NUMERIC(10,2),
    transport_fee_max           NUMERIC(10,2),

    -- Settlement timestamps
    payment_confirmed_at        TIMESTAMP,
    delivery_confirmed_at       TIMESTAMP,
    settlement_processed_at     TIMESTAMP,
    settled_at                  TIMESTAMP,

    -- References
    payment_id                  BIGINT,
    otp_verification_id         BIGINT, -- Reference to logistics_jobs or delivery_otp_verification
    settlement_reference        VARCHAR(64) NOT NULL UNIQUE,

    -- Audit
    created_by                  BIGINT,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_settlement_shipment
        FOREIGN KEY (shipment_id) REFERENCES public.shipments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_settlement_order
        FOREIGN KEY (order_id) REFERENCES public.orders (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_settlement_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_settlement_driver
        FOREIGN KEY (driver_id) REFERENCES public.users (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_settlement_transport_provider
        FOREIGN KEY (transport_provider_id) REFERENCES public.users (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_settlement_payment
        FOREIGN KEY (payment_id) REFERENCES public.payments (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_settlement_created_by
        FOREIGN KEY (created_by) REFERENCES public.users (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_settlement_shipment ON public.settlements (shipment_id);
CREATE INDEX IF NOT EXISTS idx_settlement_order ON public.settlements (order_id);
CREATE INDEX IF NOT EXISTS idx_settlement_seller ON public.settlements (seller_id);
CREATE INDEX IF NOT EXISTS idx_settlement_transport_provider ON public.settlements (transport_provider_id);
CREATE INDEX IF NOT EXISTS idx_settlement_status ON public.settlements (status);
CREATE INDEX IF NOT EXISTS idx_settlement_reference ON public.settlements (settlement_reference);
CREATE INDEX IF NOT EXISTS idx_settlement_created ON public.settlements (created_at);

-- 2. Settlement Allocations: Detailed breakdown of each settlement
CREATE TABLE IF NOT EXISTS public.settlement_allocations (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    settlement_id               BIGINT NOT NULL,
    allocation_type             VARCHAR(32) NOT NULL, -- SELLER_EARNINGS, TRANSPORT_FEE, INSPECTION_FEE, PLATFORM_COMMISSION, PLATFORM_REVENUE
    amount                      NUMERIC(18,2) NOT NULL,
    recipient_user_id           BIGINT, -- Seller or transport provider
    wallet_transaction_id       BIGINT, -- Reference to wallet_transactions
    ledger_journal_entry_id     BIGINT, -- Reference to journal_entries
    description                 TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_settlement_allocation_settlement
        FOREIGN KEY (settlement_id) REFERENCES public.settlements (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_settlement_allocation_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES public.users (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_settlement_allocation_wallet_txn
        FOREIGN KEY (wallet_transaction_id) REFERENCES public.wallet_transactions (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_settlement_allocation_journal_entry
        FOREIGN KEY (ledger_journal_entry_id) REFERENCES public.journal_entries (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_settlement_allocation_settlement ON public.settlement_allocations (settlement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_allocation_type ON public.settlement_allocations (allocation_type);
CREATE INDEX IF NOT EXISTS idx_settlement_allocation_recipient ON public.settlement_allocations (recipient_user_id);

-- 3. Delivery OTP Verification: Track OTP requests and verifications per shipment
CREATE TABLE IF NOT EXISTS public.delivery_otp_verifications (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    shipment_id                 BIGINT NOT NULL,
    order_id                    BIGINT NOT NULL,
    driver_id                   BIGINT NOT NULL,
    buyer_id                    BIGINT NOT NULL,

    -- OTP details
    otp_hash                    VARCHAR(255) NOT NULL, -- BCrypt hash
    otp_expires_at              TIMESTAMP,
    otp_attempts                INTEGER NOT NULL DEFAULT 0,
    otp_locked_until            TIMESTAMP,
    otp_verified                BOOLEAN NOT NULL DEFAULT false,
    otp_verified_at             TIMESTAMP,

    -- Status
    status                      VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, VERIFIED, EXPIRED, FAILED, USED

    -- Timestamps
    requested_at                TIMESTAMP NOT NULL DEFAULT now(),
    sent_at                     TIMESTAMP,
    verified_at                 TIMESTAMP,
    used_at                     TIMESTAMP,

    -- References
    settlement_id               BIGINT,
    logistics_job_id            BIGINT,

    CONSTRAINT fk_delivery_otp_shipment
        FOREIGN KEY (shipment_id) REFERENCES public.shipments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_otp_order
        FOREIGN KEY (order_id) REFERENCES public.orders (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_otp_driver
        FOREIGN KEY (driver_id) REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_otp_buyer
        FOREIGN KEY (buyer_id) REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_otp_settlement
        FOREIGN KEY (settlement_id) REFERENCES public.settlements (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_delivery_otp_logistics_job
        FOREIGN KEY (logistics_job_id) REFERENCES public.logistics_jobs (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_delivery_otp_shipment ON public.delivery_otp_verifications (shipment_id);
CREATE INDEX IF NOT EXISTS idx_delivery_otp_driver ON public.delivery_otp_verifications (driver_id);
CREATE INDEX IF NOT EXISTS idx_delivery_otp_buyer ON public.delivery_otp_verifications (buyer_id);
CREATE INDEX IF NOT EXISTS idx_delivery_otp_status ON public.delivery_otp_verifications (status);
CREATE INDEX IF NOT EXISTS idx_delivery_otp_settlement ON public.delivery_otp_verifications (settlement_id);

-- 4. Extend Platform Settings with financial configuration
-- Add new columns to platform_settings table
ALTER TABLE public.platform_settings
ADD COLUMN IF NOT EXISTS platform_fee_type VARCHAR(16) NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE, FIXED
ADD COLUMN IF NOT EXISTS platform_fee_value NUMERIC(10,2) NOT NULL DEFAULT 3.00,
ADD COLUMN IF NOT EXISTS platform_fee_charged_to VARCHAR(16) NOT NULL DEFAULT 'SELLER', -- SELLER, BUYER
ADD COLUMN IF NOT EXISTS platform_fee_by_category JSONB, -- {"FARMER": 3.0, "SUPPLIER": 5.0}

ADD COLUMN IF NOT EXISTS transport_fee_type VARCHAR(16) NOT NULL DEFAULT 'FIXED', -- FIXED, PER_KM
ADD COLUMN IF NOT EXISTS transport_fee_default NUMERIC(10,2) NOT NULL DEFAULT 100.00,
ADD COLUMN IF NOT EXISTS transport_fee_per_km NUMERIC(10,2),
ADD COLUMN IF NOT EXISTS transport_fee_min NUMERIC(10,2),
ADD COLUMN IF NOT EXISTS transport_fee_max NUMERIC(10,2),
ADD COLUMN IF NOT EXISTS transport_fee_paid_to_provider BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS transport_fee_per_shipment BOOLEAN NOT NULL DEFAULT true,

ADD COLUMN IF NOT EXISTS min_withdrawal_amount NUMERIC(10,2) NOT NULL DEFAULT 100.00,
ADD COLUMN IF NOT EXISTS max_withdrawal_amount NUMERIC(10,2) NOT NULL DEFAULT 70000.00,
ADD COLUMN IF NOT EXISTS withdrawal_approval_required BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS auto_payout_enabled BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS supported_payout_methods VARCHAR(255) NOT NULL DEFAULT 'MPESA_B2C',
ADD COLUMN IF NOT EXISTS payout_retry_limit INTEGER NOT NULL DEFAULT 3;

-- Update existing transaction_fee to platform_fee_value for backward compatibility
UPDATE public.platform_settings
SET platform_fee_value = transaction_fee
WHERE platform_fee_value = 3.00 AND transaction_fee != 3.00;

-- 5. Extend Seller Wallet to support transport provider type
-- The seller_type column already supports FARMER, SUPPLIER - add TRANSPORT_PROVIDER
-- This is handled in the entity enum, no schema change needed.

-- 6. Add settlement reference to shipments for quick lookup
ALTER TABLE public.shipments
ADD COLUMN IF NOT EXISTS settlement_id BIGINT;

ALTER TABLE public.shipments
ADD CONSTRAINT fk_shipment_settlement
    FOREIGN KEY (settlement_id) REFERENCES public.settlements (id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_shipment_settlement ON public.shipments (settlement_id);

-- 7. Add financial settings to notification preferences JSON
-- This is handled in the application layer, no schema change needed.

-- 8. Seed default transport provider ledger account
INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'TRANSPORT_PAYABLES', NULL, 'Transport Provider Payables', 'Amounts owed to transport providers', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'TRANSPORT_PAYABLES' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'TRANSPORT_REVENUE', NULL, 'Transport Revenue', 'Revenue from transport fees collected from buyers', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'TRANSPORT_REVENUE' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'INSPECTION_FEE_REVENUE', NULL, 'Inspection Fee Revenue', 'Revenue from inspection fees', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'INSPECTION_FEE_REVENUE' AND seller_id IS NULL);