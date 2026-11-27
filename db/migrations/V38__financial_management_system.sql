-- V38: Financial management system tables.
-- Creates tables for seller wallets, payout accounts, payouts, double-entry ledger,
-- refund transactions, and financial audit logs.

-- 1. Seller Wallets: One per eligible seller (farmer/supplier)
CREATE TABLE IF NOT EXISTS public.seller_wallets (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    seller_id           BIGINT NOT NULL UNIQUE,
    seller_type         VARCHAR(32) NOT NULL,
    pending_balance     NUMERIC(18,2) NOT NULL DEFAULT 0,
    available_balance   NUMERIC(18,2) NOT NULL DEFAULT 0,
    processing_balance  NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_paid          NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_earned        NUMERIC(18,2) NOT NULL DEFAULT 0,
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_seller_wallet_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_seller_wallet_seller ON public.seller_wallets (seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_wallet_status ON public.seller_wallets (status);

-- 2. Wallet Transactions: Immutable record of every wallet balance change
CREATE TABLE IF NOT EXISTS public.wallet_transactions (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    wallet_id                   BIGINT NOT NULL,
    seller_id                   BIGINT NOT NULL,
    order_id                    BIGINT,
    seller_fulfilment_id        BIGINT,
    transaction_type            VARCHAR(32) NOT NULL,
    amount                      NUMERIC(18,2) NOT NULL,
    status                      VARCHAR(32) NOT NULL,
    reference                   VARCHAR(64) NOT NULL UNIQUE,
    description                 TEXT,
    balance_after               NUMERIC(18,2),
    pending_balance_after       NUMERIC(18,2),
    available_balance_after     NUMERIC(18,2),
    processing_balance_after    NUMERIC(18,2),
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_txn_wallet
        FOREIGN KEY (wallet_id) REFERENCES public.seller_wallets (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_txn_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_txn_order
        FOREIGN KEY (order_id) REFERENCES public.orders (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_wallet_txn_fulfilment
        FOREIGN KEY (seller_fulfilment_id) REFERENCES public.seller_fulfilments (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_wallet_txn_wallet ON public.wallet_transactions (wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_seller ON public.wallet_transactions (seller_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_type ON public.wallet_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_status ON public.wallet_transactions (status);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_reference ON public.wallet_transactions (reference);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_order ON public.wallet_transactions (order_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_fulfilment ON public.wallet_transactions (seller_fulfilment_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_created ON public.wallet_transactions (created_at);

-- 3. Seller Payout Accounts: Registered payout destinations for sellers
CREATE TABLE IF NOT EXISTS public.seller_payout_accounts (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    seller_id               BIGINT NOT NULL,
    payout_method           VARCHAR(32) NOT NULL,
    account_name            VARCHAR(255) NOT NULL,
    mpesa_phone             VARCHAR(15),
    bank_name               VARCHAR(100),
    bank_account_number     VARCHAR(50),
    bank_branch_code        VARCHAR(20),
    is_default              BOOLEAN NOT NULL DEFAULT false,
    verification_status     VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    verified_at             TIMESTAMP,
    verification_reference  VARCHAR(64),
    notes                   TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_payout_account_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payout_account_seller ON public.seller_payout_accounts (seller_id);
CREATE INDEX IF NOT EXISTS idx_payout_account_default ON public.seller_payout_accounts (seller_id, is_default);
CREATE INDEX IF NOT EXISTS idx_payout_account_verified ON public.seller_payout_accounts (verification_status);

-- 4. Seller Payouts: Withdrawal requests with admin approval and provider processing
CREATE TABLE IF NOT EXISTS public.seller_payouts (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    seller_id               BIGINT NOT NULL,
    wallet_id               BIGINT NOT NULL,
    payout_account_id       BIGINT NOT NULL,
    amount                  NUMERIC(18,2) NOT NULL,
    payout_method           VARCHAR(32) NOT NULL,
    destination_snapshot    TEXT,
    status                  VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reference               VARCHAR(64) NOT NULL UNIQUE,
    approved_by             BIGINT,
    approved_at             TIMESTAMP,
    rejection_reason        TEXT,
    provider_reference      VARCHAR(64),
    provider_result_code    INTEGER,
    provider_result_desc    TEXT,
    mpesa_receipt_number    VARCHAR(32),
    paid_at                 TIMESTAMP,
    attempt_count           INTEGER NOT NULL DEFAULT 0,
    last_attempt_at         TIMESTAMP,
    idempotency_key         VARCHAR(64),
    notes                   TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_payout_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_payout_wallet
        FOREIGN KEY (wallet_id) REFERENCES public.seller_wallets (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_payout_account
        FOREIGN KEY (payout_account_id) REFERENCES public.seller_payout_accounts (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_payout_approved_by
        FOREIGN KEY (approved_by) REFERENCES public.users (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_seller_payout_seller ON public.seller_payouts (seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_payout_status ON public.seller_payouts (status);
CREATE INDEX IF NOT EXISTS idx_seller_payout_wallet ON public.seller_payouts (wallet_id);
CREATE INDEX IF NOT EXISTS idx_seller_payout_account ON public.seller_payouts (payout_account_id);
CREATE INDEX IF NOT EXISTS idx_seller_payout_reference ON public.seller_payouts (reference);
CREATE INDEX IF NOT EXISTS idx_seller_payout_provider_ref ON public.seller_payouts (provider_reference);
CREATE INDEX IF NOT EXISTS idx_seller_payout_created ON public.seller_payouts (created_at);

-- 5. Ledger Accounts: Chart of accounts for double-entry bookkeeping
CREATE TABLE IF NOT EXISTS public.ledger_accounts (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    account_type        VARCHAR(64) NOT NULL,
    seller_id           BIGINT,
    name                VARCHAR(255) NOT NULL,
    balance             NUMERIC(18,2) NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT true,
    description         TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_ledger_account_type_seller UNIQUE (account_type, seller_id),
    CONSTRAINT fk_ledger_account_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ledger_account_type ON public.ledger_accounts (account_type);
CREATE INDEX IF NOT EXISTS idx_ledger_account_seller ON public.ledger_accounts (seller_id);
CREATE INDEX IF NOT EXISTS idx_ledger_account_active ON public.ledger_accounts (active);

-- 6. Journal Entries: Balanced financial transactions
CREATE TABLE IF NOT EXISTS public.journal_entries (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    reference           VARCHAR(64) NOT NULL UNIQUE,
    entry_date          TIMESTAMP NOT NULL,
    description         TEXT NOT NULL,
    source_type         VARCHAR(32) NOT NULL,
    source_id           BIGINT,
    created_by          BIGINT NOT NULL,
    posted              BOOLEAN NOT NULL DEFAULT false,
    posted_at           TIMESTAMP,
    reversal_of         BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_journal_entry_created_by
        FOREIGN KEY (created_by) REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_journal_entry_reversal
        FOREIGN KEY (reversal_of) REFERENCES public.journal_entries (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_journal_entry_reference ON public.journal_entries (reference);
CREATE INDEX IF NOT EXISTS idx_journal_entry_date ON public.journal_entries (entry_date);
CREATE INDEX IF NOT EXISTS idx_journal_entry_source ON public.journal_entries (source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_journal_entry_posted ON public.journal_entries (posted);

-- 7. Journal Lines: Individual debit/credit lines within a journal entry
CREATE TABLE IF NOT EXISTS public.journal_lines (
    id                  BIGSERIAL PRIMARY KEY,
    journal_entry_id    BIGINT NOT NULL,
    account_id          BIGINT NOT NULL,
    account_type        VARCHAR(64) NOT NULL,
    seller_id           BIGINT,
    debit_amount        NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
    description         TEXT,
    CONSTRAINT fk_journal_line_entry
        FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_journal_line_account
        FOREIGN KEY (account_id) REFERENCES public.ledger_accounts (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_journal_line_amounts CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (debit_amount = 0 AND credit_amount > 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_journal_line_entry ON public.journal_lines (journal_entry_id);
CREATE INDEX IF NOT EXISTS idx_journal_line_account ON public.journal_lines (account_id);
CREATE INDEX IF NOT EXISTS idx_journal_line_seller ON public.journal_lines (seller_id);

-- 8. Refund Transactions: Buyer refunds with seller impact tracking
CREATE TABLE IF NOT EXISTS public.refund_transactions (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    payment_id                  BIGINT NOT NULL,
    order_id                    BIGINT NOT NULL,
    seller_fulfilment_id        BIGINT,
    seller_id                   BIGINT,
    buyer_id                    BIGINT NOT NULL,
    amount                      NUMERIC(18,2) NOT NULL,
    reason                      TEXT,
    status                      VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reference                   VARCHAR(64) NOT NULL UNIQUE,
    provider_reference          VARCHAR(64),
    provider_result_code        INTEGER,
    provider_result_desc        TEXT,
    approved_by                 BIGINT,
    approved_at                 TIMESTAMP,
    rejection_reason            TEXT,
    completed_at                TIMESTAMP,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_id) REFERENCES public.payments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_refund_order
        FOREIGN KEY (order_id) REFERENCES public.orders (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_refund_fulfilment
        FOREIGN KEY (seller_fulfilment_id) REFERENCES public.seller_fulfilments (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_refund_seller
        FOREIGN KEY (seller_id) REFERENCES public.users (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_refund_buyer
        FOREIGN KEY (buyer_id) REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_refund_approved_by
        FOREIGN KEY (approved_by) REFERENCES public.users (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_refund_payment ON public.refund_transactions (payment_id);
CREATE INDEX IF NOT EXISTS idx_refund_order ON public.refund_transactions (order_id);
CREATE INDEX IF NOT EXISTS idx_refund_fulfilment ON public.refund_transactions (seller_fulfilment_id);
CREATE INDEX IF NOT EXISTS idx_refund_seller ON public.refund_transactions (seller_id);
CREATE INDEX IF NOT EXISTS idx_refund_buyer ON public.refund_transactions (buyer_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON public.refund_transactions (status);
CREATE INDEX IF NOT EXISTS idx_refund_reference ON public.refund_transactions (reference);
CREATE INDEX IF NOT EXISTS idx_refund_provider_ref ON public.refund_transactions (provider_reference);
CREATE INDEX IF NOT EXISTS idx_refund_created ON public.refund_transactions (created_at);

-- 9. Financial Audit Logs: Immutable audit trail for sensitive operations
CREATE TABLE IF NOT EXISTS public.financial_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    actor_id        BIGINT,
    action          VARCHAR(64) NOT NULL,
    entity_type     VARCHAR(64) NOT NULL,
    entity_id       BIGINT,
    description     TEXT,
    parameters      JSONB,
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_log_actor
        FOREIGN KEY (actor_id) REFERENCES public.users (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_financial_audit_actor ON public.financial_audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_financial_audit_action ON public.financial_audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_financial_audit_entity ON public.financial_audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_financial_audit_created ON public.financial_audit_logs (created_at);

-- Seed default platform-level ledger accounts
INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'PAYMENT_CLEARING', NULL, 'Payment Clearing', 'Funds received from buyers awaiting settlement by payment provider', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'PAYMENT_CLEARING' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'SETTLED_FUNDS', NULL, 'Settled Funds', 'Funds settled by payment provider and available for disbursement', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'SETTLED_FUNDS' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'SELLER_PAYABLES', NULL, 'Seller Payables (Platform)', 'Platform-level control account for amounts owed to all sellers', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'SELLER_PAYABLES' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'REFUND_PAYABLE', NULL, 'Refund Payable', 'Amounts owed to buyers for refunds', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'REFUND_PAYABLE' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'PLATFORM_COMMISSION_REVENUE', NULL, 'Platform Commission Revenue', 'Commission earned from seller transactions', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'PLATFORM_COMMISSION_REVENUE' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'OTHER_PLATFORM_REVENUE', NULL, 'Other Platform Revenue', 'Other platform revenue (fees, subscriptions, etc.)', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'OTHER_PLATFORM_REVENUE' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'PAYMENT_PROCESSING_EXPENSES', NULL, 'Payment Processing Expenses', 'Fees paid to payment providers', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'PAYMENT_PROCESSING_EXPENSES' AND seller_id IS NULL);

INSERT INTO public.ledger_accounts (account_type, seller_id, name, description, active)
SELECT 'OTHER_PLATFORM_EXPENSES', NULL, 'Other Platform Expenses', 'Other approved platform operating expenses', true
WHERE NOT EXISTS (SELECT 1 FROM public.ledger_accounts WHERE account_type = 'OTHER_PLATFORM_EXPENSES' AND seller_id IS NULL);