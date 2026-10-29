-- Allow checkout orders to use a buyer-entered pickup location instead of a
-- platform pickup station or saved delivery address.
BEGIN;

ALTER TABLE orders ALTER COLUMN address_id DROP NOT NULL;
ALTER TABLE orders ADD COLUMN pickup_location TEXT;

COMMIT;
