-- Review and apply before deploying email OTP. Never run on a shared database from tests.
-- Existing users retain access; this does NOT claim historical email proof.
BEGIN;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
-- Grandfather old rows, then fail closed for future inserts that omit this field.
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
CREATE TABLE email_verification_challenges (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    challenge_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    purpose VARCHAR(255) NOT NULL CHECK (purpose = 'EMAIL_VERIFICATION'),
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts BETWEEN 0 AND 5),
    attempts_reset_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE email_otp_rate_limits (
    bucket_key VARCHAR(64) PRIMARY KEY,
    reset_at TIMESTAMPTZ NOT NULL,
    requests INTEGER NOT NULL DEFAULT 0 CHECK (requests >= 0),
    last_requested_at TIMESTAMPTZ,
    latest_challenge_id UUID
);
CREATE INDEX email_otp_rate_limits_reset_at_idx ON email_otp_rate_limits(reset_at);
COMMIT;
