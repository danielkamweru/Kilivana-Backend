-- Inspectors state what they are qualified to check.
ALTER TABLE inspector_profiles ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
