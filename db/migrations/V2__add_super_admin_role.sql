-- Widen the users.role CHECK constraint to include SUPER_ADMIN.
--
-- Why this is needed manually:
-- Hibernate's ddl-auto=update generated a CHECK constraint from the UserRole enum when it
-- still had six values. Adding SUPER_ADMIN to the enum does NOT rewrite an existing
-- constraint, so on any database created before that change the seeder's insert fails with
--   ERROR: new row for relation "users" violates check constraint "users_role_check"
-- and the application refuses to start.
--
-- A freshly created database gets the correct seven-value constraint automatically, so this
-- script only needs running against environments that predate the SUPER_ADMIN change.
--
-- Run once, against each existing environment (dev, staging, production).

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('FARMER','BUYER','SUPPLIER','INSPECTOR','DRIVER','ADMIN','SUPER_ADMIN'));

-- Verify
SELECT conname, pg_get_constraintdef(oid)
  FROM pg_constraint
 WHERE conrelid = 'users'::regclass
   AND conname = 'users_role_check';