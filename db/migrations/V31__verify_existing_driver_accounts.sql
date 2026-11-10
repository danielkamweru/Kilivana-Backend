BEGIN;

UPDATE users
SET email_verified_at = CURRENT_TIMESTAMP
WHERE role = 'DRIVER'
  AND email_verified_at IS NULL;

COMMIT;
