-- Backfill users.reference_code for accounts created before the column existed.
--
-- Why: reference_code was added after these accounts were registered, so ddl-auto created
-- the column as NULL for all of them. Every user read returns it, and the admin screens
-- display it, so existing accounts showed a blank code while new registrations got one.
--
-- Codes follow the role prefix from UserRole.referencePrefix():
--   FARMER F-, BUYER B-, SUPPLIER S-, DRIVER DA-, INSPECTOR IN-, ADMIN AD-, SUPER_ADMIN SA-
--
-- Numbered per role in id order, so the result is stable if re-run. The role prefixes are
-- hardcoded rather than derived because SQL cannot call a Java enum method.

UPDATE users SET reference_code = 'F-'  || lpad(f.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'FARMER'
) f WHERE users.id = f.id AND users.reference_code IS NULL;

UPDATE users SET reference_code = 'B-'  || lpad(b.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'BUYER'
) b WHERE users.id = b.id AND users.reference_code IS NULL;

UPDATE users SET reference_code = 'S-'  || lpad(s.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'SUPPLIER'
) s WHERE users.id = s.id AND users.reference_code IS NULL;

UPDATE users SET reference_code = 'DA-' || lpad(d.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'DRIVER'
) d WHERE users.id = d.id AND users.reference_code IS NULL;

UPDATE users SET reference_code = 'IN-' || lpad(i.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'INSPECTOR'
) i WHERE users.id = i.id AND users.reference_code IS NULL;

UPDATE users SET reference_code = 'AD-' || lpad(a.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'ADMIN'
) a WHERE users.id = a.id AND users.reference_code IS NULL;

UPDATE users SET reference_code = 'SA-' || lpad(a.row_number::text, 3, '0') FROM (
    SELECT id, row_number() OVER (ORDER BY id) AS row_number FROM users WHERE role = 'SUPER_ADMIN'
) a WHERE users.id = a.id AND users.reference_code IS NULL;

-- Verify: should return zero rows
SELECT id, email, role, reference_code
  FROM users
 WHERE reference_code IS NULL
 ORDER BY id;