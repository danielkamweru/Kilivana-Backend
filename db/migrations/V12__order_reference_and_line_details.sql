-- Orders carry the panel-facing reference (e.g. ORD-2851), and each
-- order line keeps the name and unit the product had at purchase, so an
-- order keeps reading correctly after a product is renamed or removed.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS code VARCHAR(32);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_code ON orders (code);

-- Backfill the reference for orders that already exist, so the column
-- is never null once it is read back.
UPDATE orders
SET code = 'ORD-' || LPAD(id::text, 5, '0')
WHERE code IS NULL;

ALTER TABLE orders ALTER COLUMN code SET NOT NULL;

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS unit VARCHAR(64);
