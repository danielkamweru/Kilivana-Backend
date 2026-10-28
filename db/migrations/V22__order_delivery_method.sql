-- Add delivery method column to orders table
BEGIN;

ALTER TABLE orders ADD COLUMN delivery_method VARCHAR(50) NOT NULL DEFAULT 'DOOR_DELIVERY';

-- Add index for delivery method queries
CREATE INDEX orders_delivery_method_idx ON orders(delivery_method);

COMMIT;
