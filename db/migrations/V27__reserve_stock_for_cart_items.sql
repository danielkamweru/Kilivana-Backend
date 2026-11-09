BEGIN;

ALTER TABLE cart_items
    ADD COLUMN stock_reserved BOOLEAN NOT NULL DEFAULT FALSE;

-- Reserve legacy active/checkout cart lines only when the full line fits in
-- the remaining stock after existing order reservations.
WITH ranked_cart_items AS (
    SELECT ci.id,
           ci.product_id,
           ci.quantity,
           p.stock_qty - p.reserved_qty AS available_stock,
           SUM(ci.quantity) OVER (
               PARTITION BY ci.product_id
               ORDER BY ci.id
               ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS cumulative_cart_quantity
    FROM cart_items ci
    JOIN carts c ON c.id = ci.cart_id
    JOIN products p ON p.id = ci.product_id
    WHERE c.status IN ('ACTIVE', 'CHECKOUT_PROCESSING')
),
reservable_cart_items AS (
    SELECT id
    FROM ranked_cart_items
    WHERE cumulative_cart_quantity <= available_stock
)
UPDATE cart_items ci
SET stock_reserved = TRUE
FROM reservable_cart_items rci
WHERE ci.id = rci.id;

UPDATE products p
SET reserved_qty = p.reserved_qty + reserved_cart_items.quantity
FROM (
    SELECT ci.product_id, SUM(ci.quantity) AS quantity
    FROM cart_items ci
    WHERE ci.stock_reserved = TRUE
    GROUP BY ci.product_id
) reserved_cart_items
WHERE p.id = reserved_cart_items.product_id;

COMMIT;
