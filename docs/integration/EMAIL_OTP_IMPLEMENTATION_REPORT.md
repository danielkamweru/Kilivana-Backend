# Email OTP implementation — mandatory file change report

Root: `C:\Users\AppleFi\AndroidStudioProjects\kilivana-supplier-backend`. Every path below is exact and relative to this root.

Branch: `feature/supplier-api-updates`. Commit remains `bf610306b3fa8c18277bb1cbbd81af2cc148f664`. No commit, push, merge, reset, clean, stash mutation, deployment, live-account operation or shared-database write occurred. Android, Admin Portal and original kilivana-shared-backend worktrees were not modified.

## A. Files created

- `db/migrations/V20__email_otp_verification.sql` — Unapplied reviewed migration: grandfather existing users, fail-closed future insert default, email timestamp and credential/rate tables.
- `docs/integration/EMAIL_OTP_IMPLEMENTATION_REPORT.md` — This mandatory A-H change, validation, preservation and Git report.
- `docs/integration/EMAIL_OTP_VERIFICATION_API.md` — Complete API examples, role flows, Android/Admin changes, limits, key/mail config, migration and deployment/testing instructions.
- `src/main/java/com/kilivana/backend/common/controller/EmailVerificationController.java` — New anonymous request/resend/verify routes under both auth bases; typed validation and existing ApiResponse envelopes.
- `src/main/java/com/kilivana/backend/common/dto/EmailOtpChallengeResponse.java` — Uniform opaque challenge acknowledgment and account-independent timing hints; no code or existence flag.
- `src/main/java/com/kilivana/backend/common/dto/EmailOtpRequest.java` — Validated email-only request/resend DTO; no role or user-ID authority input.
- `src/main/java/com/kilivana/backend/common/dto/EmailOtpVerifyRequest.java` — Validated UUID/email/six-digit DTO; no generated code-bearing toString.
- `src/main/java/com/kilivana/backend/common/entity/EmailOtpRateLimit.java` — HMAC-address/IP bucket with counters, cooldown and latest queued send ticket.
- `src/main/java/com/kilivana/backend/common/entity/EmailVerificationChallenge.java` — One keyed-hash challenge per user, bound email/role/purpose, expiry/consumption and durable attempt budget.
- `src/main/java/com/kilivana/backend/common/exception/EmailOtpInvalidException.java` — Generic proof failure whose attempt-counter transaction commits.
- `src/main/java/com/kilivana/backend/common/exception/EmailOtpRateLimitException.java` — Generic rate-limit exception with Retry-After duration.
- `src/main/java/com/kilivana/backend/common/repository/EmailOtpRateLimitRepository.java` — Atomic PostgreSQL bucket upsert, pessimistic bucket locking and latest-ticket matching.
- `src/main/java/com/kilivana/backend/common/repository/EmailVerificationChallengeRepository.java` — Challenge lookup and persistence; serialization uses the existing user row lock.
- `src/main/java/com/kilivana/backend/common/service/EmailOtpDispatchQueue.java` — Two-worker/100-job bounded background dispatcher; acknowledgment before account lookup/provider I/O; generic failures without payload logs.
- `src/main/java/com/kilivana/backend/common/service/EmailOtpRequestLimiter.java` — Persistent email/IP send and verification budgets, cooldown across hourly rollover; phase separated from account transactions.
- `src/main/java/com/kilivana/backend/common/service/EmailVerificationService.java` — OTP orchestration facade; limits before transactions, queued non-enumerating request/resend, synchronous verification delegation.
- `src/main/java/com/kilivana/backend/common/service/EmailVerificationTransactions.java` — Account-locked OTP generation/rotation, attempt persistence, single use and email-only updates; suppress obsolete queued jobs.
- `src/main/java/com/kilivana/backend/mail/EmailVerificationEmailTemplate.java` — Kilivana HTML role-aware email with expiry, single-use, privacy and unrequested-email guidance.
- `src/main/java/com/kilivana/backend/security/EmailOtpSecurity.java` — Required dedicated HMAC-SHA-256 key, SecureRandom code generation, purpose/account/email/role/challenge binding, constant-time digest comparison.
- `src/test/java/com/kilivana/backend/common/controller/EmailVerificationControllerTest.java` — 15 HTTP cases: all public aliases, staff-role rejection, response/validation, cooldown, tampered flags and real signed JWT gate.
- `src/test/java/com/kilivana/backend/common/service/EmailOtpDispatchQueueTest.java` — Three cases proving acknowledgment before lookup, generic queue-full errors and secret-safe worker failure logging.
- `src/test/java/com/kilivana/backend/common/service/EmailVerificationServiceTest.java` — 17 unit cases: role templates, binding, expiry, reuse, attempts, resend, failure and stale queued-job rejection.
- `src/test/java/com/kilivana/backend/repository/EmailOtpDatabaseTest.java` — 23 real PostgreSQL cases: all permitted enrollment/provisioning paths, rollback, user/migration compatibility, counters, cooldown, races and queue ordering.
- `src/test/java/com/kilivana/backend/security/EmailOtpAuthorizationTest.java` — Seven actual service-proxy cases for unauthorized staff creation and authorized unverified Inspector provisioning.

## B. Files modified

- `src/main/java/com/kilivana/backend/admin/controller/AdminDriverController.java` — Acquire initial send budget before existing Admin Driver registration.
- `src/main/java/com/kilivana/backend/admin/controller/AdminSupplierController.java` — Acquire initial send budget before existing Admin Supplier registration.
- `src/main/java/com/kilivana/backend/admin/controller/UserController.java` — Acquire initial send limit and validate existing Admin creation DTO before provisioning.
- `src/main/java/com/kilivana/backend/admin/controller/UserProfileController.java` — Apply identical initial send budget to the existing public /users registration aliases.
- `src/main/java/com/kilivana/backend/admin/dto/AdminDriverResponse.java` — Add emailVerified and initial challenge without changing driver/KYC fields.
- `src/main/java/com/kilivana/backend/admin/dto/AdminSupplierResponse.java` — Add emailVerified and initial challenge without removing existing roster fields.
- `src/main/java/com/kilivana/backend/admin/dto/UserResponse.java` — Add emailVerified/emailVerifiedAt/initial challenge; clarify approval status is independent.
- `src/main/java/com/kilivana/backend/admin/entity/User.java` — Add grandfather-compatible emailVerified and timestamp; changing actual email invalidates its proof.
- `src/main/java/com/kilivana/backend/admin/repository/UserRepository.java` — Add pessimistic account lock for OTP and email-contact updates.
- `src/main/java/com/kilivana/backend/admin/service/AdminDriverService.java` — Same OTP flow for existing combined account/profile provisioning; preserve driver/KYC policy; expose new flags.
- `src/main/java/com/kilivana/backend/admin/service/AdminSupplierService.java` — Same OTP flow for existing combined account/profile provisioning; preserve pending policy; expose new flags.
- `src/main/java/com/kilivana/backend/admin/service/UserService.java` — Admin-only service-boundary provisioning; same unverified OTP flow; map email fields and lock contact edits.
- `src/main/java/com/kilivana/backend/common/controller/AuthController.java` — Acquire durable initial send limits before the registration transaction; retain aliases and payloads.
- `src/main/java/com/kilivana/backend/common/enums/KycStatus.java` — Correct reference to separate email flag without changing KYC enum or behavior.
- `src/main/java/com/kilivana/backend/common/enums/VerificationStatus.java` — Correct stale comment/schema: existing enum represents Admin verification/approval, not the new email field.
- `src/main/java/com/kilivana/backend/common/exception/GlobalExceptionHandler.java` — Generic OTP rate-limit envelope and Retry-After header.
- `src/main/java/com/kilivana/backend/common/service/AuthService.java` — Explicitly reject public ADMIN/INSPECTOR; create unverified users and dispatch initial OTP; require verification for login/refresh; map email fields and lock contact edits.
- `src/main/java/com/kilivana/backend/config/CorsConfig.java` — Expose Retry-After so the Admin web UI can obey server cooldowns.
- `src/main/java/com/kilivana/backend/config/SecurityConfig.java` — Public allowlist for the six exact OTP POST URLs; retain existing role gates.
- `src/main/java/com/kilivana/backend/mail/SendGridMailService.java` — Treat only 2xx as accepted; handle null/rejected/runtime responses; never log provider exception bodies, OTPs or keys.
- `src/main/java/com/kilivana/backend/security/AccountAccessValidator.java` — Deny bearer authentication for an unverified current email; preserve status/role validation.
- `src/main/java/com/kilivana/backend/security/ResourceAuthorization.java` — Require current email gate at service authorization boundaries as well as the JWT filter.
- `src/main/resources/application.properties` — Add external EMAIL_OTP_HMAC_SECRET binding only; no default key or credential value.
- `src/test/java/com/kilivana/backend/admin/service/UserServiceUsernameRegionTest.java` — Update dependency fixtures and new locking lookup, preserving username/county regression assertions.
- `src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java` — Mock initial send limiter required by existing controller regression slice.
- `src/test/java/com/kilivana/backend/common/service/AuthServiceEmailNormalizationTest.java` — Provide initial challenge stub for new dependency; preserve case/phone/login and pending-approval regressions.
- `src/test/java/com/kilivana/backend/common/service/PasswordResetFlowTest.java` — Update AuthService constructor fixture only; preserve complete password reset proof tests.
- `src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java` — Update AuthService constructor fixture only; preserve generic recovery/security regressions.
- `src/test/java/com/kilivana/backend/mail/SendGridMailServiceTest.java` — Explicit accepted 202 fixture; add non-2xx/runtime and no-secret-log regression.
- `src/test/java/com/kilivana/backend/security/AccountAccessValidatorTest.java` — Add unverified-email and changed-email bearer rejection coverage.
- `src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java` — Add mocked EmailVerificationService to the shared service-security fixture; retain earlier FarmRepository addition and all prior assertions.

## C. Files read/reused

The files in B were inspected before modification; the new files in A were read/reviewed during implementation and tests. Additional exact code/contract files inspected or reused:

- `pom.xml` — Maven dependencies, existing JDK/source target and absence of a new migration/auth dependency.
- `mvnw.cmd` — Existing Maven wrapper reused for all validation.
- `kilivana-openapi.json` — Existing Android/Admin contract snapshot: registration aliases, UserResponse/AuthTokenResponse and Admin provisioning routes; left unchanged.
- `src/main/java/com/kilivana/backend/common/enums/UserRole.java` — Existing enum values and isStaff mismatch; no role added or changed.
- `src/main/java/com/kilivana/backend/common/enums/UserStatus.java` — Preserve account access policy and state values.
- `src/main/java/com/kilivana/backend/security/JwtAuthenticationFilter.java` — Reuse signed access-token type, current account validator and Long principal.
- `src/main/java/com/kilivana/backend/security/JwtService.java` — Reuse JWT issuance and claims; OTP does not introduce another login/token system.
- `src/main/java/com/kilivana/backend/security/JwtProperties.java` — Existing configurable token expiry and signing-key contract.
- `src/main/java/com/kilivana/backend/admin/dto/UserRegistrationRequest.java` — Retain existing role/name/email/password registration contract; no client verification flag.
- `src/main/java/com/kilivana/backend/admin/dto/SupplierRegistrationRequest.java` — Existing Admin combined Supplier enrollment contract.
- `src/main/java/com/kilivana/backend/admin/dto/DriverRegistrationRequest.java` — Existing Admin combined Driver enrollment contract.
- `src/main/java/com/kilivana/backend/common/dto/AuthLoginRequest.java` — Existing login payload reused by HTTP/database tests.
- `src/main/java/com/kilivana/backend/common/dto/AuthTokenResponse.java` — Existing JWT envelope retained; no tokens returned by OTP verification.
- `src/main/java/com/kilivana/backend/common/dto/ApiResponse.java` — Existing success/error envelopes reused.
- `src/main/java/com/kilivana/backend/common/service/UserReferenceCodeGenerator.java` — Retain role reference codes; no role or identity reassignment on email proof.
- `src/main/java/com/kilivana/backend/common/entity/PasswordResetToken.java` — Existing separate hashed recovery purpose and lifecycle.
- `src/main/java/com/kilivana/backend/common/repository/PasswordResetTokenRepository.java` — Existing locked one-use recovery token, reused without OTP duplication.
- `src/main/java/com/kilivana/backend/mail/SendGridProperties.java` — Reuse existing externally configured mail integration.
- `src/main/java/com/kilivana/backend/mail/SendGridBootstrapValidator.java` — Existing enabled-mail key/sender checks; no credentials inspected.
- `src/main/java/com/kilivana/backend/mail/DeliveryOtpNotifier.java` — Keep delivery/handover purpose separate from email verification.
- `src/main/java/com/kilivana/backend/mail/DriverOtpEmailTemplate.java` — Inspect existing email rendering pattern; new account template uses fixed safe role labels.
- `src/main/java/com/kilivana/backend/admin/controller/AdminInspectorController.java` — Existing staff profile/suspension workflow; account provisioning remains /admin/users.
- `src/main/java/com/kilivana/backend/admin/controller/AdminFarmerController.java` — Verify Admin approval mutates separate verificationStatus; OTP never invokes it.
- `src/main/java/com/kilivana/backend/config/RequestLoggingFilter.java` — Confirm application HTTP logs exclude request body/code.
- `src/main/java/com/kilivana/backend/common/exception/TooManyRequestsException.java` — Reuse rate-limit error family.
- `src/main/java/com/kilivana/backend/common/exception/ServiceUnavailableException.java` — Reuse provider/configuration failure status 503.
- `src/main/java/com/kilivana/backend/common/enums/VehicleType.java` — Preserve existing Driver provisioning contract for integration fixtures.
- `db/migrations/V6__unique_email_case_insensitive.sql` — Preserve normalized email identity and existing uniqueness assumptions.
- `db/migrations/V8__single_admin_role.sql` — Inspect current role constraint; V20 does not rewrite it.
- `db/migrations/V19__seller_product_lifecycle.sql` — Confirm prior uncommitted product migration remains byte-for-byte unchanged.
- `docs/integration/01-password-reset-security.md` — Historical recovery decisions; actual current code and tests remain authoritative.
- `docs/integration/02-token-lifecycle.md` — Existing JWT/client architecture and alias constraints; no new token system.
- `docs/integration/BACKEND_API_AUDIT.md` — Reuse prior baseline API/domain audit without overwriting it.
- `docs/integration/FARMER_PRODUCT_ADMIN_FARM_API_CHANGES.md` — Preserve earlier Farmer/Admin integration work and approval separation.
- `docs/integration/BACKEND_IMPLEMENTATION_REPORT.md` — Preserve prior task report and validation history.

Repository instruction check: no AGENTS.md existed in this worktree or the checked ancestor locations. No skill was necessary for this backend task. Existing src/db/docs files were also read as bytes to record preservation hashes; those checksum-only reads are listed in the appendix and are not claimed as semantic API audits. No environment/credential file was opened or overwritten.

## D. Files deleted

None. Docker cleanup removed only the newly created disposable container and its synthetic data; no source file or existing database was deleted.

## E. Tests/builds

**Final: 437 passed, 0 failed, 0 errors, 9 skipped (446 total cases).** BUILD SUCCESS on JDK 21.0.1 with the existing Java 17 source target.

Exact Maven commands executed:

```powershell
.\mvnw.cmd -o -B -DskipTests compile
.\mvnw.cmd -o -B test
```

The test command was rerun after material security changes. The initial database-free run passed 390 cases with 14 gated skips. An intermediate new database test failed testCompile because of a missing UserResponse import; that fixture was corrected. Subsequent PostgreSQL runs passed, including the final full-source run after async dispatch/latest-ticket and hourly-rollover hardening. No known failing test remains.

Final process-local environment (no .env modification):

```text
JAVA_HOME=C:\Program Files\Java\jdk-21
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55439/kilivana_audit
SPRING_DATASOURCE_USERNAME=audit_test
SPRING_DATASOURCE_PASSWORD=<synthetic disposable-container password>
KILIVANA_RUN_EMAIL_OTP_DB_TESTS=true
KILIVANA_RUN_API_AUDIT_DB_TESTS=true
KILIVANA_RUN_ISOLATED_DB_TESTS=false
ENV_FILE=target/nonexistent-otp-test.env
```

Database safety: fresh postgres:16-alpine container `kilivana-email-otp-test`, localhost port 55439, no mounted volumes. Tests use create-drop only behind the exact opt-in URL. The V20 SQL was executed in an extra fresh synthetic schema within this disposable database. No copied/shared/live user data was used. The container was stopped and auto-removed after tests.

Coverage: 23 OTP PostgreSQL cases; 17 OTP transaction/template unit cases; 15 HTTP contract/security cases; 7 real service-authorization cases; 3 queue boundary/saturation/logging cases; additional bearer and SendGrid log regressions; all 5 prior FarmerProductDatabaseTest cases also ran. The 9 deliberate skips are the pre-existing OptionalFilterQueryTest baseline cases restricted to a different database on port 55432.

The real provider was mocked. Inbox delivery, SendGrid credentials/sender validation in a deployed environment, and frontend screens were not tested. No JAR packaging/deployment was performed or claimed. Compilation and the full Maven test lifecycle succeeded using cached dependencies. Existing Lombok/annotation/deprecation warnings remain. Git diff --check passed.

Generated ignored validation artifacts (not source changes, do not commit):

- `target/email-otp-task-baseline.json` — initial uncommitted status and preservation hashes; no source contents or credentials.
- `target/email-otp-compile.log` — successful JDK 21 compilation.
- `target/email-otp-unit-validation.log` — successful early full database-free regression run.
- `target/email-otp-integration-validation.log` — successful PostgreSQL-backed run before final hardening.
- `target/email-otp-final-validation.log` — final complete successful run.
- `target/surefire-reports/` and `target/classes/`, `target/test-classes/`, Maven compiler metadata — generated test/build outputs.

Final per-suite totals:

| Suite | Passed | Failed/errors | Skipped |
|---|---|---|---|
| com.kilivana.backend.admin.controller.AdminFarmerControllerTest | 5 | 0 | 0 |
| com.kilivana.backend.admin.controller.ProfileControllerTest | 20 | 0 | 0 |
| com.kilivana.backend.admin.dto.DriverCapacityFormatterTest | 5 | 0 | 0 |
| com.kilivana.backend.admin.dto.DriverEnumParsingTest | 5 | 0 | 0 |
| com.kilivana.backend.admin.service.UserServiceUsernameRegionTest | 4 | 0 | 0 |
| com.kilivana.backend.common.controller.AuthControllerTest | 7 | 0 | 0 |
| com.kilivana.backend.common.controller.EmailVerificationControllerTest | 15 | 0 | 0 |
| com.kilivana.backend.common.controller.HealthControllerTest | 1 | 0 | 0 |
| com.kilivana.backend.common.controller.PublicFarmerControllerTest | 3 | 0 | 0 |
| com.kilivana.backend.common.enums.EnumWireFormatTest | 45 | 0 | 0 |
| com.kilivana.backend.common.enums.PaymentMethodTest | 21 | 0 | 0 |
| com.kilivana.backend.common.service.AuthServiceEmailNormalizationTest | 4 | 0 | 0 |
| com.kilivana.backend.common.service.EmailOtpDispatchQueueTest | 3 | 0 | 0 |
| com.kilivana.backend.common.service.EmailVerificationServiceTest | 17 | 0 | 0 |
| com.kilivana.backend.common.service.KenyaLocaleTest | 31 | 0 | 0 |
| com.kilivana.backend.common.service.PasswordResetFlowTest | 5 | 0 | 0 |
| com.kilivana.backend.common.service.PasswordResetSecurityTest | 2 | 0 | 0 |
| com.kilivana.backend.config.BootstrapConfigTest | 6 | 0 | 0 |
| com.kilivana.backend.config.DatabaseUrlEnvironmentPostProcessorTest | 5 | 0 | 0 |
| com.kilivana.backend.config.RequestLoggingFilterTest | 2 | 0 | 0 |
| com.kilivana.backend.ecommerce.service.DisputeServiceTest | 6 | 0 | 0 |
| com.kilivana.backend.ecommerce.service.OrderServiceTest | 16 | 0 | 0 |
| com.kilivana.backend.ecommerce.service.PaymentCallbackTest | 3 | 0 | 0 |
| com.kilivana.backend.ecommerce.service.PaymentServiceTest | 4 | 0 | 0 |
| com.kilivana.backend.ecommerce.service.ProductImageManagementTest | 9 | 0 | 0 |
| com.kilivana.backend.ecommerce.service.ProductStockSafetyTest | 2 | 0 | 0 |
| com.kilivana.backend.farm.controller.AdminFarmVisibilityTest | 3 | 0 | 0 |
| com.kilivana.backend.farm.controller.FarmerFarmControllerTest | 6 | 0 | 0 |
| com.kilivana.backend.logistics.controller.DriverProfileControllerTest | 9 | 0 | 0 |
| com.kilivana.backend.logistics.service.DeliveryOtpFlowTest | 8 | 0 | 0 |
| com.kilivana.backend.logistics.service.LocationRateLimiterTest | 3 | 0 | 0 |
| com.kilivana.backend.logistics.service.LocationValidatorTest | 12 | 0 | 0 |
| com.kilivana.backend.logistics.service.LogisticsServiceTest | 18 | 0 | 0 |
| com.kilivana.backend.logistics.service.TrackingChannelInterceptorTest | 7 | 0 | 0 |
| com.kilivana.backend.logistics.service.TrackingDestinationTest | 2 | 0 | 0 |
| com.kilivana.backend.mail.SendGridMailServiceTest | 5 | 0 | 0 |
| com.kilivana.backend.repository.EmailOtpDatabaseTest | 23 | 0 | 0 |
| com.kilivana.backend.repository.FarmerProductDatabaseTest | 5 | 0 | 0 |
| com.kilivana.backend.repository.OptionalFilterQueryTest | 0 | 0 | 9 |
| com.kilivana.backend.security.AccountAccessValidatorTest | 5 | 0 | 0 |
| com.kilivana.backend.security.CatalogVisibilityTest | 18 | 0 | 0 |
| com.kilivana.backend.security.EmailOtpAuthorizationTest | 7 | 0 | 0 |
| com.kilivana.backend.security.JwtAccountEnforcementTest | 4 | 0 | 0 |
| com.kilivana.backend.security.JwtHandshakeAccountTest | 1 | 0 | 0 |
| com.kilivana.backend.security.LocalProductImageVisibilityTest | 8 | 0 | 0 |
| com.kilivana.backend.security.ProductManagementSecurityTest | 15 | 0 | 0 |
| com.kilivana.backend.security.ServiceAuthorizationTest | 30 | 0 | 0 |
| com.kilivana.backend.service.DarajaServiceTest | 2 | 0 | 0 |

## F. Git status

Branch `feature/supplier-api-updates`; unchanged HEAD `bf610306b3fa8c18277bb1cbbd81af2cc148f664`. Nothing was committed or pushed. All changes remain unstaged.

Task-specific files are enumerated in A/B. Entire current pending tree, including preserved prior work:

```text
M src/main/java/com/kilivana/backend/admin/controller/AdminDriverController.java
 M src/main/java/com/kilivana/backend/admin/controller/AdminSupplierController.java
 M src/main/java/com/kilivana/backend/admin/controller/UserController.java
 M src/main/java/com/kilivana/backend/admin/controller/UserProfileController.java
 M src/main/java/com/kilivana/backend/admin/dto/AdminDriverResponse.java
 M src/main/java/com/kilivana/backend/admin/dto/AdminSupplierResponse.java
 M src/main/java/com/kilivana/backend/admin/dto/UserResponse.java
 M src/main/java/com/kilivana/backend/admin/entity/User.java
 M src/main/java/com/kilivana/backend/admin/repository/UserRepository.java
 M src/main/java/com/kilivana/backend/admin/service/AdminDriverService.java
 M src/main/java/com/kilivana/backend/admin/service/AdminSupplierService.java
 M src/main/java/com/kilivana/backend/admin/service/UserService.java
 M src/main/java/com/kilivana/backend/common/controller/AuthController.java
 M src/main/java/com/kilivana/backend/common/enums/KycStatus.java
 M src/main/java/com/kilivana/backend/common/enums/ProductStatus.java
 M src/main/java/com/kilivana/backend/common/enums/VerificationStatus.java
 M src/main/java/com/kilivana/backend/common/exception/GlobalExceptionHandler.java
 M src/main/java/com/kilivana/backend/common/service/AuthService.java
 M src/main/java/com/kilivana/backend/common/service/LocalImageStorage.java
 M src/main/java/com/kilivana/backend/config/CorsConfig.java
 M src/main/java/com/kilivana/backend/config/SecurityConfig.java
 M src/main/java/com/kilivana/backend/ecommerce/controller/ProductController.java
 M src/main/java/com/kilivana/backend/ecommerce/repository/ProductRepository.java
 M src/main/java/com/kilivana/backend/ecommerce/service/ProductService.java
 M src/main/java/com/kilivana/backend/farm/controller/FarmerFarmController.java
 M src/main/java/com/kilivana/backend/farm/repository/FarmRepository.java
 M src/main/java/com/kilivana/backend/mail/SendGridMailService.java
 M src/main/java/com/kilivana/backend/security/AccountAccessValidator.java
 M src/main/java/com/kilivana/backend/security/ProductVisibility.java
 M src/main/java/com/kilivana/backend/security/ResourceAuthorization.java
 M src/main/resources/application.properties
 M src/test/java/com/kilivana/backend/admin/controller/AdminFarmerControllerTest.java
 M src/test/java/com/kilivana/backend/admin/service/UserServiceUsernameRegionTest.java
 M src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java
 M src/test/java/com/kilivana/backend/common/service/AuthServiceEmailNormalizationTest.java
 M src/test/java/com/kilivana/backend/common/service/PasswordResetFlowTest.java
 M src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java
 M src/test/java/com/kilivana/backend/farm/controller/FarmerFarmControllerTest.java
 M src/test/java/com/kilivana/backend/logistics/service/LogisticsServiceTest.java
 M src/test/java/com/kilivana/backend/mail/SendGridMailServiceTest.java
 M src/test/java/com/kilivana/backend/security/AccountAccessValidatorTest.java
 M src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java
?? db/migrations/V19__seller_product_lifecycle.sql
?? db/migrations/V20__email_otp_verification.sql
?? docs/integration/BACKEND_API_AUDIT.md
?? docs/integration/BACKEND_IMPLEMENTATION_REPORT.md
?? docs/integration/EMAIL_OTP_IMPLEMENTATION_REPORT.md
?? docs/integration/EMAIL_OTP_VERIFICATION_API.md
?? docs/integration/FARMER_PRODUCT_ADMIN_FARM_API_CHANGES.md
?? src/main/java/com/kilivana/backend/common/controller/EmailVerificationController.java
?? src/main/java/com/kilivana/backend/common/dto/EmailOtpChallengeResponse.java
?? src/main/java/com/kilivana/backend/common/dto/EmailOtpRequest.java
?? src/main/java/com/kilivana/backend/common/dto/EmailOtpVerifyRequest.java
?? src/main/java/com/kilivana/backend/common/entity/EmailOtpRateLimit.java
?? src/main/java/com/kilivana/backend/common/entity/EmailVerificationChallenge.java
?? src/main/java/com/kilivana/backend/common/exception/EmailOtpInvalidException.java
?? src/main/java/com/kilivana/backend/common/exception/EmailOtpRateLimitException.java
?? src/main/java/com/kilivana/backend/common/repository/EmailOtpRateLimitRepository.java
?? src/main/java/com/kilivana/backend/common/repository/EmailVerificationChallengeRepository.java
?? src/main/java/com/kilivana/backend/common/service/EmailOtpDispatchQueue.java
?? src/main/java/com/kilivana/backend/common/service/EmailOtpRequestLimiter.java
?? src/main/java/com/kilivana/backend/common/service/EmailVerificationService.java
?? src/main/java/com/kilivana/backend/common/service/EmailVerificationTransactions.java
?? src/main/java/com/kilivana/backend/mail/EmailVerificationEmailTemplate.java
?? src/main/java/com/kilivana/backend/security/EmailOtpSecurity.java
?? src/test/java/com/kilivana/backend/common/controller/EmailVerificationControllerTest.java
?? src/test/java/com/kilivana/backend/common/service/EmailOtpDispatchQueueTest.java
?? src/test/java/com/kilivana/backend/common/service/EmailVerificationServiceTest.java
?? src/test/java/com/kilivana/backend/ecommerce/service/ProductImageManagementTest.java
?? src/test/java/com/kilivana/backend/farm/controller/AdminFarmVisibilityTest.java
?? src/test/java/com/kilivana/backend/repository/EmailOtpDatabaseTest.java
?? src/test/java/com/kilivana/backend/repository/FarmerProductDatabaseTest.java
?? src/test/java/com/kilivana/backend/security/EmailOtpAuthorizationTest.java
?? src/test/java/com/kilivana/backend/security/ProductManagementSecurityTest.java
```

Preservation evidence:

- Initial pending files: 20. No baseline source/db/docs file was removed.
- Every initial pending file except the shared ServiceAuthorizationTest fixture remains byte-for-byte identical to the task-start snapshot.
- The only overlap is `src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java`: one new EmailVerificationService mock bean was added; the earlier FarmRepository mock and existing product/farm security assertions remain.
- Prior product/farm source, V19 and all three integration reports were left intact. No other worktree was written to; no Git stash command changed anything.

Recommended review scope for a future commit: exactly A/B. Keep prior uncommitted product/farm changes separate according to the user’s eventual commit decision; do not commit target outputs. No commit approval is assumed.

## G. API impacts

Full endpoint/DTO/status-code examples are in [EMAIL_OTP_VERIFICATION_API.md](EMAIL_OTP_VERIFICATION_API.md).

- New: POST `/api/v1/auth/email-verification/request`, `/resend`, `/verify`, with identical `/api/auth` aliases. EmailOtpRequest/EmailOtpVerifyRequest inputs and EmailOtpChallengeResponse acknowledgment; 202 request/resend, 200 verify; generic 400, 429 Retry-After, generic queue 503.
- Existing registration: both auth aliases and both /users aliases retain UserRegistrationRequest and 201 UserResponse, adding emailVerified/emailVerifiedAt/emailVerificationChallengeId. Initial provider failure rolls back and returns 503. Public ADMIN/INSPECTOR rejected, existing FARMER/SUPPLIER/BUYER/DRIVER roles retained.
- Existing Admin creation: /api/v1/admin/users, /admin/suppliers and /admin/drivers reuse the same OTP flow and acquire rate budgets before transactions. Service/URL Admin authorization remains enforced. Specialized responses add emailVerified/initial challenge.
- Existing login/refresh: no JWT before email proof; correct credentials with unverified email return 403. Existing signed access tokens cannot authenticate while the current email is unverified (401). Password recovery does not bypass the gate.
- UserResponse reads add email fields. Actual email edits clear proof centrally; updated clients request a new code without needing a JWT. Role, account status, Admin approval, farms/products and KYC remain separate.
- Migration V20 requires review/application before deploying the binary. Existing users are grandfathered TRUE with no invented verified timestamp; future database inserts default FALSE. No role enum change or existing-record deletion.
- Android requires registration-to-OTP entry, challenge storage, leading-zero-safe input, resend/cooldown/Retry-After handling, 503 recovery, login gate and Inspector pre-provisioned onboarding. No Android code changed.
- Admin Portal requires separate email/approval display and provisioned-user onboarding, plus 429/503 handling. No frontend source was modified or UI behavior claimed verified.
- Existing checked-in kilivana-openapi.json was inspected but not regenerated; runtime annotations describe the new contract. Regenerate its export from the reviewed configured build before client generation.

## H. Blockers / deployment requirements

- No implementation/test blocker remains. Deploy only after configuring a dedicated cryptographically random EMAIL_OTP_HMAC_SECRET (at least 32 bytes, stable/shared across instances), enabled SendGrid and a verified sender. Missing/short HMAC configuration intentionally fails startup. No keys were generated into source/env files.
- V20 was not applied to Daniel’s, shared or production database. Review and apply separately, then use schema validation and a coordinated all-auth-node rollout. Older binaries do not enforce the new gate; do not leave mixed auth versions serving registration/login.
- Real outbound provider acceptance/inbox delivery needs an explicitly authorized environment smoke test; only a mocked provider was used here.
- Request/resend queue is intentionally bounded and in-memory, not a durable mail outbox. Restart can discard pending resend jobs; 202 does not promise delivery. Retry/resend recovers after cooldown. Latest persisted send tickets prevent obsolete queued jobs overwriting newer challenges. Initial registration still requires synchronous accepted dispatch before 201.
- Configure trusted proxy/client-IP behavior and normal gateway abuse controls. Review retention for expired HMAC rate buckets without clearing active budgets. Neither gateway configuration nor production cleanup was performed.
- Frontend OTP screens and static OpenAPI export regeneration are required integration work, documented in the API guide. Existing approval policy and pending-account login semantics are preserved, not redefined.

## Preservation checksum appendix

Exact files read solely or additionally for initial src/db/docs preservation hashes. These hashes were not source edits; code inspection is distinguished above.

- `db/demo/README.md` — task-start preservation checksum.
- `db/demo/short_distance_order.sql` — task-start preservation checksum.
- `db/migrations/V10__order_status_placed.sql` — task-start preservation checksum.
- `db/migrations/V11__align_order_and_product_states.sql` — task-start preservation checksum.
- `db/migrations/V12__order_reference_and_line_details.sql` — task-start preservation checksum.
- `db/migrations/V13__inspector_specialization.sql` — task-start preservation checksum.
- `db/migrations/V14__driver_location_tracking.sql` — task-start preservation checksum.
- `db/migrations/V15__daraja_payment_fields.sql` — task-start preservation checksum.
- `db/migrations/V16__reconciled_tracking_schema.sql` — task-start preservation checksum.
- `db/migrations/V17__reconciled_product_payment_fields.sql` — task-start preservation checksum.
- `db/migrations/V18__reconciled_payment_checks_order_item_fk.sql` — task-start preservation checksum.
- `db/migrations/V19__seller_product_lifecycle.sql` — task-start preservation checksum.
- `db/migrations/V2__add_super_admin_role.sql` — task-start preservation checksum.
- `db/migrations/V3__proof_of_delivery_optional_fields.sql` — task-start preservation checksum.
- `db/migrations/V4__one_proof_of_delivery_per_job.sql` — task-start preservation checksum.
- `db/migrations/V5__backfill_reference_codes.sql` — task-start preservation checksum.
- `db/migrations/V6__unique_email_case_insensitive.sql` — task-start preservation checksum.
- `db/migrations/V7__driver_profile_admin_fields.sql` — task-start preservation checksum.
- `db/migrations/V8__single_admin_role.sql` — task-start preservation checksum.
- `db/migrations/V9__kenyan_payment_methods.sql` — task-start preservation checksum.
- `docs/integration/01-password-reset-security.md` — task-start preservation checksum.
- `docs/integration/02-token-lifecycle.md` — task-start preservation checksum.
- `docs/integration/17-final-functionality-parity-handover.md` — task-start preservation checksum.
- `docs/integration/18-product-approval-decision-and-verification.md` — task-start preservation checksum.
- `docs/integration/19-handover-checklist-endpoints-pr-summary.md` — task-start preservation checksum.
- `docs/integration/20-catalog-visibility-category-security-fixes.md` — task-start preservation checksum.
- `docs/integration/21-final-local-reconciliation-handover.md` — task-start preservation checksum.
- `docs/integration/BACKEND_API_AUDIT.md` — task-start preservation checksum.
- `docs/integration/BACKEND_IMPLEMENTATION_REPORT.md` — task-start preservation checksum.
- `docs/integration/FARMER_PRODUCT_ADMIN_FARM_API_CHANGES.md` — task-start preservation checksum.
- `docs/integration/README.md` — task-start preservation checksum.
- `docs/integration/review/05-local-demo-verification.md` — task-start preservation checksum.
- `docs/integration/review/06-backend-feature-parity-matrix.md` — task-start preservation checksum.
- `docs/integration/review/07-android-integration-audit.md` — task-start preservation checksum.
- `docs/integration/review/08-farmer-inspection-verification-audit.md` — task-start preservation checksum.
- `docs/integration/review/09-backend-migration-plan.md` — task-start preservation checksum.
- `docs/integration/review/10-open-decisions-technical-risks.md` — task-start preservation checksum.
- `docs/integration/review/11-phase1-security-implementation-report.md` — task-start preservation checksum.
- `docs/integration/review/12-phase1-upstream-reconciliation.md` — task-start preservation checksum.
- `docs/integration/review/13-local-schema-migration-plan.md` — task-start preservation checksum.
- `docs/integration/review/14-isolated-migration-rehearsal.md` — task-start preservation checksum.
- `docs/integration/review/15-isolated-migration-rehearsal-results.md` — task-start preservation checksum.
- `docs/integration/review/16-local-migration-backend-verification.md` — task-start preservation checksum.
- `docs/integration/review/README.md` — task-start preservation checksum.
- `docs/integration/schema-review/SHA256SUMS.txt` — task-start preservation checksum.
- `docs/integration/schema-review/apply-reviewed-schema.psql` — task-start preservation checksum.
- `docs/integration/schema-review/preflight.sql` — task-start preservation checksum.
- `docs/integration/schema-review/preservation-query.sql` — task-start preservation checksum.
- `docs/integration/schema-review/verify.sql` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/KilivanaBackendApplication.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AddressController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminBuyerController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminDriverController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminFarmerController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminInspectorController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminOperationsController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminProductController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AdminSupplierController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/AuditLogController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/InspectionController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/NotificationController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/ProfileController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/UserController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/controller/UserProfileController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/AddressRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/AddressResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/AdminBuyerResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/AdminDriverResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/AdminSupplierResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/AuditLogResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/BuyerProfileRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/BuyerProfileResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/DashboardStatsResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/DriverCapacityFormatter.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/DriverProfileRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/DriverProfileResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/DriverRegistrationRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/FarmerProfileRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/FarmerProfileResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/InspectionRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/InspectionResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/InspectorProfileRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/InspectorProfileResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/NotificationResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/SupplierProfileRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/SupplierProfileResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/SupplierRegistrationRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/UserProfileUpdateRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/UserRegistrationRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/dto/UserResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/Address.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/AuditLog.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/BuyerProfile.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/DriverProfile.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/DriverProfileImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/FarmerProfile.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/FarmerProfileImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/Inspection.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/InspectionEvidenceImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/InspectorProfile.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/InspectorProfileImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/Notification.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/SupplierProfile.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/SupplierProfileImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/entity/User.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/AddressRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/AuditLogRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/BuyerProfileRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/DriverProfileImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/DriverProfileRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/FarmerProfileImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/FarmerProfileRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/InspectionEvidenceImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/InspectionRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/InspectorProfileImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/InspectorProfileRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/NotificationRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/SupplierProfileImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/SupplierProfileRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/repository/UserRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/AddressService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/AdminBuyerService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/AdminDriverService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/AdminSupplierService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/AuditLogService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/DashboardService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/InspectionService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/NotificationService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/ProfileService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/admin/service/UserService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/controller/AuthController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/controller/HealthController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/controller/PublicFarmerController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/controller/RegionController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/controller/StoredImageController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/dto/ApiResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/dto/AuthLoginRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/dto/AuthTokenResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/dto/ImageResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/dto/PasswordResetRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/dto/RefreshTokenRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/entity/BaseEntity.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/entity/BaseImageEntity.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/entity/PasswordResetToken.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/entity/StoredImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/CropCategory.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/CropStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/DeliveryStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/DisputeStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/DocumentStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/DocumentType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/DriverStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/EnumNameMatcher.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/FarmStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/InspectionResult.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/InspectionStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/KycStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/OrderStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/OwnershipType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/PaymentMethod.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/PaymentStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/ProductStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/QualityGrade.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/SellerType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/UserRole.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/UserStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/VehicleType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/enums/VerificationStatus.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/BadRequestException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/ConflictException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/ForbiddenException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/GlobalExceptionHandler.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/ResourceNotFoundException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/ServiceUnavailableException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/TooManyRequestsException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/exception/UnauthorizedException.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/repository/ImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/repository/PasswordResetTokenRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/repository/StoredImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/AuthService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/DatabaseImageStorage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/DatabaseImageStorageProvider.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/ImageStorage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/KenyaCounty.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/KenyanIdType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/LocalImageStorage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/common/service/UserReferenceCodeGenerator.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/CorsConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/DarajaProperties.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/DatabaseUrlEnvironmentPostProcessor.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/DevelopmentDataSeeder.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/DotenvEnvironmentPostProcessor.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/IntegrationReadinessLogger.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/LocalStorageWebConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/OpenApiConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/RequestLoggingFilter.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/SecurityConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/SeedDataRunner.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/config/WebClientConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/dto/DarajaCallbackRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/dto/DarajaCallbackResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/dto/PublicFarmerProfileResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/dto/StkPushInitiateRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/dto/StkPushInitiateResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/dto/StkPushResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/CartCompatibilityController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/CartController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/CatalogCompatibilityController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/CategoryController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/CropTypeController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/DarajaCallbackController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/DisputeController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/EcommerceNotificationController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/OrderController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/PaymentController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/controller/ProductController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/CartItemRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/CartResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/CategoryRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/CategoryResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/DisputeRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/DisputeResolutionRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/DisputeResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderItemRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderItemResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/PaymentRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/PaymentResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/ProductImageResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/ProductRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/dto/ProductResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/Cart.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/CartItem.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/Category.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/CategoryImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/Dispute.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/Order.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/OrderEvent.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/OrderItem.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/Payment.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/Product.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/entity/ProductImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/enums/EcommerceNotificationType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/CartItemRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/CartRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/CategoryImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/CategoryRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/DisputeRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderEventRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderItemRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/PaymentRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/ProductImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/repository/ProductRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/CartService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/CategoryService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/DisputeService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/EcommerceNotificationService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/OrderService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/PaymentService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/ecommerce/service/ProductService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/controller/CropController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/controller/FarmController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/controller/FarmerFarmController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/controller/KycController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/CropResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/CropSummaryDto.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/CropTypeDto.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/FarmImageResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/FarmRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/FarmResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/FarmerListItem.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/FarmerRegistrationRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/FarmerResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/dto/KycDocumentResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/entity/Crop.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/entity/CropType.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/entity/Farm.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/entity/FarmImage.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/entity/KycDocument.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/repository/CropRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/repository/CropTypeRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/repository/FarmImageRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/repository/FarmRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/repository/KycDocumentRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/farm/service/AdminFarmerService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/config/TrackingConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/config/TrackingProperties.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/config/WebSocketConfig.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/DriverLocationController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/DriverProfileController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/LogisticsCompatibilityController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/LogisticsJobController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/ProofOfDeliveryController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/TrackingEventController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/TrackingWebSocketController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/controller/TripsController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/dto/DriverLocationResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/dto/LocationEvent.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/dto/LocationUpdateRequest.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/dto/LocationUpdateResponse.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/dto/TrackingStoppedEvent.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/entity/DriverLatestLocation.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/entity/DriverLocation.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/entity/LogisticsJob.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/entity/ProofOfDelivery.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/entity/TrackingEvent.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/repository/DriverLatestLocationRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/repository/DriverLocationRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/repository/LogisticsJobRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/repository/ProofOfDeliveryRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/repository/TrackingEventRepository.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/DriverLocationService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/LocationRateLimiter.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/LocationValidator.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/LogisticsService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/TrackingAuthorizationService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/TrackingBroadcastService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/TrackingChannelInterceptor.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/logistics/service/TrackingStopService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/mail/DeliveryOtpNotifier.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/mail/DriverOtpEmailTemplate.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/mail/MailAdminController.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/mail/SendGridBootstrapValidator.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/mail/SendGridMailService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/mail/SendGridProperties.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/AccountAccessValidator.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/CurrentUser.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/JwtAuthenticationFilter.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/JwtHandshakeInterceptor.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/JwtProperties.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/JwtService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/ProductVisibility.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/ResourceAuthorization.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/security/RestAuthenticationErrorHandler.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/service/DarajaCallbackService.java` — task-start preservation checksum.
- `src/main/java/com/kilivana/backend/service/DarajaService.java` — task-start preservation checksum.
- `src/main/resources/META-INF/spring.factories` — task-start preservation checksum.
- `src/main/resources/application.properties` — task-start preservation checksum.
- `src/main/resources/email/driver-delivery-otp.html` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/admin/controller/AdminFarmerControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/admin/controller/ProfileControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/admin/dto/DriverCapacityFormatterTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/admin/dto/DriverEnumParsingTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/admin/service/UserServiceUsernameRegionTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/controller/HealthControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/controller/PublicFarmerControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/enums/EnumWireFormatTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/enums/PaymentMethodTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/service/AuthServiceEmailNormalizationTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/service/KenyaLocaleTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/service/PasswordResetFlowTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/config/BootstrapConfigTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/config/DatabaseUrlEnvironmentPostProcessorTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/config/RequestLoggingFilterTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/ecommerce/service/DisputeServiceTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/ecommerce/service/OrderServiceTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/ecommerce/service/PaymentCallbackTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/ecommerce/service/PaymentServiceTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/ecommerce/service/ProductImageManagementTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/ecommerce/service/ProductStockSafetyTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/farm/controller/AdminFarmVisibilityTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/farm/controller/FarmerFarmControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/logistics/controller/DriverProfileControllerTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/logistics/service/DeliveryOtpFlowTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/logistics/service/LocationValidatorTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/logistics/service/LogisticsServiceTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/logistics/service/TrackingChannelInterceptorTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/logistics/service/TrackingDestinationTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/mail/SendGridMailServiceTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/repository/FarmerProductDatabaseTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/repository/OptionalFilterQueryTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/AccountAccessValidatorTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/CatalogVisibilityTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/JwtAccountEnforcementTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/JwtHandshakeAccountTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/LocalProductImageVisibilityTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/ProductManagementSecurityTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/service/DarajaServiceTest.java` — task-start preservation checksum.
- `src/test/java/com/kilivana/backend/support/JwtTestSupport.java` — task-start preservation checksum.
