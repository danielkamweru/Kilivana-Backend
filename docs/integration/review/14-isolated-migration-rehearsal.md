> Historical local-development evidence, sanitized for review. Private paths are omitted. Product approval is now mandatory; see [current policy](../18-product-approval-decision-and-verification.md). This document does not authorize SQL execution.

# Kilivana — isolated migration rehearsal status

2026-10-15; canonical HEAD `8f9dded`. **BACKUP COMPLETED; REHEARSAL BLOCKED AT ADMINISTRATOR PROVISIONING BOUNDARY.** No existing database migration, isolated migration, restore, application restart, commit or push occurred.

## Completed and verified

- Read plan 13, reviewed V16–V18 and helper scripts; all seven reviewed SHA-256 checksums match. No proposed SQL changes were required or made.
- Source identity authenticated with Windows-encrypted credentials as non-superuser `kilivana_app` at `127.0.0.1:5432/kilivana`. Source SQL connections were read-only; no PostgreSQL administrator account was used.
- PostgreSQL 18.1 pg_dump complete custom-format backup with `--large-objects`, entire database, no table filtering. Saved outside Git in a new directory whose inherited permissions were removed and FullControl granted to the current Windows user only. No credentials were written to command arguments or logs.
- Backup: `<private-local-path>`
- Size: **1,033,128 bytes**.
- SHA-256: `A771B3B186940F11E8CFEB8CC00BE69799D4FC2C54F8674D84F31CBFC622CA0C`.
- pg_dump exited 0; file exists and is nonempty; pg_restore --list exited 0.
- pg_restore --file NUL exited 0: full archive read/decompression into Windows null device, not a SQL execution or database restore. This tests archive readability, not restore correctness.
- TOC contains all 35 table-data entries and large-object metadata/payload for eight image objects. PostgreSQL18 groups the objects into TOC ranges: one metadata group and one payload group represent eight objects, not one image.
- All **71 preservation entries** match before versus after backup: 35 original table digests/counts, 35 sequence states and the aggregate byte fingerprint of all eight stored-image large objects. No personal rows, passwords, tokens or image bytes were printed.
- Read-only preflight still passes the exact reviewed 35-table schema signature. Existing products remain unchanged, including PENDING_APPROVAL. The source backend listener remains 127.0.0.1:8081, original PID16396; PostgreSQL on5432 remains running.

Private backup evidence beside the archive includes backup-manifest.json, archive-toc.txt, source-identity.json and before/after fingerprint files. These are not committed or shared as PR content.

## Isolated provisioning blocker

No server is listening on55432. Docker reports: `Docker Desktop is unable to start`. Installed native initdb/pg_ctl binaries are PostgreSQL18.1, but initdb explicitly bootstraps a database superuser (`--username`: database superuser name). Provisioning a restricted app role and database then requires using that bootstrap administrator inside the new cluster. Existing kilivana_app has neither CREATEDB nor CREATEROLE and cannot perform this setup through5432. It must not be promoted.

A native new cluster ordinarily need not require Windows administrator elevation. **The blocker is permission to use a newly created PostgreSQL bootstrap administrator**, not a need for the existing postgres password or permission to alter the5432 instance. The user's explicit instruction was to stop and explain if isolated provisioning requires administrator access, so no initdb, server startup, role creation, database creation or restore was attempted. No data directory was reused, allocated or initialized for a second server.

Two safe ways to unblock:

1. The user/operator provides an independently provisioned, empty PostgreSQL18 cluster on127.0.0.1:55432, database kilivana, limited role kilivana_app, app-owned application objects and public CREATE/USAGE plus database TEMP; or
2. Explicitly authorize a new bootstrap administrator **only inside a newly created private rehearsal cluster with a distinct data directory**, to provision the limited app role/database. All restore/migration/validation operations would then use kilivana_app, not the bootstrap account. No existing administrator account, role grants or data directory on5432 would be touched.

This is a disposable rehearsal environment, not a permanent Android backend/database.

## Required remaining verification

| Deliverable | Actual result |
|---|---|
| Backup creation and archive integrity checks | Passed; full restore is not yet tested |
| Isolated connection | Planned 127.0.0.1:55432/kilivana as kilivana_app; no instance provisioned |
| Restore into isolated database | NOT RUN |
| V16–V18 on isolated restored copy | NOT RUN |
| Pre/post restored-copy records/sequences/images comparison | NOT RUN; source before/after backup comparison passed |
| Post-migration constraints/index checks | NOT RUN |
| Hibernate validation against migrated isolated database | NOT RUN |
| Migration-script corrections | None made; execution may still reveal issues |
| Ready to apply to existing local database | **NO — requires successful restore/rehearsal/Hibernate validation first, then separate approval** |

After provisioning approval, verify separate data directory/listener identity and empty database; restore this archive only to55432; compare the restored baseline; execute the reviewed driver with expected_port=55432; compare every preserved record, sequence and image fingerprint; run verify.sql and Hibernate validate with hardcoded isolated JDBC URL, no schema updates/seeders and no Spring application startup needed. Do not reuse the local validation helper unchanged because it hardcodes5432. Correct only reviewed migration/helper defects if revealed; document new checksums and rerun on an approved clean rehearsal target rather than deleting live data.

The current backup was consistent and source fingerprints were stable while dumping, but the live app remains running. A final local migration still requires a fresh backup/preflight after any subsequent writes, a maintenance plan and explicit local execution approval. Product approval remains **PENDING TEAM DECISION**. No Phase2 work started.
