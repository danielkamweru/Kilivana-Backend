-- Allow proof of delivery to be filed without a signature or photo.
--
-- Why: signature_url, photo_url and otp_reference were all NOT NULL, so any proof that
-- omitted a signature was rejected with
--   ERROR: null value in column "signature_url" ... violates not-null constraint
-- The delivery OTP is the authoritative confirmation of hand-over, so a driver who cannot
-- capture a signature still has to be able to complete the delivery. These three columns
-- are now optional; recipient_name and delivered_at remain mandatory.
--
-- ddl-auto=update does not relax an existing NOT NULL constraint, so this must be run once
-- against each existing database. Safe to re-run.

ALTER TABLE proof_of_deliveries ALTER COLUMN signature_url DROP NOT NULL;
ALTER TABLE proof_of_deliveries ALTER COLUMN photo_url     DROP NOT NULL;
ALTER TABLE proof_of_deliveries ALTER COLUMN otp_reference DROP NOT NULL;

-- Verify
SELECT column_name, is_nullable
  FROM information_schema.columns
 WHERE table_name = 'proof_of_deliveries'
   AND column_name IN ('signature_url', 'photo_url', 'otp_reference')
 ORDER BY column_name;