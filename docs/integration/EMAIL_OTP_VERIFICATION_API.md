# Kilivana role-aware email OTP verification

Implemented on `feature/supplier-api-updates`, based on `bf61030`, while preserving the prior uncommitted product/farm work. This document describes the new backend contract. Android and Admin Portal source were not changed, and no live or shared database or email recipient was used for testing.

## Inspection and design decisions

The existing AuthService, UserService, UserRole, JWT filter/account validator, SendGrid service, public registration aliases, Admin provisioning routes, password-reset tokens and delivery OTP were inspected and reused where appropriate. The existing `verificationStatus` is actively used by Admin farmer approval and must not be repurposed for email proof, despite its old comment. Those comments/schema descriptions are corrected. Password reset and delivery OTP have different purposes and retain their own credential stores and endpoints.

Public registration documentation claimed Inspector provisioning was restricted, but the actual `isStaff()` predicate blocked only ADMIN. Public AuthService registration now explicitly rejects ADMIN and INSPECTOR. The existing DRIVER public registration flow is retained for compatibility; no new public staff role is introduced. CUSTOMER is a display name only: the existing enum/API role remains BUYER.

All HTTP account-creation routes now create unverified accounts and use the same email OTP implementation. Existing user/farm/product approvals and KYC are not changed. Existing users are grandfathered by the migration; grandfathering records compatibility, not historical proof that an email challenge was completed.

## Endpoint overview

All payloads use the existing ApiResponse envelope. OTP endpoints are anonymous POST routes so unverified accounts can complete onboarding without receiving JWTs.

| HTTP / URL | Authorization | Request DTO / payload | Success | Implementation / impact |
|---|---|---|---|---|
| POST `/api/v1/auth/register`, `/api/auth/register` | Public; FARMER, SUPPLIER, BUYER, and existing DRIVER only | Existing UserRegistrationRequest | 201 ApiResponse<UserResponse> | AuthController -> AuthService; send limit acquired before registration transaction; emailVerified=false and initial challenge ID added |
| POST `/api/v1/users`, `/api/users` | Same public policy | Same UserRegistrationRequest | 201 ApiResponse<UserResponse> | UserProfileController -> same AuthService; no bypass alias |
| POST `/api/v1/admin/users` | Verified authenticated ADMIN only | Existing UserRegistrationRequest | 201 ApiResponse<UserResponse> | UserController -> UserService.createUser; authorized INSPECTOR/ADMIN provisioning remains available, followed by email proof |
| POST `/api/v1/admin/suppliers` | Verified authenticated ADMIN only | Existing SupplierRegistrationRequest | 201 ApiResponse<AdminSupplierResponse> | Existing combined account/profile transaction; adds emailVerified and initial challenge ID; pending status remains pending |
| POST `/api/v1/admin/drivers` | Verified authenticated ADMIN only | Existing DriverRegistrationRequest | 201 ApiResponse<AdminDriverResponse> | Existing combined account/profile transaction; same gate; driver KYC unchanged |
| POST `/api/v1/auth/email-verification/request` | Public | EmailOtpRequest: email | 202 ApiResponse<EmailOtpChallengeResponse> | Request code for registered email; generic eligibility acknowledgment |
| POST `/api/v1/auth/email-verification/resend` | Public | Same EmailOtpRequest | Same 202 response | Same implementation and rate budgets as request; queued successful dispatch rotates active challenge |
| POST `/api/v1/auth/email-verification/verify` | Public | EmailOtpVerifyRequest: challengeId, email, code | 200 ApiResponse<Void> | Confirms email only; never issues JWTs or changes role/approval/status |
| POST `/api/auth/email-verification/request`, `/resend`, `/verify` | Same as canonical aliases | Same DTOs | Same responses | Legacy auth base remains supported |
| POST `/api/v1/auth/login`, `/api/auth/login` | Existing email/password authentication | Existing AuthLoginRequest | Existing 200 AuthTokenResponse after verification | Correct password for unverified account returns 403; invalid credentials remain generic 401 |
| POST `/api/v1/auth/refresh`, `/api/auth/refresh` | Existing refresh credential validation | Existing RefreshTokenRequest | Existing 200 AuthTokenResponse after verification | Unverified account returns 403; refresh cannot bypass email gate |
| GET `/api/v1/auth/me`, `/api/auth/me` and existing UserResponse reads | Existing authorization | Existing requests | Same envelope with additive email fields | Signed access JWT is rejected with 401 while current account email is unverified |
| PUT `/api/v1/users/me`, `/api/users/me`, `/api/v1/admin/users/{id}`, `/api/v1/admin/suppliers/{id}`, `/api/v1/admin/drivers/{id}` | Existing self/Admin ownership policy | Existing update DTOs | Existing response shape with additive email flags where applicable | Changing the actual email clears its verified flag/timestamp. Case-only/whitespace-equivalent address changes do not. Request a new OTP for the new registered email |

No new role, alternative login system, approval endpoint, public Inspector route, or password-recovery credential type was added. Public route allowlisting names only the six concrete OTP POST paths. Other authentication/authorization rules are retained.

## Registration request and response

Use the existing registration payload; do not submit emailVerified, approval fields, OTP purpose or an elevated role. Server-created email fields cannot be set through UserRegistrationRequest.

```http
POST /api/v1/auth/register
Content-Type: application/json

{"name":"Example Supplier","email":"supplier@example.invalid","phone":"0700000000","password":"<user-selected-password>","role":"SUPPLIER","username":"example.supplier","region":"Kiambu"}
```

Representative 201 response (synthetic IDs and timestamps; no OTP in the response):

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 41,
    "name": "Example Supplier",
    "email": "supplier@example.invalid",
    "phone": "0700000000",
    "username": "example.supplier",
    "referenceCode": "S-041",
    "region": "Kiambu",
    "role": "SUPPLIER",
    "status": "ACTIVE",
    "verificationStatus": "NOT_REQUIRED",
    "emailVerified": false,
    "emailVerifiedAt": null,
    "emailVerificationChallengeId": "84e8230b-0d66-497b-96e5-524e2c65e3bd",
    "createdAt": "2026-10-21T12:00:00"
  },
  "timestamp": "2026-10-21T12:00:00",
  "error": null
}
```

`emailVerified`, `emailVerifiedAt` and `emailVerificationChallengeId` are additive UserResponse fields. The challenge ID is populated only for successful creation. Login/profile responses normally have a null initial challenge field; the public request endpoint obtains a new challenge after an app restart. AdminSupplierResponse and AdminDriverResponse add emailVerified and emailVerificationChallengeId to their existing fields.

Initial SendGrid rejection, disabled/unconfigured mail, or provider exception returns **503 SERVICE_UNAVAILABLE** and rolls back the new account/profile and challenge. Rate budgets remain consumed. No successful registration response is returned for an unsent initial verification message. The client should keep the form and retry after any cooldown; duplicate email/phone conflicts retain existing behavior.

## Request and resend

```http
POST /api/v1/auth/email-verification/request
Content-Type: application/json

{"email":"supplier@example.invalid"}
```

Resend uses the same body at `/api/v1/auth/email-verification/resend`. Registered eligible, unknown, already verified, attempt-locked and delivery-failed requests receive the same 202 shape and eligibility message:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "challengeId": "84e8230b-0d66-497b-96e5-524e2c65e3bd",
    "expiresInSeconds": 600,
    "resendAfterSeconds": 60,
    "message": "If the account is eligible, a verification email will be sent."
  },
  "timestamp": "2026-10-21T12:00:00",
  "error": null
}
```

A fresh opaque UUID from the durable send budget is returned in every accepted response. All accepted request/resend jobs are queued before any account lookup, so HTTP acknowledgment does not wait on account-specific mail-provider latency. Unknown/ineligible/failed-delivery UUIDs are decoys and cannot verify an account. No `exists`, `sent`, `role`, `userId`, remaining-attempt count or provider status is disclosed. Successful resend replaces the stored challenge and invalidates the previous challenge ID. Failed resend leaves the previous working challenge unchanged. A 202 acknowledges queued work, not provider acceptance or inbox arrival. The bounded queue has two workers and capacity 100. Queue saturation returns the same generic 503 for every address; it does not look up the account.

The queue is in-memory: a restart may discard pending jobs. No plaintext code is queued; it is generated only when a worker dispatches. Retry/resend after cooldown recovers from lost work. The latest send-ticket UUID is stored in the rate bucket, and workers recheck it under the account lock so delayed jobs cannot overwrite newer resends, including across instances.

Do not treat 202 as proof that an account exists or that mail arrived. If no new email arrives, wait for cooldown and request another code. A code from an earlier email must not be submitted with a newer challenge ID. Keep challenge IDs private in application state; never put submitted OTPs in URLs, analytics or logs.

## Verify

```http
POST /api/v1/auth/email-verification/verify
Content-Type: application/json

{"challengeId":"84e8230b-0d66-497b-96e5-524e2c65e3bd","email":"supplier@example.invalid","code":"123456"}
```

`123456` is a documentation-only dummy code. Preserve leading zeros and submit the code as a six-character ASCII digit string. UUID/email/code are required; code must match `[0-9]{6}`. Role, account ID and purpose are not accepted as authorization inputs.

```json
{"success":true,"message":"Email verified successfully. Continue to login and any required approval steps.","data":null,"timestamp":"2026-10-21T12:00:00","error":null}
```

Verify does not issue access/refresh tokens. Continue to the existing login flow. Reuse of a consumed code/challenge returns generic 400; verification is single-use rather than idempotent. Correct proof changes only User.emailVerified=true, User.emailVerifiedAt, and challenge consumption time. Account role, UserStatus, VerificationStatus, farm status, product approval and driver KYC remain untouched.

## Errors and timing hints

| Status / error code | Meaning and client action |
|---|---|
| 400 VALIDATION_ERROR | Invalid email, missing UUID, or code not exactly six digits; fix input |
| 400 MALFORMED_REQUEST_BODY | Malformed JSON/UUID; fix payload |
| 400 BAD_REQUEST | Generic invalid/expired/used/stale challenge, mismatched account/email/role/purpose, or exhausted verification budget; no account detail disclosed |
| 400 BAD_REQUEST on public registration | Staff role not permitted; use authorized Admin provisioning |
| 401 UNAUTHORIZED | Invalid login/refresh/bearer credentials, stale role, or unverified account using a bearer token |
| 403 FORBIDDEN | Valid login/refresh proof but email not verified, or existing RBAC rejection |
| 409 CONFLICT | Existing registration uniqueness behavior (some Admin provisioning validation paths retain 400) |
| 429 TOO_MANY_REQUESTS | Rate budget/cooldown; honor Retry-After response header |
| 503 SERVICE_UNAVAILABLE | Initial dispatch unavailable (account creation rolled back), or request/resend queue is full; honor cooldown and retry |
| 500 | Unexpected database/server failure; no success should be inferred |

Generic verification failure:

```json
{"success":false,"message":"Invalid or expired email verification challenge","data":null,"timestamp":"2026-10-21T12:00:00","error":{"code":"BAD_REQUEST","details":"Invalid or expired email verification challenge"}}
```

Rate-limited response:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
```

```json
{"success":false,"message":"Too many verification requests. Please try again later.","data":null,"timestamp":"2026-10-21T12:00:00","error":{"code":"TOO_MANY_REQUESTS","details":"Too many verification requests. Please try again later."}}
```

Actual Retry-After is computed conservatively and may be 61 seconds immediately after a send, or the remaining hourly window. CORS exposes Retry-After for the Admin web client. The fixed successful response hints are 600-second code expiry and 60-second nominal resend cooldown, independent of account existence.

## Limits, binding and concurrency

- Cryptographically secure SecureRandom generates six digits, including possible leading zeros. Codes expire at exactly 600 seconds.
- A dedicated shared HMAC-SHA-256 key hashes purpose, user ID, normalized registered email, current role, challenge UUID and code. Only a 64-character keyed digest is stored. Plaintext OTPs exist only in memory and the email body. Comparisons use MessageDigest.isEqual.
- One challenge row per account; successful resend rotates its UUID/hash/expiry. Wrong attempts persist across resends.
- Maximum five incorrect attempts per account per anchored one-hour attempt window. After exhaustion even a correct code is rejected until the window resets; resend does not reset or bypass the budget. The general failure remains generic.
- Send budget: five requests per normalized email per hour, minimum 60 seconds between sends. Cooldown survives the hourly counter reset. Identical durable buckets apply to unknown and known addresses, including failed deliveries.
- IP budgets: 60 send requests/hour across addresses, 120 verification requests/hour. Rate keys are HMACs; the rate table does not store plaintext unknown emails or IPs.
- The effective servlet remote address supplies the IP identity; raw X-Forwarded-For is not trusted by the limiter. Configure the trusted reverse-proxy boundary correctly so a proxy neither permits spoofing nor collapses all users into one address.
- Account-row pessimistic locks serialize challenge issuance/rotation, failed attempts and consumption across application instances. State is refreshed after acquiring the lock to avoid stale persistence-context reads. Failed-attempt exceptions explicitly commit their counters.
- Rate limits commit in a separate phase before account transactions. Controllers acquire initial send budgets before starting registration/provisioning. OTP request/verify orchestration suspends surrounding transactions before rate limiting, avoiding nested connection holds under concurrency.
- Successful delivery/initial account writes remain transactional. SendGrid non-2xx/null/exception responses count as failures. Provider exception contents are not logged because they can echo request bodies or keys. The existing HTTP log filter logs method/path/status, not request bodies.

Request/resend queueing happens before account lookup, so provider latency and delivery failures do not distinguish eligible from unknown email addresses in HTTP responses. Queue-full failure is account-independent. This is not a claim that an entire HTTP/database stack is constant-time. Use normal gateway abuse controls and do not add raw request-body or provider-payload logging. Existing registration uniqueness responses and password-recovery contracts are unchanged. Password reset never sets emailVerified or approval state.

## Role-specific flows

| Backend role | Email label / subject | Enrollment flow |
|---|---|---|
| FARMER | Farmer; Kilivana — Farmer Email Verification | Public registration -> OTP -> login -> existing farmer/profile/farm/product approval process |
| SUPPLIER | Supplier; Kilivana — Supplier Email Verification | Public registration or existing Admin Supplier provisioning -> OTP -> login -> existing supplier workflow |
| BUYER | Customer; Kilivana — Customer Email Verification | Public registration -> OTP -> login; no CUSTOMER enum added |
| INSPECTOR | Inspector; Kilivana — Inspector Email Verification | Verified Admin POST /api/v1/admin/users with role INSPECTOR -> assigned email receives code -> Inspector verifies -> login and existing staff/profile policy |
| DRIVER | Driver; Kilivana — Driver Email Verification | Existing public/Admin provisioning paths retained -> OTP -> login; licence/KYC untouched |
| ADMIN | Administrator; Kilivana — Administrator Email Verification | Existing authorized Admin provisioning only; never public registration; email proof grants no role |

The professional HTML template uses fixed server role labels, Kilivana branding, mobile-width table layout, prominent six-digit code, ten-minute expiry, single-use wording, a do-not-share warning and ignore-if-unrequested guidance. No untrusted display name is interpolated. Only the stored account role controls the label and HMAC binding.

## Database migration and deployment

Review `db/migrations/V20__email_otp_verification.sql` before deployment. It is manual like existing migrations; no Flyway/auto-migration system was added.

1. Add users.email_verified with TRUE for existing rows, then set its default FALSE for future inserts. Existing roles/statuses/approvals are unchanged, including approved Farmers and provisioned staff. A null email_verified_at for a grandfathered user does not establish historical verification time.
2. Add nullable users.email_verified_at.
3. Create email_verification_challenges with one row per user, unique UUID, registered email/role/purpose binding, HMAC digest, expiry/consumption and durable attempt budget. User deletion cascades only its credential row.
4. Create email_otp_rate_limits with digest bucket keys, reset time, request count, last-request time and latest send-ticket UUID, plus expiry index.

No existing record is deleted and no account role is rewritten. Existing Java builder defaults retain trusted seed/test compatibility; all HTTP creation paths explicitly set false. Development seeding remains controlled by the existing disabled-by-default seeder and is not a public enrollment path.

Do not apply V20 to Daniel's/shared/production database from this task. The exact SQL was tested only in a fresh schema inside the disposable test database. No live-data backfill was run. V19 from the prior product task remains unchanged and independent; do not blindly replay historical role/state-normalizing migrations.

Before deployment, configure:

```text
EMAIL_OTP_HMAC_SECRET=<dedicated cryptographically random secret, at least 32 bytes>
SENDGRID_ENABLED=true
SENDGRID_API_KEY=<secret with required mail-send permissions>
SENDGRID_FROM_EMAIL=<verified sender identity>
SENDGRID_FROM_NAME=Kilivana
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

The HMAC secret has no fallback, no default development key and no JWT/SendGrid-key reuse. Missing/short configuration fails application startup. Share the same secret across instances and restarts; store it outside Git. Generate it using an approved secrets manager or a cryptographic generator. Rotation invalidates outstanding challenges and changes rate digest keys; coordinate deployment and let users request fresh codes.

Deploy the migration and all authentication nodes in a coordinated rollout, with registration/authentication drained or paused while old nodes are removed. Older binaries do not enforce the new gate; do not leave them serving auth traffic. New binaries require the columns/tables and the key. Set schema validation rather than relying on the repository's existing ddl-auto=update default.

Have deployment owners verify SendGrid sender/domain status, outbound network access and real mailbox deliverability in an authorized environment. Tests use a mocked provider and do not certify inbox delivery. Rate-limit bucket retention can be maintained by an independently reviewed job that removes only rows whose reset window expired sufficiently long ago; never reset active budgets or delete credential rows to bypass failed attempts. No production cleanup job or data repair was executed here.

## Android integration

- Farmer/Supplier/Customer registration: retain email and emailVerificationChallengeId from 201; navigate to OTP entry instead of immediately logging in. Send code as text, preserving leading zeros.
- On app restart/login 403 requiring email verification, call the anonymous request endpoint with the registered email; keep the returned challenge ID and display generic delivery guidance.
- Show a ten-minute expiry and sixty-second resend timer; HTTP 429 Retry-After overrides a local timer. Replace the current challenge only for the corresponding newly requested email/code; old challenge IDs are invalid after a successful resend.
- Call verify; on 200 discard the entered OTP and proceed to login. Clear any stored OTP on screen exit or successful verification. Do not log it or persist it in analytics.
- Treat generic 400 as invalid/expired/unusable proof; support resend or wait/retry without assuming account existence or displaying a server-supplied attempt count.
- Handle initial registration 503 by retaining the form and retrying, not by assuming an account was created. Normal existing uniqueness errors remain possible.
- Existing UserResponse decoders need three additive optional fields; Admin specialized response decoders need the two added fields. Existing token response fields and roles remain unchanged.
- Email changes trigger re-verification. Existing bearer/refresh authentication is denied until the new registered email is confirmed. The anonymous verification endpoints remain usable.
- An Inspector must not use public registration. Its onboarding UI uses an account already provisioned by an authorized Admin and requests/resends to that assigned email.

## Admin Portal integration

Use existing POST /api/v1/admin/users for Inspector provisioning, with unchanged role enum INSPECTOR and existing registration fields. Store or hand off the initial opaque challenge appropriately; an Inspector can obtain a new one through request/resend after cooldown. Admins do not receive the user's OTP in the API and must not mark emailVerified directly. Specialized Supplier/Driver provisioning remains atomic for account/profile plus initial verification dispatch.

Display emailVerified separately from verificationStatus, account status, farm use status and driver KYC. A successful OTP must not trigger Admin approval automatically. Handle 429/Retry-After and 503 without claiming provisioned accounts exist after rollback. Email edits require the recipient to reverify. No Admin Portal source was available or changed, and browser UI behavior was not tested.

The checked-in kilivana-openapi.json was inspected as an existing contract snapshot and left unchanged. New controller/DTO annotations feed runtime Swagger/OpenAPI; regenerate the static export from the reviewed, configured build before using it for client code generation. The prior product/farm audit documents remain intact.

## Testing and execution

JDK 21 and the Maven wrapper were used, preserving the existing Java 17 source target. The full final test suite includes stale queued-job suppression, account-independent dispatch acknowledgment, real PostgreSQL row locks, twelve-way correct/incorrect verification races, concurrent resends, known/unknown cooldown equivalence, hourly rollover, provider-failure rollback, all role templates and provisioning paths, signed-token gating, alias/DTO validation, email changes, approval separation and existing-user migration compatibility.

Tests require explicit opt-in and this exact disposable URL; they never use a shared datasource:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:55439/kilivana_audit'
$env:SPRING_DATASOURCE_USERNAME='audit_test'
$env:SPRING_DATASOURCE_PASSWORD='<test-container-only-password>'
$env:KILIVANA_RUN_EMAIL_OTP_DB_TESTS='true'
$env:KILIVANA_RUN_API_AUDIT_DB_TESTS='true'
$env:KILIVANA_RUN_ISOLATED_DB_TESTS='false'
$env:ENV_FILE='target/nonexistent-otp-test.env'
.\mvnw.cmd -o -B test
```

Only run those opt-ins with a fresh, disposable localhost PostgreSQL database. DataJpaTest intentionally creates/drops that isolated schema. The separate legacy baseline tests for port 55432 remain disabled. Provider API calls are mocked, and test secrets are not deployment credentials. PostgreSQL tests inject a deterministic direct dispatcher to test transactions/races; separate queue tests verify acknowledgment-before-lookup, generic saturation errors and secret-safe background failures. See EMAIL_OTP_IMPLEMENTATION_REPORT.md for final totals, file-level changes, Git status and all validation limits.
