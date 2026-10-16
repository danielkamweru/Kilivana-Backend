# Phase 1 upstream reconciliation — 2026-10-15

Code reconciliation is complete; live verification of the reconciled build is BLOCKED by the existing local database schema. No schema changes, seeders, database migrations, remote pushes, pull requests or Phase 2 work were performed. Historical device evidence remains historical and does not verify this merged build.

## Recoverable Git baseline

- Branch: `feature/android-backend-integration`.
- Original feature HEAD: `f66af627efac5fab873bc9566be34ed4ddfb97bb`.
- Backup: `backup/phase1-before-main-20261015`, pointing to that original HEAD.
- Fetched Daniel main: `af453d3eab2de319e3c4d002739b7f90acf71e6d`.
- Local main remains `16e32929bd609916a8de513f2b20f6f21af48a91`.
- Merge procedure: `git merge --no-commit --no-ff origin/main`; no rebase, reset, amend or force push.
- Remote: `origin`, Daniel's `danielkamweru/Kilivana-Backend`, not a fork. Proposed PR base: `main`.
- Fourteen pre-existing local documentation/script files were copied and SHA-256 recorded under ignored `target/reconciliation-20261015-originals/`; all remain byte-for-byte unchanged and excluded from the merge commit. This includes the modified integration README and local launcher. The original/reference backend and Android source were not edited.

## Eight textual conflicts, individually resolved

All paths below are relative to `src/main/java/com/kilivana/backend/`.

| Conflict | Resolution |
|---|---|
| `admin/service/InspectionService.java` | Retain upstream inspection/evidence implementation and documentation together with the Phase 1 class admin default and assigned-inspector method guards. No final verification policy added. |
| `admin/service/NotificationService.java` | Combine upstream documentation with admin-default writes/global reads and recipient-scoped reads/updates. |
| `common/service/AuthService.java` | Preserve upstream authentication implementation but retain non-enumerating recovery response without account lookup and disabled reset (503). Upstream lookup would disclose unknown accounts; email-only password replacement remains unavailable. |
| `config/SecurityConfig.java` | Keep Daniel's request logging filter, WebSocket route and filter-chain changes; construct JWT filter with current-account validator. |
| `ecommerce/service/CartService.java` | Preserve existing ownership guards and 503 checkout. Upstream checkout only changes cart state; it still does not atomically place an order or establish approved delivery pricing. Other upstream changes retained. |
| `ecommerce/service/DisputeService.java` | Combine upstream documentation with dispute/order-participant ownership guards and admin-only resolution. |
| `ecommerce/service/EcommerceNotificationService.java` | Combine upstream documentation with recipient-scoped read guards and admin-only global/creation operations. |
| `ecommerce/service/PaymentService.java` | Retain Daniel's complete STK initiation implementation and Daraja dependency, with Phase 1 admin defaults and buyer-owned payment reads. The read guard belongs to `getPaymentById`, not the newly inserted STK method. Give STK an explicit authenticated seller/role/order membership guard. |

## Additional semantic conflicts

1. **Account state consistency:** `AuthService.login` and `refreshToken` permit `PENDING_VERIFICATION`, rejecting suspended/inactive accounts. `AccountAccessValidator.permitsAuthentication` now explicitly accepts ACTIVE or PENDING_VERIFICATION; `ResourceAuthorization` uses the same predicate. Token-role changes, missing accounts and disabled states remain rejected. Farmer verification status is a separate field and is not changed. Regression coverage exercises pending farmer login, refresh, protected own-user access and rejection of product escalation. A legitimate seller also reaches upstream outstanding-balance validation rather than being blocked by the class admin default.
2. **New product moderation method:** `ProductService.updateProductStatusWithReason` now receives the same admin guard and pessimistic product lock as the original status method. Daniel's reason, actor and timestamp fields remain. No status transitions, visibility rules or eligibility rules were added or removed by this reconciliation. Individual product approval remains **PENDING TEAM DECISION**.
3. **STK service authorization:** `ResourceAuthorization.sellerPayment` derives the actor from server security context and current user record; supplied ID/role must match, role must be FARMER/SUPPLIER, and the order must contain that seller's lines. Provider calls were not executed. Full-order outstanding balance, phone rules, rounding and callback behavior remain upstream behavior requiring payment-specific review/testing.
4. **WebSocket authentication:** upstream `JwtHandshakeInterceptor` now applies the same current-account and token-role validation as HTTP before storing a session identity. Rejected token exception contents are no longer logged.
5. **Tracking destinations:** `TrackingChannelInterceptor` rejects destination-bearing messages without authenticated identity and another user's `/user/{id}/tracking` subscription. Retains assigned-driver send checks and participant topic authorization. Already established subscriptions after later account suspension are not proven revoked; long-lived session enforcement remains a review item.
6. **Order-item persistence:** upstream added a read-only `OrderItem.order` association but made `orderId` transient. Existing order creation still sets scalar `orderId`, so writes would lose the order reference and reads would return null IDs. Restore writable `@Column(name="order_id", nullable=false)` while keeping Daniel's read-only association and relationship queries. Uses the existing column; no migration introduced by this fix. Live persistence verification remains blocked by unrelated upstream schema additions.
7. **Test compatibility:** supply new Daraja/TrackingStop dependencies in service tests and current-account validator in the logging MVC slice; update payment-status enum assertion to preserve upstream FAILED status. Give moderation fixtures a realistic non-null stock quantity required by upstream response mapping.
8. **Catalog/inventory:** retained catalog-authoritative prices, integer/minimum quantity validation, duplicate/self-order rejection, ascending product row locks, reserved-stock floor and order locks. Mock tests verify validation and lock invocation; they do not prove PostgreSQL concurrent overselling prevention under load.

## Preserved upstream work and wire contracts

Daniel's inspection changes, product enrichment/admin controller, payment initiation/callback implementation, logistics tracking/WebSocket components, dependency/toolchain changes, migrations V14/V15 and request logging are retained. Upstream changes (including its documentation removal) are merge history, not a replacement of main with the old feature tree.

No Android farmer JSON request, envelope, numeric ID, login/refresh token shape or multipart `image` field was redesigned. The existing local launcher remains unchanged. Upstream product enrichment adds response fields, PaymentStatus includes FAILED, and upstream tracking/STK routes remain available in source. Product moderation enum/wire spellings remain Daniel's current definitions (`pending` maps to PENDING_APPROVAL); removed historical enum values require a data audit before migrating environments that might contain them.

Authorization changes are intentional: status updates require admin, owned records are scoped, disabled/stale-role accounts cannot authenticate, and STK cannot trust supplied identity. Recovery and incomplete checkout continue returning 503. Web/admin client implementations have not been tested.

## Database compatibility: approval required, nothing applied

Verified through a read-only transaction as `kilivana_app`: `127.0.0.1:5432/kilivana`, 35 public base tables. Offline Hibernate export finds 37 entities. Direct Hibernate schema validation using a connection with `default_transaction_read_only=on` fails with `Schema-validation: missing table [driver_latest_location]`.

Offline metadata comparison identifies:

| Required upstream change | Existing local schema gap |
|---|---|
| `db/migrations/V14__driver_location_tracking.sql` | Missing `driver_locations` and `driver_latest_location`, with their indexes. |
| `db/migrations/V15__daraja_payment_fields.sql` | `payments`: callback_status, checkout_request_id, merchant_request_id, mpesa_receipt_number, phone_number, transaction_date; upstream indexes and receipt uniqueness also need review. |
| Upstream product entity additions, not covered by V14/V15 | `products`: farm_id, harvest_date, last_inspection_id, low_stock_threshold, quality_grade, status_changed_at, status_changed_by, status_reason, verified_at, verified_by, verified_qty. A reviewed forward migration is required; do not use Hibernate update. |

This column/table comparison is not a complete constraint/index/type/data audit. Before approval, prepare a forward-only proposal against actual existing data, check enum/check constraints and historical values, timestamp types, index collisions, receipt uniqueness and defaults. Do not run historical migrations blindly. No existing data was changed. No new Android schema/database was created.

The pre-reconciliation backend remains listening on 127.0.0.1:8081; a read-only category request returned 200. This is NOT evidence the merged code starts. Do not restart it with the merged build until the schema proposal is reviewed and approved. Startup may reveal further configuration requirements after schema validation is satisfied.

## Verification

- Backend on JDK 17: `mvnw.cmd -o "-Dtest=!OptionalFilterQueryTest" test`: **279 passed, zero failures/errors**. Initial missing dependencies were downloaded; initial wiring/fixture/enum failures were corrected, not skipped.
- `OptionalFilterQueryTest` was not run: it loads a real database application context. No test against that context should be reported passed while schema validation fails.
- Coverage includes registration/login controllers, profile contracts, password recovery guards, pending login/refresh, JWT/current account enforcement, service authorization negative cases, product stock/edit/moderation safeguards, catalog order prices, upstream logistics/tracking, profile image-controller contracts and WebSocket handshake validation. Product image live compatibility is not established by those profile tests.
- Android: `gradlew.bat :app:testDebugUnitTest -Pkilivana.demoInstall=true --offline --rerun-tasks`: **138 passed, zero failures/errors**, BUILD SUCCESSFUL, all 24 tasks executed. No Android source changes.
- Connected phone is available; physical tests against the merged runtime are **NOT RUN**, because the merged runtime cannot validate the approved 35-table schema. No duplicate farmer/product test records were created.
- Live merged registration/login, refresh/session restoration, product create/edit and image upload/retrieval remain **BLOCKED**, not passed. Earlier physical-device evidence is preserved in historical reports only.
- High-confidence private-key/token secret-pattern scan of the feature diff found no hits; local credentials and runtime logs remain excluded. Reviewable diff and local test/schema logs remain under ignored target.

## Remaining approvals and next steps

1. Review a non-destructive, local-only forward schema proposal covering V14/V15 plus missing product fields and data compatibility. Do not execute without approval.
2. After approval and validation, refresh the runtime dependency classpath for new upstream WebSocket/WebFlux dependencies, then start with the secure launcher on 127.0.0.1:8081 and ddl-auto=validate, seeders off. Preserve reference port 8080.
3. Rerun real farmer registration/login/profile/categories/product create/edit/images and process-restart refresh tests on the connected phone; distinguish new records from reusable fixtures. Test row-lock behavior with concurrent PostgreSQL transactions in a separately approved test setup.
4. Review Daraja sandbox configuration, callback authorization/idempotency, full-order versus mixed-seller payment behavior, fractional amounts and long-lived WebSocket authorization and per-user broker delivery/principal wiring. No external payment requests were made.
5. Product approval remains PENDING TEAM DECISION. No Phase 2 started. No website/admin compatibility claim.
6. Only after explicit push approval: `git push --set-upstream origin feature/android-backend-integration`. No push to main, force push, PR creation or merge is authorized.

## Exact feature/merge-resolution file inventory relative to Daniel main

This is the proposed PR file scope, excluding the pre-existing unstaged local files. Daniel's upstream additions are already in main and therefore absent from this list.

- `docs/integration/01-password-reset-security.md`
- `docs/integration/README.md`
- `docs/integration/review/05-local-demo-verification.md`
- `docs/integration/review/06-backend-feature-parity-matrix.md`
- `docs/integration/review/07-android-integration-audit.md`
- `docs/integration/review/08-farmer-inspection-verification-audit.md`
- `docs/integration/review/09-backend-migration-plan.md`
- `docs/integration/review/10-open-decisions-technical-risks.md`
- `docs/integration/review/11-phase1-security-implementation-report.md`
- `docs/integration/review/12-phase1-upstream-reconciliation.md`
- `docs/integration/review/README.md`
- `src/main/java/com/kilivana/backend/admin/service/AddressService.java`
- `src/main/java/com/kilivana/backend/admin/service/InspectionService.java`
- `src/main/java/com/kilivana/backend/admin/service/NotificationService.java`
- `src/main/java/com/kilivana/backend/admin/service/UserService.java`
- `src/main/java/com/kilivana/backend/common/controller/AuthController.java`
- `src/main/java/com/kilivana/backend/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/kilivana/backend/common/service/AuthService.java`
- `src/main/java/com/kilivana/backend/config/SecurityConfig.java`
- `src/main/java/com/kilivana/backend/ecommerce/entity/OrderItem.java`
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderRepository.java`
- `src/main/java/com/kilivana/backend/ecommerce/repository/ProductRepository.java`
- `src/main/java/com/kilivana/backend/ecommerce/service/CartService.java`
- `src/main/java/com/kilivana/backend/ecommerce/service/DisputeService.java`
- `src/main/java/com/kilivana/backend/ecommerce/service/EcommerceNotificationService.java`
- `src/main/java/com/kilivana/backend/ecommerce/service/OrderService.java`
- `src/main/java/com/kilivana/backend/ecommerce/service/PaymentService.java`
- `src/main/java/com/kilivana/backend/ecommerce/service/ProductService.java`
- `src/main/java/com/kilivana/backend/logistics/service/LogisticsService.java`
- `src/main/java/com/kilivana/backend/logistics/service/TrackingChannelInterceptor.java`
- `src/main/java/com/kilivana/backend/security/AccountAccessValidator.java`
- `src/main/java/com/kilivana/backend/security/JwtAuthenticationFilter.java`
- `src/main/java/com/kilivana/backend/security/JwtHandshakeInterceptor.java`
- `src/main/java/com/kilivana/backend/security/ResourceAuthorization.java`
- `src/test/java/com/kilivana/backend/admin/controller/ProfileControllerTest.java`
- `src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java`
- `src/test/java/com/kilivana/backend/common/controller/HealthControllerTest.java`
- `src/test/java/com/kilivana/backend/common/enums/PaymentMethodTest.java`
- `src/test/java/com/kilivana/backend/common/service/AuthServiceEmailNormalizationTest.java`
- `src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java`
- `src/test/java/com/kilivana/backend/config/RequestLoggingFilterTest.java`
- `src/test/java/com/kilivana/backend/ecommerce/service/OrderServiceTest.java`
- `src/test/java/com/kilivana/backend/ecommerce/service/ProductStockSafetyTest.java`
- `src/test/java/com/kilivana/backend/logistics/controller/DriverProfileControllerTest.java`
- `src/test/java/com/kilivana/backend/logistics/service/TrackingChannelInterceptorTest.java`
- `src/test/java/com/kilivana/backend/security/AccountAccessValidatorTest.java`
- `src/test/java/com/kilivana/backend/security/JwtAccountEnforcementTest.java`
- `src/test/java/com/kilivana/backend/security/JwtHandshakeAccountTest.java`
- `src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java`
