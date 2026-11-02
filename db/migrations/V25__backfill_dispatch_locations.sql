BEGIN;

WITH eligible_supplier_locations AS (
    SELECT p.id AS product_id,
           COALESCE(default_location.id, sole_location.id) AS warehouse_id
    FROM products p
    LEFT JOIN LATERAL (
        SELECT sw.id
        FROM supplier_warehouses sw
        WHERE sw.user_id = p.seller_id
          AND sw.is_active = TRUE
          AND sw.is_default = TRUE
          AND NULLIF(BTRIM(sw.address), '') IS NOT NULL
          AND sw.latitude BETWEEN -90 AND 90
          AND sw.longitude BETWEEN -180 AND 180
        LIMIT 1
    ) default_location ON TRUE
    LEFT JOIN LATERAL (
        SELECT MIN(sw.id) AS id
        FROM supplier_warehouses sw
        WHERE sw.user_id = p.seller_id
          AND sw.is_active = TRUE
          AND NULLIF(BTRIM(sw.address), '') IS NOT NULL
          AND sw.latitude BETWEEN -90 AND 90
          AND sw.longitude BETWEEN -180 AND 180
        HAVING COUNT(*) = 1
    ) sole_location ON TRUE
    WHERE p.seller_type = 'SUPPLIER'
      AND p.supplier_warehouse_id IS NULL
)
UPDATE products p
SET supplier_warehouse_id = eligible.warehouse_id
FROM eligible_supplier_locations eligible
WHERE p.id = eligible.product_id
  AND eligible.warehouse_id IS NOT NULL;

WITH eligible_farmer_locations AS (
    SELECT p.id AS product_id, sole_location.farm_id
    FROM products p
    JOIN LATERAL (
        SELECT MIN(f.id) AS farm_id
        FROM farms f
        WHERE f.farmer_id = p.seller_id
          AND f.status = 'ACTIVE'
          AND NULLIF(BTRIM(f.address), '') IS NOT NULL
          AND f.latitude BETWEEN -90 AND 90
          AND f.longitude BETWEEN -180 AND 180
        HAVING COUNT(*) = 1
    ) sole_location ON TRUE
    WHERE p.seller_type = 'FARMER'
      AND p.farm_id IS NULL
)
UPDATE products p
SET farm_id = eligible.farm_id
FROM eligible_farmer_locations eligible
WHERE p.id = eligible.product_id;

UPDATE order_items oi
SET seller_id = COALESCE(oi.seller_id, p.seller_id),
    dispatch_location_id_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN f.id
        ELSE w.id
    END,
    dispatch_location_type_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN 'FARM'
        ELSE 'WAREHOUSE'
    END,
    dispatch_location_name_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN f.name
        ELSE w.name
    END,
    dispatch_address_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN f.address
        ELSE w.address
    END,
    dispatch_county_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN f.county
        ELSE w.county
    END,
    dispatch_latitude_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN f.latitude
        ELSE w.latitude
    END,
    dispatch_longitude_snapshot = CASE
        WHEN p.seller_type = 'FARMER' THEN f.longitude
        ELSE w.longitude
    END
FROM products p
LEFT JOIN farms f
    ON p.seller_type = 'FARMER'
   AND f.id = p.farm_id
   AND f.farmer_id = p.seller_id
   AND f.status = 'ACTIVE'
   AND NULLIF(BTRIM(f.address), '') IS NOT NULL
   AND f.latitude BETWEEN -90 AND 90
   AND f.longitude BETWEEN -180 AND 180
LEFT JOIN supplier_warehouses w
    ON p.seller_type = 'SUPPLIER'
   AND w.id = p.supplier_warehouse_id
   AND w.user_id = p.seller_id
   AND w.is_active = TRUE
   AND NULLIF(BTRIM(w.address), '') IS NOT NULL
   AND w.latitude BETWEEN -90 AND 90
   AND w.longitude BETWEEN -180 AND 180
WHERE oi.product_id = p.id
  AND (oi.dispatch_location_id_snapshot IS NULL
       OR oi.dispatch_location_type_snapshot IS NULL)
  AND (f.id IS NOT NULL OR w.id IS NOT NULL);

COMMIT;
