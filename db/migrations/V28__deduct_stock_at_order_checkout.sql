BEGIN;

DO $$
DECLARE
    invalid_products BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO invalid_products
    FROM products p
    LEFT JOIN (
        SELECT product_id, SUM(quantity) AS quantity
        FROM cart_items
        WHERE stock_reserved = TRUE
        GROUP BY product_id
    ) cart_holds ON cart_holds.product_id = p.id
    WHERE p.reserved_qty < COALESCE(cart_holds.quantity, 0)
       OR p.stock_qty < p.reserved_qty - COALESCE(cart_holds.quantity, 0);

    IF invalid_products > 0 THEN
        RAISE EXCEPTION 'Cannot migrate inventory: % products have inconsistent stock reservations',
            invalid_products;
    END IF;
END $$;

-- Undo cart reservations from V27. Cart items must not reduce purchasable stock.
UPDATE products p
SET reserved_qty = p.reserved_qty - COALESCE((
    SELECT SUM(ci.quantity)
    FROM cart_items ci
    WHERE ci.product_id = p.id
      AND ci.stock_reserved = TRUE
), 0);

-- Existing open orders already consumed stock at checkout under the new model.
UPDATE products
SET stock_qty = stock_qty - reserved_qty,
    reserved_qty = 0
WHERE reserved_qty > 0;

ALTER TABLE cart_items
    DROP COLUMN stock_reserved;

COMMIT;
