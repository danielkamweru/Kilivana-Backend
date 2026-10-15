> Historical local-development evidence, sanitized for review. Private paths are omitted. Product approval is now mandatory; see [current policy](../18-product-approval-decision-and-verification.md). This document does not authorize SQL execution.

# Kilivana — isolated PostgreSQL migration rehearsal results

2026-10-15, canonical HEAD `8f9dded`. **ISOLATED RESTORE, V16–V18 MIGRATION AND HIBERNATE VALIDATION PASSED.** The rehearsal server was then stopped. The existing local database was not migrated; the working backend was not restarted. No application feature changes, commits or pushes.

## Backup and isolated environment

- Backup: `<private-local-path>`; 1,033,128 bytes.
- SHA-256 verified again before initialization and after rehearsal: `A771B3B186940F11E8CFEB8CC00BE69799D4FC2C54F8674D84F31CBFC622CA0C`.
- PostgreSQL18.1 custom archive, 35 table-data entries and all eight image large objects; original archive-list and full-decompression checks passed. **Actual restore is now tested successfully.**
- Independent data directory: `<private-local-path>`.
- Verified connection: `127.0.0.1:55432/kilivana`, login `kilivana_app`.
- Listener was exclusively 127.0.0.1:55432; verified OS listener PID against this cluster's postmaster.pid, connected server port and actual data_directory. Checked identity again before migration. No existing data directory was reused; no service was registered and no external binding was enabled.
- Explicitly approved bootstrap administrator existed only inside the new cluster, used to create the app role/database. Application role verified NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS. Existing5432 administrator credentials were neither read nor changed.
- Bootstrap credential retained only in Windows-encrypted CLIXML inside the private rehearsal directory. Passwords never appeared in command arguments or reports. The temporary initdb password file was cleared and removed after use.
- Restore executed as the restricted app role, with --no-owner --no-privileges --exit-on-error --single-transaction, into a verified empty isolated database. There was no --clean, data deletion or status repair.

## Results

| Check | Result |
|---|---|
| Full isolated archive restore | PASS |
| Restored baseline versus original backup evidence | PASS: all 71 preservation entries identical |
| Corrected reviewed preflight | PASS on isolated55432 |
| V16 tracking tables/indexes | PASS |
| V17 nullable product/payment fields and indexes | PASS |
| V18 expanded payment checks and non-cascading order-item FK | PASS |
| Single-transaction driver preservation assertions and COMMIT | PASS |
| Independent post-migration verify.sql | PASS |
| Original versus restored versus migrated versus final source fingerprints | PASS: all 71 entries identical |
| Read-only Hibernate schema validation | PASS: all 37 mapped entities on55432 |
| Rehearsal shutdown | PASS: pg_ctl fast/clean shutdown of this data directory; no55432 listener remains |

Final isolated schema: **37 tables, 36 sequences, 69 indexes**. Preserved: **5 users, 5 farmer profiles, 5 products, 19 categories, 8 product-image links, 8 stored-image rows and 8 large objects**. Every original record value and original sequence state matches the backup, not just row counts. All five products are still PENDING_APPROVAL; no new verification fields have values. All eight images can be read through lo_get, match their recorded lengths and retain their exact aggregate byte fingerprint. New tracking tables remain empty and all 17 added product/payment fields remain NULL.

Hibernate validation ran through a standalone helper hardcoded to jdbc:postgresql://127.0.0.1:55432/kilivana, with default_transaction_read_only=on. It invoked the real Hibernate schema validator over the current 37 entities. No Spring context, automatic schema updates, seeders, backend restart or external integrations ran. No claim of migrated Android/device/web/admin runtime testing is made.

## Necessary correction discovered by rehearsal

The first restored preflight rejected the raw schema signature before any DDL. Investigation compared every captured column, index, sequence and constraint. PostgreSQL18 dump/restore redistributed varchar-to-text casts in **27 enum CHECK expressions**: source casts the complete array; restored checks cast each array element. Column references, operators, allowed values and validation flags are equivalent. All other captured structures matched exactly. The extra driver suspension check also remained intact.

Only **preflight.sql** changed: it now accepts either exact reviewed signature:

- Original source: `7a3b48703b3f4a7e711a98bc08f0aca6`.
- Verified restored equivalent: `7152b9b7de31b4f352c52970d34e5a0e`.

This is an exact second allowlisted structure, not a generic removal of CHECK validation. All other schema drift remains rejected. SHA256SUMS.txt was updated; the original reviewed manifest is retained privately for comparison. **V16, V17, V18, transaction driver, preservation query and post-verification SQL bodies were unchanged.** No failed migration transaction or cleanup of data was needed: the initial guard prevented migration, and the corrected run committed successfully.

A Windows provisioning issue also occurred before database creation: initdb's console prompt blocked redirected input. Only those new initdb processes were canceled; the first private attempt contains encrypted bootstrap metadata and no initialized database. Retry used a protected temporary password file. The pg_ctl wrapper inherited output handles and was stopped separately once the new server was ready; PostgreSQL itself stayed available for verified provisioning. Neither issue required changing migration SQL, the existing cluster or backend. All private attempt/evidence directories are retained; no unrelated work was discarded.

## Evidence retained

Private directory: `<private-local-path>`.

Includes initdb/server/stop logs, verified server identity, restricted app-role identity, restore log, restored catalog/schema signature, initial rejected preflight, successful corrected preflight, migration COMMIT output, post-verification output, restored/migrated/final-source fingerprints, migrated summary, Hibernate validation log and cluster state with Started=false/Completed=true. These contain private database evidence and are not proposed for Git sharing. The backup and encrypted bootstrap credential remain in their private locations. The initial rejected preflight log and original SQL manifest are preserved.

## Existing local environment and readiness

- Existing PostgreSQL5432 remains its original process PID7448; backend127.0.0.1:8081 remains original PID16396. Neither was stopped or restarted.
- An intentional **read-only** final source comparison confirms all 71 original record/sequence/image entries still match the backup. There were no source DDL, application-record writes, grants or administrator operations.
- Rehearsal is complete and the proposed migration is **technically ready for separate approval to apply to this reviewed local baseline**. No execution approval for5432 is implied.
- Before local application: review the preflight correction and new checksum; recheck schema/identity; take a fresh complete large-object-inclusive backup if any subsequent writes occurred (prefer a fresh maintenance-window backup regardless); quiesce client writes under an approved maintenance plan; use the one-transaction driver, then verify Hibernate and actual farmer app workflows. Do not enable Hibernate update or historical migrations.
- Remaining limits: tiny local dataset, no populated order/payment cases, no concurrency/load rehearsal, no migrated backend startup/device verification, no production/staging/web/admin readiness claim. Environments already running historicalV14/V15 need their own schema review. Existing timestamp/nullability choices remain the reviewed entity-compatible ones.
- Product approval remains **PENDING TEAM DECISION**; existing product status transitions and visibility were not changed. No Phase2 or other backend development started.

**Await separate approval before applying anything to the existing local database.**
