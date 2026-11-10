CREATE TABLE IF NOT EXISTS checkout_quotes (
    quote_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    delivery_method VARCHAR(32) NOT NULL,
    address_id BIGINT,
    pickup_station_id BIGINT,
    pickup_location VARCHAR(500),
    subtotal NUMERIC(12, 2) NOT NULL,
    delivery_fee NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    order_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_checkout_quotes_user_id ON checkout_quotes (user_id);
