-- Driver fields the admin panel records but the profile could not store.
--
-- Why: driver_profiles had no address and no suspension reason, so an administrator
-- could not record where a driver lives or why a driver was pulled off the road, and
-- neither fact survived a reload. KYC state was held only on users.verification_status,
-- which is an account-level email check rather than a licence check, so a verified
-- driver's licence state could not be shown at all.
--
-- Also constrains availability_status and vehicle_type. Both were free text, so the
-- table already held "Truck" and "TRUCK" side by side and a query filtering on one
-- spelling silently missed the other. The application ships matching enums; these
-- constraints make the database agree instead of accepting a third spelling.

ALTER TABLE driver_profiles
    ADD COLUMN IF NOT EXISTS address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(255) NOT NULL DEFAULT 'PENDING';

-- Normalise the spellings already in the table before constraining it. Mixed case is
-- what the constraints are about to reject, so it is fixed here rather than reported.
UPDATE driver_profiles
   SET vehicle_type = upper(btrim(vehicle_type))
 WHERE vehicle_type <> upper(btrim(vehicle_type));

UPDATE driver_profiles
   SET availability_status = upper(btrim(availability_status))
 WHERE availability_status <> upper(btrim(availability_status));

-- Backfill KYC from the licence expiry that was already stored: an expired licence is
-- not a verified one, and anything expiring within the month is treated the same way.
UPDATE driver_profiles
   SET kyc_status = CASE
           WHEN license_expiry_date IS NULL THEN 'PENDING'
           WHEN license_expiry_date >= (CURRENT_DATE + INTERVAL '30 days') THEN 'VERIFIED'
           ELSE 'PENDING'
       END
 WHERE kyc_status = 'PENDING';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_driver_profiles_availability_status'
    ) THEN
        ALTER TABLE driver_profiles
            ADD CONSTRAINT ck_driver_profiles_availability_status
            CHECK (availability_status IN ('AVAILABLE', 'ON_DELIVERY', 'OFFLINE', 'SUSPENDED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_driver_profiles_vehicle_type'
    ) THEN
        ALTER TABLE driver_profiles
            ADD CONSTRAINT ck_driver_profiles_vehicle_type
            CHECK (vehicle_type IN ('TRUCK', 'VAN', 'PICKUP', 'MOTORBIKE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_driver_profiles_kyc_status'
    ) THEN
        ALTER TABLE driver_profiles
            ADD CONSTRAINT ck_driver_profiles_kyc_status
            CHECK (kyc_status IN ('PENDING', 'VERIFIED'));
    END IF;
END
$$;

-- A suspended driver has to say why, otherwise the record cannot be acted on.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_driver_profiles_suspension_reason'
    ) THEN
        ALTER TABLE driver_profiles
            ADD CONSTRAINT ck_driver_profiles_suspension_reason
            CHECK (availability_status <> 'SUSPENDED' OR suspension_reason IS NOT NULL);
    END IF;
END
$$;

-- Verify
SELECT availability_status, vehicle_type, kyc_status, count(*)
  FROM driver_profiles
 GROUP BY availability_status, vehicle_type, kyc_status;