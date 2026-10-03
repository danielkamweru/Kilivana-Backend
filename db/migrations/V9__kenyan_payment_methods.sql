-- Kenyan payment rails.
--
-- The panel names the rail "method" and expects MPESA / BANK / CARD. The column stays
-- called provider so no other query has to move, and the seeded rows are translated:
-- MTN MoMo was Ghanaian mobile money and M-PESA is the Kenyan equivalent, so both land
-- on MPESA rather than being left behind as values nothing reads.
--
-- 'string' rows came from smoke tests that posted a placeholder instead of a provider.
-- They are given MPESA because that is the platform default rail, not because we know
-- what they were meant to be.

UPDATE payments
SET provider = CASE UPPER(REPLACE(provider, '-', '_'))
    WHEN 'MTN_MOMO' THEN 'MPESA'
    WHEN 'MTN'       THEN 'MPESA'
    WHEN 'MOMO'      THEN 'MPESA'
    WHEN 'M_PESA'    THEN 'MPESA'
    WHEN 'STRING'    THEN 'MPESA'
    WHEN 'BANK'      THEN 'BANK'
    WHEN 'CARD'      THEN 'CARD'
    ELSE 'MPESA'
END
WHERE provider IS NOT NULL;

-- Anything still unrecognised after the map above, so the column's NOT NULL and the
-- enum can both hold.
UPDATE payments SET provider = 'MPESA' WHERE provider IS NULL OR TRIM(provider) = '';

-- Status: PROCESSING folded into PENDING, COMPLETED renamed to PAID, and the escrow
-- steps the panel shows added. PARTIALLY_REFUNDED is gone from the enum, so any row
-- that used it becomes a full REFUND rather than being left with a value the enum
-- cannot deserialise.
UPDATE payments
SET status = CASE status
    WHEN 'PROCESSING'           THEN 'PENDING'
    WHEN 'COMPLETED'            THEN 'PAID'
    WHEN 'PARTIALLY_REFUNDED'   THEN 'REFUNDED'
    ELSE status
END;

-- paid_at is when the money arrived. Rows that were already COMPLETED have one; those
-- that were only PROCESSING did not, and PAID rows now mean the money arrived.
UPDATE payments SET paid_at = created_at WHERE status = 'PAID' AND paid_at IS NULL;