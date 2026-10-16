> Historical local-development evidence, sanitized for review. Private paths are omitted. Product approval is now mandatory; see [current policy](../18-product-approval-decision-and-verification.md). This document does not authorize SQL execution.

# Kilivana — approved local migration and canonical backend verification

2026-10-15; branch `feature/android-backend-integration`, HEAD `8f9dded`. **V16–V18 APPLIED SUCCESSFULLY to the approved LOCAL DEVELOPMENT database. Reconciled backend running on127.0.0.1:8081.** No commits, pushes, Phase2 work, new backend features, unrelated schema changes or Android source edits.

## Safety, maintenance and fresh backup

Read plans13 and15, compared all seven migration/helper hashes with the successful rehearsal manifest, verified exact database/schema identity as kilivana_app on127.0.0.1:5432/kilivana. No administrator credentials were used. Historical migration files and automatic schema updates were not run.

Before maintenance, the old backend's ten connections were idle without transactions. pgAdmin connections were present, but no database write locks or prepared transactions were detected. The known old Java process PID16396 was verified against the canonical classpath/main class and stopped under the approved maintenance window. Windows process termination/socket release was asynchronous: the first immediate listener check still saw the closing listener, and a subsequent check confirmed both process and listener gone before proceeding. No migration ran during that interval. After shutdown: zero other kilivana_app sessions, zero write locks and zero prepared transactions. No unrelated sessions or PostgreSQL processes were terminated; pgAdmin connections were not altered. The brief checks and transaction preservation assertions establish observed quiescence and unchanged data; this was not a global block on privileged pgAdmin reconnects.

Fresh full PostgreSQL18 custom archive, including large objects:

- Location: `<private-local-path>`
- Size: **1,033,128 bytes**.
- SHA-256: `8A906C950210E762BB8C86D03308D3FC701DF0DF7B1385F4BB7FC6D27187A29E`.
- New private directory restricted to the current Windows account; outside Git. Credentials came from the Windows-encrypted CLIXML and child-process environment, not command arguments or reports.
- pg_dump exit0, archive exists/nonempty, pg_restore --list exit0, full archive read/decompression to Windows NUL exit0. Before/after backup fingerprints matched across all71 preservation entries. The same procedure/archive structure had passed an actual isolated restore in rehearsal; this fresh archive itself was integrity-read, not separately restored again.
- Backup checksum rechecked successfully at final verification. Backup, private logs and encrypted smoke credentials are retained; nothing was deleted or restored over the database.

## Migration and preservation results

Only the reviewed `docs/integration/schema-review/apply-reviewed-schema.psql` driver executed writable SQL. It performed the reviewed V16/V17/V18 files in one transaction, repeated identity/schema checks, used bounded locks, verified mapped columns/indexes/constraints and compared preservation fingerprints before COMMIT. No script or manifest corrections were needed during local execution.

Result: **37 tables, 36 sequences, 69 indexes**, expected nullable product/payment fields, two tracking tables, quality-grade check, expanded FAILED payment checks, validated non-cascading order-item FK. Post-migration verify.sql and the final repeated verification both passed.

Immediately after migration, all **71 entries** matched the maintenance-window baseline: every original table's records, every original sequence state, and original image bytes. Preserved original inventory: **5 users, 5 farmer profiles, 5 products, 19 categories, 8 product-image links, 8 stored-image rows and 8 PostgreSQL large objects**. All five products remained PENDING_APPROVAL, new metadata fields were NULL and new tracking tables were empty. No farm, inspection, grade or verification values were invented.

After API smoke testing, original-record checks filtered the original ID ranges and again matched all35 original table fingerprints plus the original image-byte fingerprint. Only explicitly approved marked smoke records were added; original rows/images were not edited. Original sequence states were preserved by migration; applicable sequences then advanced normally for the authorized smoke inserts.

## Hibernate and reconciled backend startup

- Standalone Hibernate validation passed over all37 mapped entities against a hardcoded127.0.0.1:5432/kilivana JDBC connection with default_transaction_read_only=on. No Spring context, seeders or schema writes in that validation helper.
- Refreshed ignored target/local-runtime-classpath.txt from the tested dependency classpath, including Daniel's WebSocket/WebFlux dependencies and excluding test classes. No launcher or application configuration file was changed.
- Started through the existing secure Start-LocalCanonical.ps1 launcher with JDK17 and the unchanged encrypted database credential/JWT signing secret. Main class and classpath are the reconciled8f9dded tree.
- New backend PID **31092**, listener **127.0.0.1:8081** only. Startup arguments explicitly enforce ddl-auto=validate, SQL initialization never, seeders off and database image storage. No public tunnel or external exposure was added.
- GET / returns HTTP200 and 'Kilivana Backend is running'; GET /api/v1/health returns HTTP200. Final health still200.
- PostgreSQL5432 was not restarted. Reference backend/repository and other databases were untouched.

## Actual HTTP verification

| Workflow | Result |
|---|---|
| Original product retrieval | PASS: all5 original products, IDs, prices, stock and pending statuses checked |
| Original image retrieval | PASS: all8 HTTP image byte lengths/digests match PostgreSQL originals |
| Active FARMER categories | PASS: 11 returned |
| Farmer registration and email/password login | PASS: one new marked local smoke account |
| Farmer profile creation/read | PASS: new smoke farmer's own profile |
| Own seller listings | PASS: new fixture product returned after refresh with two images |
| Product creation/edit | PASS: one marked product, integer stock and exact decimal125.50 then126.75; remained pending |
| Multiple image upload/retrieval | PASS: two PNGs via multipart field image; both retrieved byte-identically; product returns both |
| Owner status escalation | PASS: attempting ACTIVE on own smoke product returns403; status remains pending |
| Cross-farmer profile read | PASS:403 |
| Access JWT submitted as refresh JWT | PASS:401 |
| Persisted session and refresh | PASS: restored encrypted Windows CLIXML in a separate PowerShell client process; /auth/me, /auth/refresh and subsequent profile/listing/image reads succeeded |

No upload was automatically retried. All image test requests were restricted to loopback8081, with redirects disabled; image retrieval requests did not forward authentication to external hosts. No payment, checkout, order or external mail/provider calls were made.

Existing device-test account login was **not verified**: those accounts use independently randomized passwords generated on the phone. An initial attempt using the uninterpolated test template returned401; the harness then stopped using those unknown credentials. No passwords, roles or verification states were reset. Login/profile verification used the newly approved marked fixture instead; original listings/images were read separately. This is a credential-availability limit, not evidence of an existing-account backend regression.

Retained authorized test additions: **one user, one farmer profile, one pending product, two product-image links, two stored-image rows/large objects**. Test names explicitly say Migration Verification/Smoke. Fixture IDs and Windows-encrypted credentials/session are in the private evidence directory, not this shareable report. Final totals:6 users,6 farmer profiles,6 products (all pending),19 categories,10 product-image links,10 stored-image rows and10 large objects. Orders, inspections and farms remain empty.

The client-process session check is **not Android physical-device persistence verification**. Refresh remains the existing stateless JWT contract; these checks do not establish refresh rotation, replay prevention or server-side logout revocation.

## Automated tests and Android status

- Backend `mvnw.cmd -o -Dtest=!OptionalFilterQueryTest test`: **279 passed**, zero failures/errors.
- PostgreSQL-specific `OptionalFilterQueryTest#search*`, explicit datasource127.0.0.1:5432/kilivana, schema validate, seeders/SQL initialization disabled and default read-only connection: **5 passed**, zero failures/errors. Verified no original record or sequence changes before smoke writes.
- Total executed backend cases: **284 passed**. Two OptionalFilterQueryTest insertion cases were deliberately not run against this existing database: they insert an order/audit log and advance nontransactional sequences even if rolled back. No claim that those two cases ran.
- Android `gradlew.bat :app:testDebugUnitTest -Pkilivana.demoInstall=true --offline --rerun-tasks`: **138 passed**, zero failures/errors; BUILD SUCCESSFUL,24 tasks executed. Existing API integration/source unchanged.
- ADB reported **no connected devices**, checked repeatedly including after API tests. No adb reverse, install, UI smoke or physical Android session restoration was performed. Historical physical-device results do not verify this new runtime.
- No website/admin frontend implementation or UI was tested.

## Evidence and remaining review

Private evidence directory: `<private-local-path>`. Includes the fresh backup manifest/archive/TOC, maintenance activity checks, preflight/migration/verification output, Hibernate results, backend test logs, startup wrapper output, original API fixture metadata, API/session result summaries, before/after preservation fingerprints and encrypted test credentials. Backend child console output is not fully captured by the launcher; health/listener, JVM flags, schema validation and successful actual API calls establish startup. Test/backup files containing private data must not be attached to a PR.

New shareable file: this report only. All previously existing integration documents, migration scripts and manifest remain unchanged. Scratch helpers/build outputs are ignored; no application source changed, no Git index changes, no commit/push and no Phase2 development.

The migrated local schema and reconciled backend are **ready for final functionality-parity review**. The observed farmer API paths pass. Before treating handover as fully verified: connect the phone for login with an existing account, existing data/images, creation/editing and actual Android process-restart restoration; review the retained smoke records and outstanding security/parity decisions; inspect Daniel's actual web/admin clients and perform their regressions. This is a local development milestone, not production readiness or completion of the entire migration roadmap.

Individual product approval remains **PENDING TEAM DECISION**; moderation/status transitions, visibility and farmer eligibility rules were not changed. Existing known refresh revocation/logout limitations, incomplete checkout, seller-safe order feeds, complete inspection/admin verification workflow, payments/logistics and cross-client parity remain review/next-phase items. No new functionality was implemented to conceal those gaps.

**Stopped for user review. No commit or push is authorized by this task.**
