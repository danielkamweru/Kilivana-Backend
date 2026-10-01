-- Email addresses are compared case-insensitively by the application, so the database has to
-- agree with it. The generated unique constraint on users(email) is case-sensitive in
-- PostgreSQL, which let "Jane@example.com" and "jane@example.com" become two accounts: the
-- second registration failed as a conflict while login reported an unknown address.

UPDATE users
SET email = lower(btrim(email))
WHERE email <> lower(btrim(email));

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON users (lower(email));