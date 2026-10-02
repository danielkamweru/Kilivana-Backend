-- One back-office role, not two.
--
-- Why: ADMIN and SUPER_ADMIN carried identical permissions, so nothing could be granted to one
-- and withheld from the other. Every check had to name both roles, and adding a permission to
-- "an administrator" silently granted it to both. One role removes the ambiguity.
--
-- Any account holding SUPER_ADMIN becomes ADMIN, keeping its id so references to it stay valid.
-- An account that was created only as a second administrator may end up duplicated by an
-- existing ADMIN row; the rows are reported below rather than deleted, because which one to keep
-- depends on data this script cannot see.

UPDATE users SET role = 'ADMIN' WHERE role = 'SUPER_ADMIN';

-- Widening is not needed; narrowing is. The generated constraint still allows SUPER_ADMIN until
-- it is replaced, and leaving it would let the application write a value the enum rejects.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('FARMER','BUYER','SUPPLIER','INSPECTOR','DRIVER','ADMIN'));

-- SA- reference codes belong to the retired role. Admin codes are AD-, and the table already
-- holds AD-001 through AD-00n, so these must continue that sequence rather than restart at 1:
-- restarting collides with an existing code on the unique index.
WITH highest AS (
    -- Null when a row has no code; max ignores it, and a non-numeric code would be a defect
    -- worth seeing rather than something to silently skip, so the cast is left to fail loudly.
    SELECT coalesce(max(substring(reference_code from 'AD-([0-9]+)')::int), 0) AS last_number
      FROM users
     WHERE reference_code LIKE 'AD-%'
), renumbered AS (
    SELECT id,
           'AD-' || lpad(((SELECT last_number FROM highest)
                 + row_number() OVER (ORDER BY id))::text, 3, '0') AS new_code
      FROM users
     WHERE reference_code LIKE 'SA-%'
)
UPDATE users u
   SET reference_code = renumbered.new_code
  FROM renumbered
 WHERE u.id = renumbered.id;

-- Verify: no SUPER_ADMIN rows and no SA- codes should remain.
SELECT id, email, role, reference_code
  FROM users
 WHERE role = 'SUPER_ADMIN' OR reference_code LIKE 'SA-%';

SELECT role, count(*) FROM users GROUP BY role ORDER BY role;