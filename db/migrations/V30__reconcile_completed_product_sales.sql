BEGIN;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS sales_recorded BOOLEAN NOT NULL DEFAULT FALSE;

WITH eligible_sales AS (
    SELECT oi.product_id, SUM(oi.quantity)::BIGINT AS quantity
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE o.status IN ('DELIVERED', 'COMPLETED')
      AND o.payment_status IN ('PAID', 'HELD', 'SETTLED')
    GROUP BY oi.product_id
)
UPDATE products p
SET sold_qty = GREATEST(COALESCE(p.sold_qty, 0), eligible_sales.quantity)
FROM eligible_sales
WHERE p.id = eligible_sales.product_id;

UPDATE orders
SET sales_recorded = TRUE
WHERE status IN ('DELIVERED', 'COMPLETED')
  AND payment_status IN ('PAID', 'HELD', 'SETTLED');

COMMIT;
