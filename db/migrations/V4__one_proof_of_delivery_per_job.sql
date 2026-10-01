-- One delivery confirmation per job.
--
-- Why: proof_of_deliveries had no constraint tying a proof to its job, so a second proof
-- could be filed for a job that was already delivered. A delivery is a one-time event;
-- allowing several records for the same job makes "was this delivered?" unanswerable and
-- lets a driver overwrite the history of a completed delivery.
--
-- BEFORE applying the index, collapse any duplicates this bug already allowed.
--
-- DESTRUCTIVE, and deliberately so. A second proof for the same job is not a valid record -
-- it can only have been created by the overwrite bug this migration closes. The earliest row
-- per job is kept, because that is the genuine delivery; later rows are the artefact.
--
-- Run the SELECT first on a non-development database and confirm the count is zero.

SELECT logistics_job_id, count(*) AS proofs
  FROM proof_of_deliveries
 GROUP BY logistics_job_id
HAVING count(*) > 1;

DELETE FROM proof_of_deliveries p
  USING proof_of_deliveries keep
 WHERE p.logistics_job_id = keep.logistics_job_id
   AND p.id > keep.id;

-- The unique index is the real guarantee. The application also checks for an existing proof
-- before inserting, but that is a race between two concurrent requests and the database is
-- the only place that can settle it.
--
-- Safe to re-run. The CREATE UNIQUE INDEX is inside a DO block that skips the work if a
-- matching index already exists.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
         WHERE tablename = 'proof_of_deliveries'
           AND indexdef ILIKE '%UNIQUE%logistics_job_id%'
    ) THEN
        CREATE UNIQUE INDEX ux_proof_of_deliveries_job
            ON proof_of_deliveries (logistics_job_id);
    END IF;
END
$$;

-- Verify
SELECT indexname, indexdef
  FROM pg_indexes
 WHERE tablename = 'proof_of_deliveries';