# Kilivana Phase 1 — Canonical Backend Security and Stabilization

> Review copy: historical evidence from the tested feature branch based on `a55b56e`, not an audit of current upstream `main`. Workstation paths/device identifiers are omitted. Original local documents are preserved outside this commit scope. See [review guide](README.md).

Date: 2026-10-14. Canonical working tree based on `a55b56e`, branch `feature/android-backend-integration`. This is an incremental security milestone, **not complete marketplace migration or production readiness**. Read all five audits (06–10) and verified relevant findings against controller, service, repository, JWT and test source before implementation.

## Baseline and implementation checklist

Canonical backend source was clean before this phase. Existing modified `docs/integration/README.md`, untracked integration documents 02–10 and `scripts/` were preserved. Original/reference backend and Android already contained uncommitted work; their recorded statuses are appended below. No commit, push, merge, reset, branch deletion, application database creation, DDL, migration, seeder, record deletion or reference-backend modification occurred.

| Checklist tied to audit | Outcome |
|---|---|
| 1A Record all working trees; inspect current contracts/security; run safe baseline | Done. 214 backend tests passed before edits; database-dependent OptionalFilterQueryTest excluded. Existing original and Android changes preserved. |
| 1B Inspection ownership and aliases | Admin-only creation/list/filter/deletion; assigned active inspector or admin reads/updates/results/evidence. Non-admin alias cannot bypass service guards. Full assignment/report/review business workflow remains future work. |
| 1B Orders/payment/dispute/cart/address/notification/user scoping | Done for reviewed service boundaries, with negative proxy tests. Whole mixed-seller orders/payments restricted to buyer/admin; seller projection remains unavailable. Payment writes/refunds and dispute resolution admin-only. |
| 1B Indirect logistics-to-order bypass | Existing logistics service methods guarded: assigned driver/admin writes; buyer/admin or assigned driver reads. Global lists, assignment/cancellation/deletion admin-only. No new delivery state, fee or fulfillment logic. |
| 1C Product status escalation | Existing intended admin moderation permission enforced at ProductService.updateProductStatus for both owner-accessible and admin routes. Existing enum documentation and admin route establish admin moderation authority. Status values, transitions accepted by admin service, pending creation, browsing visibility and farmer verification eligibility unchanged. **Individual product approval remains PENDING TEAM DECISION.** |
| 1D Catalog prices and validated reservations | Catalog price authoritative; supplied unitPrice must match as reconciliation input. Positive quantities, duplicate lines, minimum order quantity, active product, self-order and stock invariants checked before writes. Product/order row locks serialize relevant reservation/update/cancel operations. |
| 1D Unresolved checkout/delivery-price authority | Checkout now explicitly503 instead of falsely claiming order success. Direct order placement admin-only until an authoritative delivery quote and real buyer checkout are approved. Existing admin entry with validated buyer/address and catalog prices retained. No invented fee/tax/commission policy. |
| 1E Access-account state and recovery enumeration | Access JWT must reference active existing account with current matching role; token types remain enforced. Forgot-password response no longer discloses existence, reset still disabled. No token/credential details added to logs. |
| 1E Durable refresh rotation/revocation/logout | **Blocked pending session design and schema approval.** Signed refresh tokens remain reusable; server logout remains no-op; Android local-only sign-out unchanged. No in-memory revocation workaround presented as durable security. |
| 1F Preserve farmer/client | No Android source changes necessary. Farmer login, registration/profile creation, account/profile phone, categories, listings, creation/editing/images and persistence/refresh exercised on physical phone after backend restart. Buyer/supplier/inspector demo screens preserved. |
| 1G Automated/live verification | 247 selected backend tests and 138 Android tests passed; schema validate startup, local identity/counts, PostgreSQL row-lock contention, physical smoke and separate-process refresh passed. Web/admin frontends not available/tested. |

## Security behavior and API compatibility

`security/ResourceAuthorization.java` reads the authenticated Long principal from Spring SecurityContext, loads active User and derives role/ownership from canonical records. Supplied user IDs only select records and are compared to that actor; they do not establish ownership. Service `@PreAuthorize` guards are applied through Spring method-security proxies, rather than protecting just one controller alias. Admins retain cross-user administrative operations. Method access denial returns the canonical error envelope with HTTP403 and code FORBIDDEN. Anonymous protected calls remain401. Missing/nonowned records may return403 for nonadmins without disclosing existence; admins retain normal not-found handling.

No endpoint URL, DTO field, ID format, enum serialization or auth token wire format was added or removed. Existing web/admin controllers/envelopes remain, but legitimate callers **must satisfy tightened authorization**; compatibility does not mean preserving unauthorized behavior. Actual web/admin UI regression remains unverified.

| Route group / behavior change | Current authorization / effect |
|---|---|
| `/inspections` and `/admin/inspections` | Both reach same guarded service. Assigned INSPECTOR/admin single-record/result/evidence access; admin create/general lists/status/target filters/delete; inspector-ID list must match actor or admin. Admin namespace continues ADMIN-only at HTTP filter. |
| `/orders` read/list/search/timeline/receipt | Own BUYER or ADMIN for whole-order reads/receipt; unfiltered or another-buyer listing admin-only. Status/cancel/delete/dispatch remain admin operations. Sellers cannot read all lines of mixed-seller orders; no replacement farmer feed introduced. |
| POST `/orders` | Admin-only while buyer quote/checkout rules unresolved. Existing buyerId must be a BUYER and address owned by that buyer. Request unitPrice is checked but server Product.price is used for totals and stored line snapshots. Incorrect prices/totals produce400. |
| `/payments` including initiate/webhook/status/refund | Record mutations admin-only. Existing fake webhook delegate is not promoted to a provider callback. Reads buyer-of-order/admin; safe seller-specific payment view remains separate work. |
| `/disputes` | Creation requires authenticated self raisedBy plus order participation; nonadmin cannot inject arbitrary starting status. ID reads creator/own buyer/admin; order/user lists scoped. Resolution/status mutations admin-only. Real provider refunds not implemented. |
| `/carts`, `/cart`, `/cart/items` | Service derives buyer/cart owner from records; item lookup through compatibility controller cannot bypass cart-owner guard. Checkout503 after ownership check; no order/payment created. |
| Address aliases under `/users` and `/api/v1` | Explicit userId and self aliases both constrained at AddressService. Supplied address must also belong to supplied userId; users cannot impersonate another address owner. |
| `/notifications`, admin/ecommerce aliases | Recipient/admin reads, mark-read/read-all/delete; global feed and notification creation/send operations admin-only. Self clients can use existing user-scoped/ecommerce feeds. |
| `/users/{id}` | Private account read self/admin. Existing profile own/admin guards retained. |
| PUT `/products/{id}/status`, PATCH `/admin/products/{id}/status` | ADMIN only at service. New products still pending; edits don't activate them; no product approval policy or public-filter change. |
| Product creation | FARMER/SUPPLIER self or existing admin authority; BUYER/INSPECTOR/DRIVER cannot masquerade as farmer sellers. No verified-farmer gate introduced. |
| Existing logistics job/tracking/POD aliases | Assigned DRIVER/admin mutations, scoped reads; unrelated users cannot change an order via logistics side effects. Global/search/ID-only proof or tracking queries without safe per-owner projection remain admin-only. |
| Access JWT | Same claims/Long ID/tokens, plus current account/role validation. Suspended/inactive/deleted or stale-role token fails authentication; login/refresh remain compatible. |
| Forgot/reset | Forgot retains exact generic success string for both known/unknown email without repository existence lookup; this does not mean a message was delivered. Reset continues503. |

## Inventory integrity and remaining limits

ProductRepository.findByIdForUpdate and OrderRepository.findByIdForUpdate use PESSIMISTIC_WRITE. Order placement holds product row locks in ascending ID order inside its transaction; validation/reconciliation precedes reservations and catalog line snapshots. Product edit and moderation writes use the same product lock; edits cannot lower total stock below current reservations. Cancellation uses an order lock and product locks when releasing stock; receipt/status/dispatch use order locking. No version column, new table, foreign key or automatic schema update was needed.

Unit tests verify catalog substitution rejection, invalid/duplicate/minimum/insufficient-stock rejection, locked repository use, and editing without erasing reservations or pending status. A real PostgreSQL probe held an existing product FOR UPDATE and a second transaction hit a500ms lock timeout; both transactions rolled back without row changes. This proves local PostgreSQL lock contention, **not a complete concurrent HTTP checkout stress test**. Buyer ordering remains unavailable until approved real checkout, delivery-price authority and full lifecycle/concurrency tests. Other admin dispute/payment/logistics state races, stock consumption across fulfillment and max+1 reference-code collisions remain broader lifecycle work; no production guarantee is claimed.

## Database/runtime verification

Only `127.0.0.1:5432/kilivana` accessed as `kilivana_app` using existing Windows-encrypted credentials; identity confirmed before row-lock probe. Public schema count35, categories19 (11 FARMER/8 SUPPLIER). Existing approved SQL fingerprints unchanged:

- 03 schema: `2DAB1C56B76B36B2C4AA2D94A83DC1B778259532406625A1BFCBBA2EDAE3042A`
- 04 categories: `0795CAD32E7ADB1F214AEB0645254606D819B89637B6348D4ABD6E4FF72A77D5`

Stopped only the verified canonical Java process listening on127.0.0.1:8081 (old PID <local-process>); restarted with unchanged secure launcher, new PID <local-process>. Actual health UP; GET categories/type/FARMER returned11. Running command verified ddl-auto=validate and SQL init=never. Successful validate-mode startup establishes mapped schema compatibility. Seeders remain disabled; existing reference process on8080 was not stopped. No production infrastructure or other database accessed.

Current implementation needs **no database migration**. Durable refresh sessions/revocation, report/decision history and any version/outbox/schema extensions need a separately reviewed migration and approval before execution. No migration file was created or run in this phase.

## Tests executed and evidence

| Check | Actual result |
|---|---|
| Baseline Maven: `mvnw.cmd -o -B -ntp -Dtest=*,!OptionalFilterQueryTest test`, JDK21 | 214 tests,0 failures/errors/skips. `target/phase1-baseline-tests.log` |
| Final same selection after security changes | 247 tests,0 failures/errors/skips. `target/phase1-security-tests.log` and Surefire XML |
| New service-proxy/alias authorization tests | ServiceAuthorizationTest18: cross-user inspection/orders/payments/disputes/cart/addresses/notifications/private user/product moderation/creation/logistics, positive owner/admin/assigned inspector/driver checks; HTTP403 for inspection/address aliases using real proxied services with mocked repositories |
| AccountAccessValidatorTest3, JwtAccountEnforcementTest4 | Active account accepted; suspended/inactive/missing/stale-role rejected; refresh credential cannot authenticate as access token |
| Price/stock/recovery tests | OrderServiceTest +4; ProductStockSafetyTest2; PasswordResetSecurityTest +2. All passed; existing order lifecycle fixtures updated for locked read |
| Android: `gradlew.bat :app:testDebugUnitTest -Pkilivana.demoInstall=true --offline --rerun-tasks` | 138 tests,0 failures/errors; task compiled debug Kotlin/Java and test sources. `FarmApp/app/build/phase1-unit-tests.log` |
| Physical Samsung <connected-test-device>, existing canonicaldemo APK, adb reverse8081 | CanonicalBackendDeviceTest#registerLoginProfileCategoriesProductImagesAndEdit: OK1. Real register/login/farmer profile/account phone/categories/own catalog/pending product/two PNG images+binary reads/edit |
| Force-stop then separate instrumentation process | #restoreSessionAfterProcessRestartAndRefresh: OK1. Existing encrypted session restored, profile read, real backend refresh and persisted tokens |
| Open app afterward | MainActivity cold launch Status:ok; package com.example.farmapp.canonicaldemo, component com.example.farmapp.canonicaldemo/com.example.farmapp.MainActivity |
| Real unauthenticated APIs | Orders/inspections/notifications401; public product list0 ACTIVE (pending fixtures not promoted) |
| Local PostgreSQL identity/schema/lock probe | Expected identity/counts and row lock contention passed; transactions rolled back |

Physical smoke intentionally added one new local farmer/profile/product and two database images to verify changed security against real mutations. No existing data deleted or reset; no old test suite initialization repeated. Subsequent session test creates no additional account/product. No Android source changes were required, and original/reference plus Android source/schema hashes match the earlier audit snapshot. Existing 703-file snapshot was used to verify foreign-source preservation; canonical changes are the explicit file list below.

Intermediate compile/test failures were resolved before runtime restart: historical order-price mapping replacement corrected; test annotation/Order import fixed; cancellation/receipt mocks adapted to new locked reads. Final passing counts supersede those intermediate runs. Database-dependent OptionalFilterQueryTest was not run because it uses configured PostgreSQL rather than a safe disposable test database. Web/admin UI, real payment provider and full concurrent-order integration were not tested.

## Exact changed files and purpose

All following files are in `kilivana-shared-backend`; paths are relative to this repository. Existing README, SQL proposals, audits02–10 and secure scripts were not edited by this phase.

| File | Change |
|---|---|
| `src/main/java/com/kilivana/backend/security/ResourceAuthorization.java` (new) | Active database actor and owner/role predicates for guarded services |
| `src/main/java/com/kilivana/backend/security/AccountAccessValidator.java` (new) | Current account status and role check for access JWT |
| `src/main/java/com/kilivana/backend/security/JwtAuthenticationFilter.java` | Invoke validator; no exception detail in rejection log |
| `src/main/java/com/kilivana/backend/config/SecurityConfig.java` | Wire validator into existing JWT filter |
| `src/main/java/com/kilivana/backend/common/exception/GlobalExceptionHandler.java` | Canonical403 handling for method AccessDeniedException |
| `src/main/java/com/kilivana/backend/common/service/AuthService.java` | Remove forgot-password existence disclosure; reset/lifecycle wire retained |
| `src/main/java/com/kilivana/backend/admin/service/InspectionService.java` | Admin/assigned inspector method guards |
| `src/main/java/com/kilivana/backend/admin/service/AddressService.java` | Self/admin service ownership across aliases |
| `src/main/java/com/kilivana/backend/admin/service/NotificationService.java` | Recipient/admin guards; admin-only global/write operations |
| `src/main/java/com/kilivana/backend/admin/service/UserService.java` | Private account read self/admin |
| `src/main/java/com/kilivana/backend/ecommerce/service/OrderService.java` | Owner/admin guards, fail-closed buyer placement, catalog snapshots/validation/reservation and mutation locking |
| `src/main/java/com/kilivana/backend/ecommerce/service/PaymentService.java` | Admin-only record mutations and scoped buyer/admin reads/mapping |
| `src/main/java/com/kilivana/backend/ecommerce/service/DisputeService.java` | Participant/self creation and scoped read, admin resolution/status |
| `src/main/java/com/kilivana/backend/ecommerce/service/CartService.java` | Buyer/cart-owner guards; explicit unavailable checkout |
| `src/main/java/com/kilivana/backend/ecommerce/service/EcommerceNotificationService.java` | Admin sending and self/admin recipient reads |
| `src/main/java/com/kilivana/backend/ecommerce/service/ProductService.java` | Seller creation/admin moderation permission; locked stock edit/moderation and reservation floor |
| `src/main/java/com/kilivana/backend/ecommerce/repository/ProductRepository.java` | Locked product query |
| `src/main/java/com/kilivana/backend/ecommerce/repository/OrderRepository.java` | Locked order query |
| `src/main/java/com/kilivana/backend/logistics/service/LogisticsService.java` | Guard existing indirect order mutation/read methods; no new logistics behavior |
| `src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java` (new) | Real Spring proxy ownership/admin/role/alias regression suite |
| `src/test/java/com/kilivana/backend/security/AccountAccessValidatorTest.java` (new) | Current account/role validation |
| `src/test/java/com/kilivana/backend/security/JwtAccountEnforcementTest.java` (new) | Filter invokes current account validation/type enforcement |
| `src/test/java/com/kilivana/backend/ecommerce/service/ProductStockSafetyTest.java` (new) | Preserve reserved stock/pending status during edits |
| `src/test/java/com/kilivana/backend/ecommerce/service/OrderServiceTest.java` | Catalog/quantity/reservation tests and locked-read fixtures |
| `src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java` | Generic forgot response/no account lookup regression |
| `src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java` | Mock account validator in isolated MVC slice |
| `src/test/java/com/kilivana/backend/common/controller/HealthControllerTest.java` | Same isolated dependency wiring |
| `src/test/java/com/kilivana/backend/admin/controller/ProfileControllerTest.java` | Same; existing profile CRUD/security tests retained |
| `src/test/java/com/kilivana/backend/logistics/controller/DriverProfileControllerTest.java` | Same; existing driver tests retained |
| `docs/integration/11-phase1-security-implementation-report.md` (new) | Checklist, baseline, changes, behavior, evidence and blockers |

Build/runtime logs under ignored target/app/build are verification artifacts, not proposed source commits. No credentials/tokens/real user data are included here.

## Remaining security risks and approval blockers

1. Durable refresh rotation, replay-family revocation and logout are still absent. Agree token lifetime, one/all-device logout and legacy-client transition, then approve additive canonical session storage migration. Current Android local sign-out is truthful; don't claim secure server logout.
2. Private inspection/KYC evidence retrieval remains unresolved: public database/local/Cloudinary image URLs are not private authorization. New record/evidence-method ownership checks do not retroactively protect public binaries. Approve private storage/retrieval/retention policy before real sensitive inspector rollout.
3. Individual product approval **PENDING TEAM DECISION**. Only existing admin permissions tightened; no pending/visibility/purchasing/verification rule changed. Public detail/seller/category still expose statuses as previously implemented; reviewed visibility policy required for changes.
4. Full inspection assignment validation, inspector qualification, structured submitted reports/freeze and final admin review with retained reason/actor/history remain incomplete. Recommendation still does not verify farmer. No new verification workflow implemented.
5. Real buyer checkout and authoritative delivery quote require policy/contract approval; order placement restricted to admin and checkout503 meanwhile. Seller-safe orders/payments, safe unlisting, payment provider/callback/refund execution and complete fulfillment inventory/state reconciliation remain subsequent work.
6. Full concurrency/transaction and cross-client testing needs an approved isolated integration-test environment and actual web/admin implementations. No new database was created. Row-lock probe plus unit tests is bounded evidence, not production stress certification.
7. Category mutation governance, profile self-editable governance fields, public image privacy, entity-wide audit logging, rate limiting, scalar relationship/FK integrity and other audit findings outside these focused fixes still need follow-up. In particular new account-state checks do not revoke tokens after password changes while the account remains active with same role.

Next recommendation: review this diff and tightened authorization with Daniel's actual clients; then approve the durable-session/private-evidence/inspection-review design and migrations in separate small steps. Do not enable real buyer/inspector commerce or retire the original backend based solely on this phase. No commit/push/share/merge has been performed.
