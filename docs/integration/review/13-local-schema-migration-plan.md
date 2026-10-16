> Historical local-development evidence, sanitized for review. Private paths are omitted. Product approval is now mandatory; see [current policy](../18-product-approval-decision-and-verification.md). This document does not authorize SQL execution.

# Kilivana — safe local PostgreSQL schema migration proposal

**REVIEW ONLY — NOT APPROVED FOR EXECUTION.** Prepared 2026-10-15 against canonical commit `8f9dded09f84912fd868b450d61985f240305164`. No application source, existing migrations, database schema/data, running backend, Git index or commits were changed by this task. Individual product approval remains **PENDING TEAM DECISION**.

## Verified environment and existing data

Read-only queries confirmed PostgreSQL 18.1, TCP `127.0.0.1:5432/kilivana`, role `kilivana_app`, timezone `Africa/Nairobi`. Public schema: **35 tables, 35 bigint sequences, 61 indexes, 18 non-PK unique constraints, no foreign keys**. The role owns all application tables/sequences; public schema owner is `pg_database_owner`, with CREATE/USAGE granted to the app. It has SELECT, sequence access, TEMP and REFERENCES on orders. It is neither superuser nor CREATEDB/CREATEROLE. No administrator credentials were used. These checks establish catalog privileges, not an executed DDL/backup/restore test.

Aggregate inventory: 5 users, 5 farmer profiles, 5 products, 19 categories, 8 product-image links, 8 stored-image rows and 8 PostgreSQL large objects. All eight image objects are app-owned/readable; byte lengths match; no image row points to a missing object. All five listings are PENDING_APPROVAL. Orders, order items, payments, inspections, logistics jobs and farms are currently empty; all other tables are empty. Zero null/orphan order-item references. No personal account fields, image bytes, tokens or credential values were exported to this report.

A complete structural signature covers all public table columns/types/nullability/defaults, constraint definitions/validation, index definitions/validity and sequence definitions: `7a3b48703b3f4a7e711a98bc08f0aca6`. The review preflight rejects a different baseline rather than silently adapting it. Data counts are observations, not required fixed values; valid newly created farmer records may remain.

## Complete expected-schema comparison

The current compiled 37-entity Hibernate model was regenerated offline with connections forbidden. Every existing mapped column's PostgreSQL type and nullability matches. Existing primary keys, 18 mapped unique constraints, sequence ID types, non-tracking mapped indexes and all remaining enum checks match. No extra persisted columns were found. Existing ID sequence defaults match IDENTITY generation; there are no other existing column defaults. Preserve the extra `uk_users_email_lower` index, `ux_proof_of_deliveries_job` index and driver suspension-reason check; do not replay historical migrations that normalize or delete data.

### Two missing tracking tables

Exact definitions are in [V16](../../../db/migrations/V16__reconciled_tracking_schema.sql).

| Table | Required columns | Nullable columns |
|---|---|---|
| driver_locations | id BIGSERIAL PK; logistics_job_id BIGINT; driver_id BIGINT; latitude/longitude DOUBLE PRECISION; recorded_at TIMESTAMP(6) WITHOUT TIME ZONE | speed_kmh, bearing, accuracy_metres DOUBLE PRECISION; client_timestamp TIMESTAMP(6) WITHOUT TIME ZONE |
| driver_latest_location | logistics_job_id BIGINT PK; driver_id BIGINT; latitude/longitude DOUBLE PRECISION | speed_kmh, bearing, accuracy_metres DOUBLE PRECISION; location_id BIGINT; recorded_at/client_timestamp TIMESTAMP(6) WITHOUT TIME ZONE |

Only driver_locations.id generates a sequence; the latest-location PK is supplied by the service. Both tables start empty. Two history indexes: `(logistics_job_id, recorded_at)` and `(driver_id, recorded_at)`. No foreign keys, cascades, retention deletion, coordinate constraints, inferred coordinates or timestamp defaults are introduced. Scalar ID relationships match current entities.

### Six missing payment columns

All nullable, **no defaults or historical backfill**:

| Columns | PostgreSQL type |
|---|---|
| merchant_request_id, checkout_request_id, phone_number, mpesa_receipt_number, callback_status | VARCHAR(255) |
| transaction_date | TIMESTAMP(6) WITHOUT TIME ZONE |

[V17](../../../db/migrations/V17__reconciled_product_payment_fields.sql) adds three lookup indexes (checkout_request_id, merchant_request_id, phone_number) and partial UNIQUE receipt index `WHERE mpesa_receipt_number IS NOT NULL`, preserving V15's callback-index intent. NULL receipt values remain legal. Duplicate non-null receipts would be rejected for future records, as upstream intended; no receipts are fabricated. No callback-status CHECK is added: the entity uses a nullable String, not a mapped enum.

### Eleven missing product columns

All nullable, **no defaults, data rewrite or invented farmer/product metadata**:

| Columns | PostgreSQL type |
|---|---|
| farm_id, last_inspection_id, status_changed_by, verified_by | BIGINT |
| harvest_date | DATE |
| low_stock_threshold, verified_qty | INTEGER |
| quality_grade | VARCHAR(255) |
| status_changed_at, verified_at | TIMESTAMP(6) WITHOUT TIME ZONE |
| status_reason | TEXT |

V17 adds `products_quality_grade_check`: GRADE_A, GRADE_B, GRADE_C, EXPORT, ORGANIC; NULL remains permitted. No FK is invented for scalar product IDs, and no inspection/farm/administrator is assigned to a historical record. All product statuses, seller/category IDs, prices, stock, reserved stock and image associations remain unchanged. NULL quality/verification fields mean unknown/not recorded, not approved or failed.

### Additional confirmed differences

[V18](../../../db/migrations/V18__reconciled_payment_checks_order_item_fk.sql):

1. `orders_payment_status_check` currently permits PENDING/PAID/HELD/SETTLED/REFUNDED; current Java enum also permits FAILED. Replace with a six-state superset; keep all old values.
2. `payments_status_check` has the same five-state gap; expand identically. No UPDATE or reinterpretation of payment/order state.
3. Missing mapped FK: `order_items.order_id -> orders.id`, named `fkbioxgbv59vetrxe0ejfubep1w`, validated, NO ACTION on update/delete. Existing order_id NOT NULL is already correct. Reject orphan data instead of inventing parents or removing lines. No cascading delete or extra business relationship is introduced.

Expected post-state: **37 tables, 36 sequences, 69 indexes** (six secondary indexes plus two new PK indexes). Existing table data, original 35 sequence values and image bytes must be identical. A new quality-grade check and mapped FK are added; the two payment checks are expanded. Product status checks are untouched.

## Why not execute V14/V15 directly?

The project uses manual `db/migrations/V<number>__description.sql` scripts, not Flyway/Liquibase; see root README's Database migrations section and pom.xml. Default application configuration still says ddl-auto=update; the secure local launcher overrides it to **validate**, which must remain enforced.

- Historical V14 declares TIMESTAMPTZ and a NOW default, but both tracking entities use LocalDateTime and Hibernate exports TIMESTAMP(6) WITHOUT TIME ZONE, with application-generated recordedAt. V16 proposes the entity-compatible representation for these currently nonexistent/empty tables. No existing timestamp is converted. Cross-environment timezone standardization is a separate decision; environments that already ran V14 require their own review, not this script.
- Historical V15 narrows strings to 20/50/100 and adds a PENDING callback default, while current entities use default-length 255 strings and no database default. V17 matches the current mappings and leaves unknown historical callback state NULL.
- V11 rewrites statuses, V12 backfills references, V4 removes duplicates, and other older scripts also mutate data. **Do not replay historical migrations blindly.**

The proposed V16–V18 sequence is a cumulative forward repair for this exact verified local baseline, not a general replacement for all environments' histories. Do not edit Daniel's V14/V15. Confirm version numbers remain available before sharing upstream. The scripts intentionally fail on replay/partial/unexpected schema; they do not hide discrepancies behind IF NOT EXISTS. No new migration ledger or application-specific schema is created.

## Proposed files and safeguards

- V16: tracking tables and two indexes.
- V17: 17 nullable columns, four payment indexes, nullable quality-grade check.
- V18: two expanded payment checks and order-item FK.
- [preflight.sql](../schema-review/preflight.sql): read-only identity, role, permissions, 35-table signature, orphan and enum checks.
- [preservation-query.sql](../schema-review/preservation-query.sql): read-only aggregate fingerprints of every original table, excluding only newly added fields; original sequence state and actual image large-object bytes included. No raw rows are emitted.
- [verify.sql](../schema-review/verify.sql): read-only checks for every mapped column/type/nullability, 37 tables, new indexes, receipt uniqueness, payment enum values and validated FK.
- [apply-reviewed-schema.psql](../schema-review/apply-reviewed-schema.psql): **unexecuted** single-transaction driver. Identity/signature preflight before and after acquiring locks; nonblocking advisory guard; lock timeout 5 seconds and statement timeout 120 seconds; automatic pre/post preservation comparison before COMMIT. Each version file rejects invocation outside this driver.
- [SHA256SUMS.txt](../schema-review/SHA256SUMS.txt): review fingerprints of proposed SQL/driver files.

DDL obtains short exclusive locks on order_items/orders/payments/products. Ordinary non-concurrent indexes are transaction-safe and suitable for this tiny local dataset. Use a maintenance window after clone testing; if clients hold locks, abort and reschedule instead of terminating sessions. All changes are in one transaction; there is no committed gap between dropping and recreating payment checks. Existing record changes, sequence changes or altered image bytes cause rollback. No application files/configuration are edited by the migration driver.

## Secure command setup — PROPOSED, not executed

Run the following in a private PowerShell terminal from the canonical repository only after the relevant approval. Passwords are imported from the Windows-encrypted file and placed only in each child process environment; not command arguments, Git files or console output.

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
function Invoke-KilivanaPg {
    param(
        [ValidateSet('psql','pg_dump','pg_restore')][string]$Tool,
        [string[]]$PgArgs,
        [switch]$ReadOnly
    )
    $credential = (Import-Clixml -LiteralPath '<private-local-path>').DatabaseCredential
    if ($credential.UserName -ne 'kilivana_app') { throw 'Unexpected database account' }
    $start = [Diagnostics.ProcessStartInfo]::new((Join-Path $pgBin ($Tool + '.exe')))
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.Environment['PGPASSWORD'] = $credential.GetNetworkCredential().Password
    $start.Environment['PGOPTIONS'] = if ($ReadOnly) { '-c default_transaction_read_only=on' } else { '' }
    foreach ($argument in $PgArgs) { $start.ArgumentList.Add($argument) }
    try {
        $process = [Diagnostics.Process]::Start($start)
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "$Tool failed with exit code $($process.ExitCode); stop" }
    } finally {
        $start.Environment.Remove('PGPASSWORD') | Out-Null
        $credential = $null
    }
}
```

## Backup and recovery — before any approved application

1. Arrange a maintenance window and quiesce client writes only after separate execution approval. Do not stop the currently working backend during this planning task. Record the old running build/classpath/config privately; leave DB credentials and JWT signing secret unchanged. Record current image URLs/IDs privately for comparison.
2. Use PostgreSQL 18 pg_dump, custom format, entire database and **large objects included**. Table-only or data-only backups can lose the eight OID-backed images or schema. Preserve external/local image files separately if any provider is in use; no such files are modified by this SQL. Store backups outside Git with Windows permissions restricted to the owner. Dump contains personal data and password hashes; never attach it to a PR.
3. Proposed commands (directories/archive are newly named; refuse overwrite):

```powershell
$backupDir = Join-Path $env:LOCALAPPDATA 'Kilivana\Backups'
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
# Confirm this directory is private to your Windows account before exporting.
$backup = Join-Path $backupDir ('kilivana-before-v16-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump')
if (Test-Path -LiteralPath $backup) { throw 'Backup already exists' }
Invoke-KilivanaPg -Tool pg_dump -ReadOnly -PgArgs @(
    '-h','127.0.0.1','-p','5432','-U','kilivana_app','-d','kilivana','-w',
    '--format=custom','--large-objects','--file',$backup
)
Get-FileHash -LiteralPath $backup -Algorithm SHA256
Invoke-KilivanaPg -Tool pg_restore -PgArgs @('--list',$backup)
```

A successful dump/list and checksum are necessary but not sufficient: restore-test the archive and retrieve the images. Record backup checksum, SQL checksums, commit and execution output privately. If writes resume after the rehearsal backup, take a fresh final backup after quiescing and repeat baseline checks before application.

**Recovery:** a SQL error, timeout, guard failure or preservation failure aborts the single psql connection transaction; PostgreSQL rolls back DDL and constraints. Do not retry blindly: inspect identity, locks and current schema first. If COMMIT succeeds but startup fails, preserve the migrated database and diagnose it; prefer a forward correction or controlled application rollback after compatibility review. New schema is additive except expanded checks, but older binaries may not understand new FAILED records. Do not undo data, DROP columns/tables, restore over the live database, use pg_restore --clean, or delete/truncate anything. For a backup recovery, restore into a separately approved empty recovery target, verify rows/images and explicitly approve any cutover. Restoring an older backup loses later writes; maintain the original and plan recovery-point reconciliation. No destructive down migration is supplied.

## Isolated rehearsal — must precede local database changes

**Not performed. No copy or new database was created.** `kilivana_app` lacks CREATEDB, so provisioning an isolated empty PostgreSQL 18 instance/database is a prerequisite requiring separate approval/operator setup. Do not elevate it or use administrator credentials in this workflow. Use a native loopback instance on **127.0.0.1:55432**, database still named `kilivana`, with app role owning restored objects and having public CREATE/USAGE/TEMP. No permanent Android-specific database is proposed. A container whose server-reported address/port differs will intentionally fail these guards and requires its own reviewed target configuration.

Before restore, verify current_database/current_user/server address/port and zero public application tables on the isolated target. Never reuse the live 5432 arguments for restore. The isolated database role must be provisioned to authenticate securely; the commands assume the encrypted local app credential works for that app role.

```powershell
# Only after isolated-target provisioning and restore approval; assert empty public schema first.
Invoke-KilivanaPg -Tool psql -ReadOnly -PgArgs @(
    '-h','127.0.0.1','-p','55432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-c',
    "SELECT current_database(),current_user,inet_server_addr(),inet_server_port(); SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';"
)
Invoke-KilivanaPg -Tool pg_restore -PgArgs @(
    '-h','127.0.0.1','-p','55432','-U','kilivana_app','-d','kilivana','-w',
    '--no-owner','--no-privileges','--exit-on-error','--single-transaction',$backup
)
Invoke-KilivanaPg -Tool psql -ReadOnly -PgArgs @(
    '-h','127.0.0.1','-p','55432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-v','expected_port=55432','-f','docs/integration/schema-review/preflight.sql'
)
# This is DDL on the COPY and still needs explicit rehearsal execution approval.
Invoke-KilivanaPg -Tool psql -PgArgs @(
    '-h','127.0.0.1','-p','55432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-v','expected_port=55432','-f','docs/integration/schema-review/apply-reviewed-schema.psql'
)
Invoke-KilivanaPg -Tool psql -ReadOnly -PgArgs @(
    '-h','127.0.0.1','-p','55432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-v','expected_port=55432','-f','docs/integration/schema-review/verify.sql'
)
```

On the copy: compare restored counts and actual image content with the archive/source baseline; run Hibernate validation with SQL initialization/seeders disabled, schema actions disabled except validate; then run a separately configured backend on 127.0.0.1:8082 pointing ONLY at 55432. The existing launcher hardcodes 5432/8081: **do not use it for the copy or edit it silently**. Prepare a separately reviewed test launcher with explicit isolated URL/port overrides, fresh runtime dependency classpath, same JWT secret for restoration tests, database image provider and outbound mail/payment calls disabled/blocked. Existing working 8081 stays untouched. Do not accidentally redirect the physical app to the copy without explicit testing approval.

Rehearsal acceptance: all mapped columns validate, expanded checks/FK/indexes present, every original record/sequence/image fingerprint unchanged, existing farmer login/profile/listings/image GETs succeed, pending products remain pending, no external callbacks/STK calls occur. Test failure rollback/timeouts and rejected replay against separately provisioned rehearsal targets; do not inject failures into the live local database. A successful schema check alone does not prove app functionality.

## Exact proposed local migration commands — ONLY after approval and successful rehearsal

First confirm all seven SQL/driver hashes still match SHA256SUMS.txt, role/schema identity still matches, backup is restorable, and clients are quiesced. Run from canonical repository; no standalone historical script execution and no Hibernate update.

```powershell
Invoke-KilivanaPg -Tool psql -ReadOnly -PgArgs @(
    '-h','127.0.0.1','-p','5432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-v','expected_port=5432','-f','docs/integration/schema-review/preflight.sql'
)
Invoke-KilivanaPg -Tool psql -PgArgs @(
    '-h','127.0.0.1','-p','5432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-v','expected_port=5432','-f','docs/integration/schema-review/apply-reviewed-schema.psql'
)
Invoke-KilivanaPg -Tool psql -ReadOnly -PgArgs @(
    '-h','127.0.0.1','-p','5432','-U','kilivana_app','-d','kilivana','-w','-X',
    '-v','ON_ERROR_STOP=1','-v','expected_port=5432','-f','docs/integration/schema-review/verify.sql'
)
```

Do not add `--single-transaction` to this psql command: the driver already owns BEGIN/COMMIT. With ON_ERROR_STOP a failure exits and rolls back the open transaction. No schema-changing command above has been executed.

## Backend and Android verification after approved execution

1. Preserve successful migration output, signatures and fresh full backup. Confirm 37 tables, 36 sequences, 69 indexes; required constraints validated; unchanged five pending listings and original image bytes. Use preservation-query.sql and the driver's pre/post comparison; verify category/user/profile counts against the final pre-migration baseline.
2. Build/test reconciled backend on JDK17 without running DB-dependent tests against the wrong target. Phase 1 evidence is 279 backend and 138 Android tests from reconciliation, not migration execution evidence. Run the real-database context test only with explicitly reviewed target settings.
3. Refresh the local runtime classpath for upstream WebSocket/WebFlux dependencies. Run full Hibernate validate with SQL init never, all schema generation disabled except validate and all seeders disabled. Do not use default ddl-auto=update. Resolve further configuration errors before claiming startup success.
4. Only after separate maintenance/startup approval, stop the known old canonical PID gracefully, leave reference port8080 alone, and invoke the existing secure launcher for **127.0.0.1:8081 / 127.0.0.1:5432/kilivana**. Keep DB role and JWT secret unchanged. Verify logged build/current schema and listener address. No restart is authorized by this planning task.
5. Verify GET categories, existing farmer login, own profile reads, seller listings and existing product/image GETs. Use existing demo account privately; never log passwords/JWTs or expose response PII. Confirm original product/category/user IDs, exact money, integer/reserved quantities, pending moderation state and eight image links/objects are unchanged.
6. On connected phone: `adb reverse tcp:8081 tcp:8081`, open `com.example.farmapp.canonicaldemo/com.example.farmapp.MainActivity`. Log in, view existing crops and their photos. Force-stop/reopen to check encrypted session restoration/refresh without changing JWT signing secret. Existing profile and crop edits/image mutations are separate explicit smoke-test writes; use marked fixtures only after testing approval, not original data. Do not automatically register duplicate users.
7. Test new registration/product creation/image uploads only on the isolated copy first; no bypass of pending approval. Confirm unauthorized access/status escalation stays denied. Do not test payments by calling real Daraja, create orders on behalf of users, or change product eligibility.
8. Report automated checks, live API checks and physical-device results separately. Web/admin clients remain unverified until actually tested. Do not start Phase 2, commit or push.

## Remaining decisions and honest limits

- Approval is required for rehearsal provisioning/restore/DDL, then separately for the final local execution and maintenance restart. No production/staging environment is in scope.
- Confirm entity-compatible timestamp-without-time-zone and nullable 255-character/no-callback-default choice for this baseline; V14/V15 differences are not silently migrated on other environments.
- Broader UTC/Instant strategy, global FK policy for scalar IDs, role separation for deployment migrations, payment callback business semantics and product approval remain separate decisions. **Product approval is PENDING TEAM DECISION.**
- The SQL/driver has not been applied even in a rollback-only DDL transaction, nor tested on a copy. Only read-only catalog/privilege/image checks, the read-only preflight and preservation query have been run. An isolated restore/rehearsal and backend validate are required before calling the migration tested.
- Backup command options were checked against installed PostgreSQL18 tools; no backup archive was created by this planning task. No schema-changing SQL, application restart, commit or push occurred.
