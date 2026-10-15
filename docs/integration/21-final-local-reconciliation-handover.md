# Final local reconciliation and handover checkpoint

2026-10-15. Branch `feature/android-backend-integration`; upstream snapshot `4679d49d5881d8a3b870c6efa6a09e3607179810`. Local reconciliation only: no push, PR, deployment, development-database connection or migration execution.

## Recovery and curated commits

Created recovery branch `backup/pre-final-handover-20261015-165737` at original HEAD `8f9dded09f84912fd868b450d61985f240305164`. A private owner-ACL-restricted archive outside Git preserves all9 modified tracked files,32 untracked files and ignored artifacts:625 files verified individually by SHA-256 and ZIP integrity. Archive SHA-256: `9eee86f77742c3627f7ea40a57456c0f25f8b29efcd310053f67ca9678dd219a`. Its private location is retained in ignored `target/handover-backup-location.txt`; archive contents are not for sharing.

Three traceable commits precede this merge:

- `419b354` — reviewed catalog visibility/category security and regression tests.
- `9a6dca9` — reviewed V16–V18 and guarded verification tooling.
- `a2bd2c3` — curated handover documents and sanitized copies of local migration/rehearsal evidence.

Local original evidence documents and workstation scripts remain untracked and unchanged; none was discarded. The upstream editor backup ProductRepository.java.bak was removed from the index only and remains as a local untracked file. Logs, credentials, dumps, generated builds and live smoke fixtures are not staged.

## Merge resolutions and upstream preservation

Three textual conflicts were resolved individually:

| File | Resolution |
|---|---|
| StoredImageController.java | Retain approved/owner/admin product-image authorization and no-store caching; use database-storage documentation, not the obsolete claim that sequential IDs are private |
| SecurityConfig.java | Retain method-specific public GET/HEAD/POST matchers, category ADMIN enforcement, OPTIONS and account-aware JWT wiring; add Daniel's public farmer-profile GET route |
| PaymentService.java | Retain sellerPayment authorization and admin/record guards; accept upstream typed UserRole signature, UserRepository dependency and buyer-phone fallback; retain actual Daraja/STK implementation |

Automatically merged files were also inspected: ProductService retains visibility, owner pending reads, category restriction, row locking and pending creation while adopting imageStorage naming. InspectionService retains assigned-inspector/admin guards. ProductRepository retains pessimistic stock locks alongside Daniel's native optional-filter SQL. Profile/farm image response improvements, public farmer profiles, tracking timestamp fixes, trips endpoints and database/local storage changes are retained. Removed Cloudinary implementation/dependency/tests follow Daniel's upstream direction; no reference-backend payment stub was introduced.

Removed newly upstream-added authorization-header and raw-token logging from JwtAuthenticationFilter, retaining current-account checks. Blank placeholders replace the three non-placeholder Daraja credential values in .env.example. No credential values were printed or tested. **Daniel must treat upstream credential values as potentially exposed and rotate them if genuine.** They remain in existing upstream history; no history was rewritten. Sanitizing the final tree does not revoke credentials.

Updated the new public-farmer controller test with the account-validator dependency required by the merged security configuration. Existing authorization/stock/catalog tests continue to run. Live Daraja tests now require explicit `KILIVANA_RUN_LIVE_DARAJA_TESTS=true` in addition to credentials; no live provider operation is authorized by ordinary test execution.

## Schema and migration ordering

Reviewed V16/V17/V18 schema migration bodies and all seven checksum-covered scripts/helpers remain byte-identical to the successful rehearsal. Git initially normalized line endings in three helpers; exact original bytes were restored to the index. Scoped .gitattributes entries disable conversion and allow reviewed CRLF in diff checks. Both working bytes and final staged/committed blobs must match SHA256SUMS.txt; do not regenerate the manifest to hide a change.

Daniel's `db/migrations/V16__seed_short_distance_order.sql` is preserved under `db/demo/short_distance_order.sql`, with only trailing whitespace normalized. Its README explicitly marks it archived, optional and unsuitable for execution as-is: fixed IDs, dummy authentication/OTP data, fabricated pre-approved states and legacy columns. No SQL statement/value was changed or executed. Inspected pom/deployment/startup configuration contains no Flyway/Liquibase runner or reference executing that seed; documented manual migration commands reference older named scripts. This separation changes no configured deployment command and removes the version collision. There is one versioned schema file per migration version.

Do not run historical migrations, the optional seed or the local baseline-specific migration driver against another environment without a new review. Default application ddl-auto=update remains a pre-existing risk; tests explicitly use validate and SQL initialization never. No new schema change or migration is required by this reconciliation based on isolated Hibernate validation.

## Verified semantic query correction

The added isolated PostgreSQL regression reproduced an upstream native-query issue: filtering by ProductStatus returned no matching ACTIVE fixture because Hibernate bound the enum rather than its name. The existing findFilteredProducts enum-facing method is retained as a default wrapper; the native query receives an explicit String status.name() through findFilteredProductsByStatusName. Daniel's null casts/count query and controller contracts remain intact. Tests cover omitted/individual filters and exact enum matching with page counts. No product status or approval policy changed.

## Test evidence

- Final database-free command: `mvnw.cmd -o "-Dtest=!OptionalFilterQueryTest" test`, JDK17, live Daraja opt-in false. **304 passed,0 failures/errors;3 live-provider tests skipped (307 reported cases).** Newly upstream public-farmer tests ran. The prior314 count is not expected after removal of the Cloudinary tests.
- Initial offline build stopped on an uncached HttpClient dependency after the dependency graph changed; an approved online run downloaded it. The final run succeeded offline.
- Isolated PostgreSQL: verified PostgreSQL18, independent rehearsal data directory,127.0.0.1:55432/kilivana, restricted kilivana_app role,37 application tables. Server/data-directory identity was checked before test connections. Port5432 was never contacted by this task.
- `OptionalFilterQueryTest`: **9 passed,0 failures/errors/skips**, including2 new admin-query cases. Spring JPA ran with ddl-auto=validate, SQL initialization never and seeders disabled. The class now requires explicit isolated-test opt-in and a55432 datasource URL, preventing a normal test invocation from falling back to the development database. Test inserts existed only in the isolated copy and rolled back.
- Preservation comparison: all35 original table fingerprints and image-byte fingerprint remained unchanged across71 observed entries; only3 isolated sequences (audit_logs,orders,products) advanced normally through test inserts. No original rows/images were changed. This is evidence for the test copy, not a new inspection of development data.
- The rehearsal server was cleanly stopped and no55432 listener remained. A Windows startup wrapper waited for its descendant server rather than returning; independent PID/data-directory/database checks established identity before tests. After clean shutdown its delayed check found the removed PID file. This control-wrapper issue did not bypass verification or fail a test.
- No live STK/OAuth/payment, Android device, website or Admin Portal test ran. The existing8081 backend process was not restarted or replaced. Test logs/private evidence remain outside the review tree or under ignored target/.

Total final executed automated cases across the two suites: **313 passed;3 external-provider cases intentionally skipped**. Earlier failed enum regression is corrected and its final rerun passed. Final diff/secret-path/credential-placeholder/manifest/conflict-marker checks are required before completing the merge commit.

## Remaining limitations and handover boundary

Every farmer-submitted product requires admin approval before public visibility. Pending creation, public ACTIVE-only reads, owner/admin pending access, image metadata/byte guards and admin-only status changes are retained. Editing an already approved product still needs the team's reapproval decision. Unlisting and sold-out functionality remain Daniel's handover requirements; none was implemented here.

Android defaults to `http://127.0.0.1:8081/api/v1/`. The Admin Portal uses `https://either-juvenile-progeny.ngrok-free.dev/api/v1/admin/products`. These URLs were not changed; their backend/database identity remains unverified. Do not infer shared records or deployment from matching route names or IDs.

Android's pending-image byte loader is token-free. Private pending database/local images now require authenticated same-origin owner/admin loading, so Android and relevant web/admin image transports need a coordinated change or reviewed private delivery design. Never forward bearer tokens to external image hosts. Removing Cloudinary code does not revoke pre-existing external URLs or cached/downloaded images. Current runtime/device image compatibility is not established by source tests.

Stateless refresh/logout revocation limits, disabled buyer checkout, safe seller order projections, complete inspection/admin review and other accepted transfer-or-defer items remain documented in17–20. This reconciliation does not complete those features or establish production readiness. The current backend is suitable for a review-branch handover with these limitations explicitly disclosed; deployment requires separate runtime/client/environment verification. No push or PR has been authorized.
