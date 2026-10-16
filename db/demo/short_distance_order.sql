-- Seed a test order with short-distance logistics job (5km) for admin testing
-- Status: PENDING_ASSIGNMENT - admin can assign driver, update status, cancel, etc.

-- 1. Create test buyer (if not exists)
INSERT INTO users (id, username, email, phone, password_hash, role, status, verification_status, reference_code, created_at)
VALUES (900, 'testbuyer_short', 'buyer.short@test.com', '254700000900', '$2a$10$dummyhash', 'BUYER', 'ACTIVE', 'VERIFIED', 'B-900', NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Create test farmer/supplier (seller for the order)
INSERT INTO users (id, username, email, phone, password_hash, role, status, verification_status, reference_code, created_at)
VALUES (901, 'testfarmer_short', 'farmer.short@test.com', '254700000901', '$2a$10$dummyhash', 'FARMER', 'ACTIVE', 'VERIFIED', 'F-901', NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. Create test driver (for assignment)
INSERT INTO users (id, username, email, phone, password_hash, role, status, verification_status, reference_code, created_at)
VALUES (902, 'testdriver_short', 'driver.short@test.com', '254700000902', '$2a$10$dummyhash', 'DRIVER', 'ACTIVE', 'VERIFIED', 'DA-902', NOW())
ON CONFLICT (id) DO NOTHING;

-- 4. Create buyer address (destination)
INSERT INTO addresses (id, user_id, address_text, latitude, longitude, label, created_at, updated_at)
VALUES (900, 900, 'Westlands, Nairobi - Short Distance Test', -1.2676, 36.8108, 'Home', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 5. Create test product (from farmer 901)
INSERT INTO products (id, seller_id, name, description, category, unit, price_per_unit, stock_quantity, min_order_quantity, status, quality_grade, images, created_at, updated_at)
VALUES (900, 901, 'Fresh Tomatoes (Short Distance Test)', 'Test product for short distance delivery', 'VEGETABLES', 'KG', 150.00, 100, 1, 'ACTIVE', 'GRADE_A', '[]', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 6. Create test order (PLACED - appears in admin "Placed Orders" tab)
INSERT INTO orders (id, buyer_id, code, status, subtotal, delivery_fee, total, payment_status, address_id, created_at, updated_at)
VALUES (900, 900, 'ORD-SHORT-001', 'PLACED', 3000.00, 100.00, 3100.00, 'PAID', 900, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 7. Create order item
INSERT INTO order_items (id, order_id, product_id, seller_id, product_name, unit, quantity, unit_price, subtotal, created_at)
VALUES (900, 900, 900, 901, 'Fresh Tomatoes (Short Distance Test)', 'KG', 20, 150.00, 3000.00, NOW())
ON CONFLICT (id) DO NOTHING;

-- 8. Create logistics job with SHORT DISTANCE (5km) in PENDING_ASSIGNMENT
-- This gives admin full control: assign driver, update status, cancel, etc.
INSERT INTO logistics_jobs (
    id, order_id, pickup_address, destination_address,
    pickup_latitude, pickup_longitude,
    destination_latitude, destination_longitude,
    distance_km, estimated_minutes,
    payout_amount, status, driver_id,
    delivery_otp_hash, delivery_otp_expires_at, delivery_otp_attempts,
    created_at, updated_at
) VALUES (
    900, 900,
    'Kikuyu Farm, Kiambu County', 'Westlands, Nairobi - Short Distance Test',
    -1.2450, 36.6600,  -- Pickup: Kikuyu (~5km from Westlands)
    -1.2676, 36.8108,  -- Destination: Westlands
    5.0, 15,           -- 5km, ~15 minutes
    2910.00,           -- Payout: subtotal (3000) - 3% platform fee (90) = 2910
    'PENDING_ASSIGNMENT',
    NULL,              -- No driver assigned yet - admin can assign
    '$2a$10$dummyotphash',  -- Dummy OTP hash
    NOW() + INTERVAL '24 HOURS',
    0,
    NOW(), NOW()
)
ON CONFLICT (id) DO NOTHING;

-- 9. Create payment record for the order
INSERT INTO payments (id, order_id, provider, reference, amount, status, paid_at, created_at, updated_at)
VALUES (900, 900, 'MPESA', 'TEST-MPESA-900', 3100.00, 'PAID', NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Verify the seeded data
SELECT 'Users' as table_name, count(*) FROM users WHERE id BETWEEN 900 AND 902
UNION ALL
SELECT 'Addresses', count(*) FROM addresses WHERE id = 900
UNION ALL
SELECT 'Products', count(*) FROM products WHERE id = 900
UNION ALL
SELECT 'Orders', count(*) FROM orders WHERE id = 900
UNION ALL
SELECT 'OrderItems', count(*) FROM order_items WHERE id = 900
UNION ALL
SELECT 'LogisticsJobs', count(*) FROM logistics_jobs WHERE id = 900
UNION ALL
SELECT 'Payments', count(*) FROM payments WHERE id = 900;

-- Show the logistics job details
SELECT id, order_id, pickup_address, destination_address, distance_km, estimated_minutes,
       payout_amount, status, driver_id, delivery_otp_expires_at
FROM logistics_jobs WHERE id = 900;
