-- Additive lifecycle expansion. Review and apply separately before deploying this code.
-- No data rewrite, deletion or automatic execution; retains all existing status values.
BEGIN;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check;
ALTER TABLE products ADD CONSTRAINT products_status_check
    CHECK (status IN ('ACTIVE','PENDING_APPROVAL','REJECTED','SUSPENDED','UNLISTED','ARCHIVED','DELETED'));
COMMIT;
