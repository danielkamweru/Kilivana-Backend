-- Aligns the stored order states with the contract the admin panel
-- speaks.
--
-- The panel's pipeline is placed -> confirmed -> in_transit ->
-- delivered -> completed, with disputed and cancelled as the two side
-- exits. The three seller-side fulfilment states no backend path ever
-- wrote are folded into in_transit, disputed joins the set, and the
-- constraint is narrowed to the seven states the panel renders. The
-- same seven states apply to the order's timeline events, and the
-- payment union drops FAILED, which nothing ever wrote and the panel
-- has no display for.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

UPDATE orders
SET status = 'IN_TRANSIT'
WHERE status IN ('PREPARING', 'READY_FOR_PICKUP', 'PICKED_UP');

UPDATE orders
SET status = 'CANCELLED'
WHERE status NOT IN ('PLACED', 'CONFIRMED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'DISPUTED', 'CANCELLED');

ALTER TABLE orders
ADD CONSTRAINT orders_status_check
CHECK (status IN ('PLACED', 'CONFIRMED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'DISPUTED', 'CANCELLED'));

-- The order's payment state is the same five-state union as a
-- payment's.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;

UPDATE orders
SET payment_status = 'PENDING'
WHERE payment_status NOT IN ('PENDING', 'PAID', 'HELD', 'SETTLED', 'REFUNDED');

ALTER TABLE orders
ADD CONSTRAINT orders_payment_status_check
CHECK (payment_status IN ('PENDING', 'PAID', 'HELD', 'SETTLED', 'REFUNDED'));

-- Order events record the order's own states, so they carry the
-- same seven-state union as the order.
ALTER TABLE order_events DROP CONSTRAINT IF EXISTS order_events_status_check;

UPDATE order_events
SET status = 'IN_TRANSIT'
WHERE status IN ('PREPARING', 'READY_FOR_PICKUP', 'PICKED_UP');

UPDATE order_events
SET status = 'CANCELLED'
WHERE status NOT IN ('PLACED', 'CONFIRMED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'DISPUTED', 'CANCELLED');

ALTER TABLE order_events
ADD CONSTRAINT order_events_status_check
CHECK (status IN ('PLACED', 'CONFIRMED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'DISPUTED', 'CANCELLED'));

-- Products: a listing is active, pending verification, rejected or
-- suspended. Nothing set INACTIVE (suspension is the panel's word for
-- taking a listing down) and out of stock is a property of the stock
-- count rather than the listing, so both are folded into their nearest
-- survivors.
UPDATE products
SET status = 'SUSPENDED'
WHERE status = 'INACTIVE';

UPDATE products
SET status = 'ACTIVE'
WHERE status = 'OUT_OF_STOCK';

UPDATE products
SET status = 'PENDING_APPROVAL'
WHERE status NOT IN ('ACTIVE', 'PENDING_APPROVAL', 'REJECTED', 'SUSPENDED');

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check;

ALTER TABLE products
ADD CONSTRAINT products_status_check
CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'REJECTED', 'SUSPENDED'));

-- Payments: any straggler from before the five-state union is reset
-- to pending.
UPDATE payments
SET status = 'PENDING'
WHERE status NOT IN ('PENDING', 'PAID', 'HELD', 'SETTLED', 'REFUNDED');

ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;

ALTER TABLE payments
ADD CONSTRAINT payments_status_check
CHECK (status IN ('PENDING', 'PAID', 'HELD', 'SETTLED', 'REFUNDED'));
