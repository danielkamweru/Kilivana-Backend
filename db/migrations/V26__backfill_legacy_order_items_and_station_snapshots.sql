BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- V26: Backfill dispatch snapshots for legacy order_items and pickup-station
--      snapshots for legacy orders.
--
-- Safe to re-run: every UPDATE is guarded by a WHERE that only touches rows
-- that are still missing the target value.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Backfill order_items whose product is a FARMER product with a linked farm.
UPDATE order_items oi
SET dispatch_location_id_snapshot   = f.id,
    dispatch_location_type_snapshot = 'FARM',
    dispatch_location_name_snapshot = f.name,
    dispatch_address_snapshot       = f.address,
    dispatch_county_snapshot        = f.county,
    dispatch_latitude_snapshot      = f.latitude,
    dispatch_longitude_snapshot     = f.longitude
FROM products p
JOIN farms f
    ON f.id = p.farm_id
   AND f.farmer_id = p.seller_id
   AND f.status = 'ACTIVE'
   AND NULLIF(BTRIM(f.address), '') IS NOT NULL
   AND f.latitude  BETWEEN -90  AND 90
   AND f.longitude BETWEEN -180 AND 180
WHERE oi.product_id = p.id
  AND p.seller_type = 'FARMER'
  AND (oi.dispatch_location_id_snapshot IS NULL
       OR oi.dispatch_location_type_snapshot IS NULL);

-- 2. Backfill order_items whose product is a SUPPLIER product with a linked warehouse.
UPDATE order_items oi
SET dispatch_location_id_snapshot   = w.id,
    dispatch_location_type_snapshot = 'WAREHOUSE',
    dispatch_location_name_snapshot = w.name,
    dispatch_address_snapshot       = w.address,
    dispatch_county_snapshot        = w.county,
    dispatch_latitude_snapshot      = w.latitude,
    dispatch_longitude_snapshot     = w.longitude
FROM products p
JOIN supplier_warehouses w
    ON w.id = p.supplier_warehouse_id
   AND w.user_id = p.seller_id
   AND w.is_active = TRUE
   AND NULLIF(BTRIM(w.address), '') IS NOT NULL
   AND w.latitude  BETWEEN -90  AND 90
   AND w.longitude BETWEEN -180 AND 180
WHERE oi.product_id = p.id
  AND p.seller_type = 'SUPPLIER'
  AND (oi.dispatch_location_id_snapshot IS NULL
       OR oi.dispatch_location_type_snapshot IS NULL);

-- 3. For FARMER products that still have no farm_id, try to assign the sole
--    active geocoded farm owned by that seller, then backfill the order_item.
WITH sole_farm AS (
    SELECT p.id AS product_id, MIN(f.id) AS farm_id
    FROM products p
    JOIN farms f
        ON f.farmer_id = p.seller_id
       AND f.status = 'ACTIVE'
       AND NULLIF(BTRIM(f.address), '') IS NOT NULL
       AND f.latitude  BETWEEN -90  AND 90
       AND f.longitude BETWEEN -180 AND 180
    WHERE p.seller_type = 'FARMER'
      AND p.farm_id IS NULL
    GROUP BY p.id
    HAVING COUNT(f.id) = 1
)
UPDATE products p
SET farm_id = sf.farm_id
FROM sole_farm sf
WHERE p.id = sf.product_id;

UPDATE order_items oi
SET dispatch_location_id_snapshot   = f.id,
    dispatch_location_type_snapshot = 'FARM',
    dispatch_location_name_snapshot = f.name,
    dispatch_address_snapshot       = f.address,
    dispatch_county_snapshot        = f.county,
    dispatch_latitude_snapshot      = f.latitude,
    dispatch_longitude_snapshot     = f.longitude
FROM products p
JOIN farms f
    ON f.id = p.farm_id
   AND f.farmer_id = p.seller_id
   AND f.status = 'ACTIVE'
   AND NULLIF(BTRIM(f.address), '') IS NOT NULL
   AND f.latitude  BETWEEN -90  AND 90
   AND f.longitude BETWEEN -180 AND 180
WHERE oi.product_id = p.id
  AND p.seller_type = 'FARMER'
  AND (oi.dispatch_location_id_snapshot IS NULL
       OR oi.dispatch_location_type_snapshot IS NULL);

-- 4. For SUPPLIER products that still have no warehouse_id, try the default or
--    sole active geocoded warehouse, then backfill the order_item.
WITH sole_warehouse AS (
    SELECT p.id AS product_id,
           COALESCE(
               (SELECT sw.id FROM supplier_warehouses sw
                WHERE sw.user_id = p.seller_id AND sw.is_active = TRUE
                  AND sw.is_default = TRUE
                  AND NULLIF(BTRIM(sw.address), '') IS NOT NULL
                  AND sw.latitude BETWEEN -90 AND 90 AND sw.longitude BETWEEN -180 AND 180
                LIMIT 1),
               (SELECT MIN(sw.id) FROM supplier_warehouses sw
                WHERE sw.user_id = p.seller_id AND sw.is_active = TRUE
                  AND NULLIF(BTRIM(sw.address), '') IS NOT NULL
                  AND sw.latitude BETWEEN -90 AND 90 AND sw.longitude BETWEEN -180 AND 180
                HAVING COUNT(*) = 1)
           ) AS warehouse_id
    FROM products p
    WHERE p.seller_type = 'SUPPLIER'
      AND p.supplier_warehouse_id IS NULL
)
UPDATE products p
SET supplier_warehouse_id = sw.warehouse_id
FROM sole_warehouse sw
WHERE p.id = sw.product_id
  AND sw.warehouse_id IS NOT NULL;

UPDATE order_items oi
SET dispatch_location_id_snapshot   = w.id,
    dispatch_location_type_snapshot = 'WAREHOUSE',
    dispatch_location_name_snapshot = w.name,
    dispatch_address_snapshot       = w.address,
    dispatch_county_snapshot        = w.county,
    dispatch_latitude_snapshot      = w.latitude,
    dispatch_longitude_snapshot     = w.longitude
FROM products p
JOIN supplier_warehouses w
    ON w.id = p.supplier_warehouse_id
   AND w.user_id = p.seller_id
   AND w.is_active = TRUE
   AND NULLIF(BTRIM(w.address), '') IS NOT NULL
   AND w.latitude  BETWEEN -90  AND 90
   AND w.longitude BETWEEN -180 AND 180
WHERE oi.product_id = p.id
  AND p.seller_type = 'SUPPLIER'
  AND (oi.dispatch_location_id_snapshot IS NULL
       OR oi.dispatch_location_type_snapshot IS NULL);

-- 5. Backfill pickup-station snapshots for PICKUP_STATION orders that have a
--    pickup_station_id but missing snapshot columns.
UPDATE orders o
SET pickup_station_name_snapshot      = ps.name,
    pickup_station_address_snapshot   = ps.address,
    pickup_station_latitude_snapshot  = ps.latitude,
    pickup_station_longitude_snapshot = ps.longitude
FROM pickup_stations ps
WHERE o.pickup_station_id = ps.id
  AND o.delivery_method = 'PICKUP_STATION'
  AND (o.pickup_station_name_snapshot IS NULL
       OR o.pickup_station_address_snapshot IS NULL
       OR o.pickup_station_latitude_snapshot IS NULL
       OR o.pickup_station_longitude_snapshot IS NULL);

-- 6. For PICKUP_STATION orders that stored the station id in address_id
--    (legacy path) and have no pickup_station_id yet, migrate the reference.
UPDATE orders o
SET pickup_station_id                 = ps.id,
    pickup_station_name_snapshot      = ps.name,
    pickup_station_address_snapshot   = ps.address,
    pickup_station_latitude_snapshot  = ps.latitude,
    pickup_station_longitude_snapshot = ps.longitude,
    address_id                        = NULL
FROM pickup_stations ps
WHERE o.address_id = ps.id
  AND o.delivery_method = 'PICKUP_STATION'
  AND o.pickup_station_id IS NULL;

COMMIT;
