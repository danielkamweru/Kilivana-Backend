-- Order status PENDING is now PLACED, and the payment statuses moved with the
-- admin panel's vocabulary (see V9). Both columns carry a CHECK constraint that mirrors
-- the enum, so the constraint has to be widened before the data moves or the update is
-- rejected. Recreating them afterwards is what stops the column drifting from the enum
-- again.
--
-- PLACED rather than pending: the panel labels a freshly created order "Placed", and
-- "pending" left the first column rendering a state it has no label for. It is a rename,
-- so no row changes meaning.
--
-- FAILED is dropped from both order and payment status. Nothing set either and the panel
-- shows no state for them, so no row should hold them; the fallbacks below only exist so
-- the column can never keep a value the enum cannot deserialise.

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;

UPDATE orders SET status = 'PLACED'      WHERE status = 'PENDING';
UPDATE orders SET status = 'CANCELLED'   WHERE status = 'FAILED';

UPDATE orders SET payment_status = 'PENDING'    WHERE payment_status = 'PROCESSING';
UPDATE orders SET payment_status = 'PAID'       WHERE payment_status = 'COMPLETED';
UPDATE orders SET payment_status = 'REFUNDED'   WHERE payment_status = 'PARTIALLY_REFUNDED';
UPDATE orders SET payment_status = 'CANCELLED'  WHERE payment_status = 'FAILED';

UPDATE orders SET status         = 'CANCELLED' WHERE status NOT IN
    ('PLACED','CONFIRMED','PREPARING','READY_FOR_PICKUP','PICKED_UP',
     'IN_TRANSIT','DELIVERED','COMPLETED','CANCELLED');
UPDATE orders SET payment_status = 'PENDING'   WHERE payment_status NOT IN
    ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED');

ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN
    ('PLACED','CONFIRMED','PREPARING','READY_FOR_PICKUP','PICKED_UP',
     'IN_TRANSIT','DELIVERED','COMPLETED','CANCELLED'));

ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check CHECK (payment_status IN
    ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED'));

ALTER TABLE payments ADD CONSTRAINT payments_status_check CHECK (status IN
    ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED'));