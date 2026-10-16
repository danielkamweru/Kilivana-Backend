# Phase 1B — Token lifecycle security audit and design

## 1. Executive summary for Daniel

Design prepared 2026-10-08; review in progress, implementation pending. This is
a documentation-only audit, not an implemented or validated token architecture.
Verified branch `feature/android-backend-integration`, HEAD `a55b56e`, initially
clean working tree. Phase 1A's reset guard and both integration documents exist.
Daniel approved Phase 1A, as reported by the user; its local commit is
`a55b56e1a6f55b7a3ac6ec6bec18178cbde7c96e`. No deployment or merge is asserted.

Recommend 15-minute access JWTs and 14-day opaque, rotating refresh credentials
backed by PostgreSQL sessions. Preserve Long user IDs, roles, response envelope
and auth aliases. Revoke a device's refresh family on logout or replay; revoke
all families on password changes and account suspension. Require a coordinated
client upgrade and forced re-login at cutover, because existing refresh JWTs
have no server-side revocation record and may never expire.

No Java/configuration/migration changes, database connections, startup, test
execution, commit or push occurred during this design. Phase 1A's 145 passing
database-free tests are historical evidence, not Phase 1B validation.

## 2. Current implementation findings

Source code is authoritative; deployed environment values were not inspected.

| Component | Verified behavior |
| --- | --- |
| `security/JwtService.java` | JJWT 0.12.5 signs both access and refresh JWTs with the same HMAC key. Claims: `sub` string user ID, numeric `uid`, uppercase `role`, `email`, `type`, random `jti`, `iat`; `exp` only when configured duration is positive. No required issuer/audience/expiration or explicit application algorithm allowlist. |
| `security/JwtProperties.java` | Class defaults: access 86400000 ms (1 day), refresh 604800000 ms (7 days). These differ from application resource defaults. |
| `application.properties`, `.env.example` | Effective resource defaults `JWT_EXPIRATION=0`, `JWT_REFRESH_EXPIRATION=0`; tokens have no expiration. `JWT_SECRET` unset produces an ephemeral key. |
| `render.yaml` | Template supplies generated stable `JWT_SECRET`, access lifetime 31536000000 ms (1 year); no refresh lifetime override observed. This is a template, not verified deployment state. |
| `common/service/AuthService.java` | Login matches BCrypt password and denies INACTIVE/SUSPENDED. Refresh verifies JWT type, loads current user, denies these statuses and issues another pair. Old refresh JWT remains valid; no rotation consumption, session storage or replay detection. |
| `common/controller/AuthController.java` | Both `/api/v1/auth` and `/api/auth` aliases: register, login, refresh, logout, forgot-password, reset-password, me. Logout only returns success. Refresh body has no `@Valid`. Phase 1A reset throws ServiceUnavailableException. |
| `security/JwtAuthenticationFilter.java` | Checks access type/signature, extracts ID and role, builds Long principal and `ROLE_<enum>` authority. Does not load user, status, password-change state or session. Parses token repeatedly; rejected-token exception message is debug logged. |
| `config/SecurityConfig.java` | Stateless Spring Security; no servlet session. Public login/register/refresh/recovery aliases; logout requires authentication through catch-all. ADMIN required for `/api/v1/admin/**` and selected logistics/order mutations. CSRF disabled; CORS resource default permits wildcard origins with credentials. |
| `security/CurrentUser.java` | Requires Long principal; role checks use `ROLE_` prefix. Do not change principal/user ID types. |
| DTOs | `AuthTokenResponse`: accessToken, refreshToken, tokenType, nullable Long expiresIn, user. Actual expiresIn is **seconds**, from milliseconds / 1000. `RefreshTokenRequest` contains only a String refreshToken without constraints. `AuthLoginRequest`, `UserResponse`, `ApiResponse` retain current contracts. |
| Errors | RestAuthenticationErrorHandler returns generic 401 UNAUTHORIZED or 403 FORBIDDEN. GlobalExceptionHandler maps UnauthorizedException to 401, ServiceUnavailableException to 503; generic exceptions to 500. No lifecycle-specific refresh codes. |
| Users | `admin/entity/User.java`: Long identity key, string enums, BCrypt password hash; statusChangedAt/statusChangedBy/suspensionReason exist but no credential version or refresh sessions. ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION; no DISABLED enum. PENDING_VERIFICATION is currently allowed to login/refresh. Roles: FARMER, BUYER, SUPPLIER, INSPECTOR, DRIVER, ADMIN. |

Tests inspected: AuthControllerTest (mock service; login/register/aliases/reset),
AuthServiceEmailNormalizationTest (real service with repository/JWT mocks),
PasswordResetSecurityTest, JwtTestSupport (real signed bearer test helper),
ProfileControllerTest and DriverProfileControllerTest (security MVC slices).
No dedicated access-expiry, refresh-rotation, logout or replay tests were found.

Password writes requiring lifecycle hooks: UserService.updateUser,
AdminSupplierService.updateSupplier (password field), SeedDataRunner's optional
password enforcement. AuthService.register/UserService.createUser create new
accounts; reset stays disabled. Account state paths include UserService status,
suspend/activate/delete methods, AdminSupplierService status/suspend/activate,
farm/service/AdminFarmerService delete/suspend/activate and
admin/controller/AdminFarmerController inactive transition. ProfileService's
profile status is a different domain state, not automatically UserStatus.

Related account endpoints requiring regression and revocation hooks:
`PUT /api/v1/admin/users/{id}` (password changes), PUT/PATCH
`/api/v1/admin/users/{id}/status`, PUT `/{id}/suspend` and `/{id}/activate`,
DELETE `/api/v1/admin/users/{id}`; PUT/DELETE
`/api/v1/admin/suppliers/{userId}`, PUT `/{userId}/suspend` and
`/{userId}/unsuspend` under that supplier prefix; PUT
`/api/v1/admin/farmers/{userId}/suspend` and `/{userId}/unsuspend`, and POST
`/api/v1/admin/farmers/{id}/reject` (sets account INACTIVE). Related create,
approve and reactivation routes must not accidentally resurrect old sessions.
No account-management request shapes are proposed to change.

Existing general README has stale assertions about demo tokens and inconsistent
expiry defaults. FRONTEND_BACKEND_INTEGRATION_REPORT says expiresIn becomes
milliseconds; current code returns seconds. It describes older Android Driver
and Angular Admin snapshots, not verified current client implementations.
Those files were not edited in this phase.

## 3. Security risks

- Critical: stolen non-expiring access JWT can remain valid indefinitely; the
  Render template's one-year access lifetime also gives a large exposure window.
- High: old refresh JWTs are replayable even after successful refresh or logout.
- High: suspended, inactive or deleted accounts can still authenticate using
  existing access claims; password changes do not invalidate existing credentials.
- High: logout gives a success message without any server-side revocation.
- Operational: missing secret generates per-process ephemeral keys, breaking
  restarts/multiple replicas. Key length is checked but entropy cannot be inferred
  from character count; raw/Base64 ambiguity and lazy initialization need review.
- Validation gaps: absent exp/iss/aud accepted, no application algorithm pinning,
  repeated parsing may cross expiry boundaries between checks. Error-message
  logging can expose parser details; no evidence of explicit token logging was
  found, but exception/DTO logging must be restricted.
- Adjacent risk: wildcard credentialed CORS and browser token persistence require
  review before a cookie-based design. This phase does not add cookies or change
  unrelated endpoint permissions. Phase 1A recovery guard must remain closed.

## 4. Proposed architecture and security behavior

### Access JWTs

Require explicit exp = iat + 900 seconds, finite positive configuration and a
stable production signing key. Keep numeric uid, string sub, uppercase role,
email, type=access, iat/jti; add stable iss, aud and numeric credential version
`ver`. Optional `sid` identifies the refresh family but grants no authorization.
Use one explicit HMAC algorithm per configured key (recommend HS256 with at
least 32 random decoded bytes); existing library auto-selects based on key
strength, so algorithm/key migration must be coordinated with legacy cutoff.
Pin issuer/audience/algorithm and validate sub/uid consistency, valid Long ID,
known role, type, required exp/iat, maximum lifetime and future iat. Reject
unsigned, malformed, wrong-type, wrong-key and unsupported-algorithm tokens.
Parse once to a validated identity. Proposed allowed skew: 30 seconds, measured
and tested; access exposure after logout is at most 15 minutes plus skew.

Keep ROLE_ and CurrentUser semantics. For immediate account enforcement, load
current user security state on protected requests: reject missing, INACTIVE,
SUSPENDED, version mismatch or role mismatch. Baseline design uses a direct
database read, not an unbounded cache. This makes security state immediately
visible to subsequent requests after commit; already-authorized in-flight work
may complete. If availability/performance requires caching, bounded staleness
must be explicitly approved and tested. DB failure is 503, never allow access.
Do not infer a blocked account from verificationStatus without a separate policy.

### Opaque refresh credentials and sessions

Generate `rt1_` + unpadded base64url of 32 SecureRandom bytes. Treat the entire
string as an opaque secret, transmit only via TLS JSON body, never in query URLs.
Store SHA-256 of exact token bytes (fixed 32-byte digest), never raw credential.
Random 256-bit entropy permits a fast indexed hash; password hashes retain
BCrypt. A versioned HMAC pepper is optional defense in depth, not required for
this high-entropy design; adding it would require key-version migration rules.
Unique hash constraint; regenerate on the extremely unlikely insertion collision.

Each login creates an independent device/session family with a fixed absolute
14-day expiry (1209600 seconds), not an endlessly sliding window. Each refresh
consumes the current token and generates a successor with the same family expiry.
Keep consumed hashes until the family expires plus retention so replay can be
recognized. Unknown token -> generic denial; known consumed token -> revoke the
whole family, including the successor. Theft of the current unused token is not
detectable until reuse; rotation cannot prevent its first use. Device labels/IP
are optional support metadata, never proof of device identity.

### Transactions, concurrency and replay

Lock user then session in a consistent order for login/refresh, account mutation
and revocation. Resolve a presented digest to current session or history first,
then lock and **recheck** all state. Serialize refreshes across replicas using
PostgreSQL row locks, not a JVM mutex. Check account/version, session expiration,
revocation and current digest under lock. Atomically insert consumed history,
replace current hash, increment generation and issue the pair; return only after
commit. Signing failure or transaction failure must not consume a token.

Replay must persist family revocation even though the API returns 401: do not
throw a rollback-triggering exception inside the transaction before committing
the security action. Return a denied outcome from a transactional service and
map it to the HTTP error after commit (or equivalent explicitly tested pattern).
Lock ordering also prevents password changes racing with new session creation.
Validate state again under lock; request arrival time is not the expiration test.

Recommend strict single use, no grace replay. Two simultaneous refresh requests:
one rotates; the other detects reuse and revokes the family. The first response
may arrive with an already-revoked successor. Clients must single-flight refresh
and coordinate browser tabs. A response lost after commit requires re-login;
hash-only storage cannot return the old plaintext successor. This usability
tradeoff requires Daniel's approval. Any retry grace/idempotency design needs a
separate theft/replay analysis and must not silently allow unlimited reuse.

### Logout, account changes and devices

Propose logout with refreshToken JSON body, no live access JWT required: possession
of the refresh credential only authorizes revoking its own family. Lookup includes
consumed history, so logout can race rotation safely. Lock as above; revoke family
idempotently, return generic 200 even for unknown/already-revoked valid-format
tokens. DB unavailability returns 503, not false success. Rate limit body-token
endpoints. Existing bearer/no-body logout remains a documented compatibility
concern, addressed during rollout below. Never derive a target from an untrusted
userId/sessionId supplied alone. Optional logout-all is a separately approved,
authenticated endpoint, not required to preserve existing API shape.

Ordinary logout/replay revokes refresh ability only: already-issued access JWTs
remain usable until exp plus skew while account/version remains eligible. Do not
claim immediate access revocation for logout without checking session state on
every protected request; that stronger option is an open decision.

Password changes: increment User credential version and revoke all families in
the same transaction. Require re-login on all devices, including initiating
device. Account suspension/inactivation, deletion and role changes similarly
revoke all sessions and increment version; reactivation does not resurrect old
sessions. Filter state checks reject old access tokens. All mutation paths must
use the same security-state helper, including optional seed enforcement. No
new password-reset/change API is introduced by this design.

Concurrent devices have independent families; logout/replay affects one, password
changes/suspension affect all. Recommend at most five active families per user;
reject additional login with an explicit session-limit response rather than
silently logging out another device. Final limit/admin policy needs approval.
PENDING_VERIFICATION remains eligible initially for compatibility; INACTIVE is
the existing disabled-account representation. Decide verification policy separately.

### Secrets, cleanup and logs

Require stable secrets supplied outside source, fail startup on missing/weak
production key, distinguish explicit development-only ephemeral behavior.
Initialize once safely. Planned keys use explicit Base64 encoding. If key rollover
is needed, use fixed allowed kid-to-key mapping, never URLs from headers; retain
old key only for a bounded access lifetime. Compromise response removes old key
immediately and revokes refresh families; new opaque sessions do not disappear
merely because an access signing key was rotated. Never log Authorization,
refresh/request/response bodies, hashes, signing secrets or Lombok DTO toString.
Log sanitized event codes, correlation IDs and internal family IDs with access
controls; redact tracing/proxy/error telemetry too. Add Cache-Control: no-store
to token responses. Inject Clock for tests; use UTC Instant/timestamptz and NTP.

Cleanup runs in bounded batches and preserves consumed history through family
absolute expiry plus proposed seven-day security retention. Use indexes below,
delete history before sessions; no unbounded full-table delete in request paths.
Expired sessions cannot refresh even if cleanup is delayed. Cleanup races must
not revive sessions or remove still-needed replay history. Rate-limit login and
refresh and cap payload length before hashing/parsing.

## 5. API contracts before and after

Both `/api/v1/auth` and `/api/auth` aliases retain the same outcome at cutover.
There are no separate Admin/e-commerce authentication providers in this backend.

| Operation | Before | Proposed final contract |
| --- | --- | --- |
| POST /login | email/password -> JWT pair; expiresIn nullable seconds | Same request/envelope/user/Long ID; access JWT + opaque refreshToken; tokenType Bearer; expiresIn=900 seconds. Optional additive refreshExpiresIn=1209600 and sessionId require DTO/client review. |
| POST /refresh | public JSON refreshToken JWT; reusable; returns pair | Public validated JSON `{refreshToken: opaque}`; same pair envelope; replacement token must be saved atomically. No access token needed. |
| POST /logout | authenticated bearer, no body; message only | Public body-token revocation of one family, generic 200 success message; malformed/missing body 400. No access token required. |
| GET /me, protected APIs | bearer claims only | Same bearer header/response/role checks plus exp and current account/version enforcement. |
| POST /register | user response, not token pair | Unchanged; do not accidentally auto-login or change response shape. |
| POST /reset-password | disabled 503 | Remains disabled. Forgot-password unchanged; not a completed recovery flow. |

Proposed envelope remains ApiResponse. Stable codes: 401 UNAUTHORIZED for missing,
invalid or expired access credentials (optional ACCESS_TOKEN_EXPIRED only for a
verified expiration failure); 403 FORBIDDEN for authorization denial; 401
INVALID_REFRESH_TOKEN uniformly for unknown/expired/revoked/replayed credentials;
400 VALIDATION_ERROR/MALFORMED_REQUEST_BODY for request errors; 429 TOO_MANY_REQUESTS;
503 SERVICE_UNAVAILABLE for storage/signing dependency outages; proposed 409
SESSION_LIMIT_REACHED at the configured device cap. Do not expose replay/account
existence distinctions externally. Refresh 401 means clear session, not recursive
refresh. A valid credential for disabled account is denied; general public login
failures should not disclose whether the account exists. Network/503 must not
automatically delete a client's session.

## 6. PostgreSQL schema proposal and safe introduction

Current users.id is Java Long/PostgreSQL bigint identity. Many profile/address/
notification entities store scalar Long userId without JPA relations; this is
not evidence that live database FKs exist. New session relationship should use
an explicit FK. Do not alter any user ID type or existing domain references.
Existing timestamps use LocalDateTime; new security timestamps use UTC Instant.

Recommend two additive tables so sessions have a stable identity and consumed
hash history can detect replay. This is a schema specification, not a migration.

| refresh_sessions column | Type / purpose |
| --- | --- |
| id | uuid primary key, server generated stable family/session ID |
| user_id | bigint NOT NULL FK users(id), indexed; proposed ON DELETE CASCADE for physical deletion, retention policy review required |
| current_token_hash | bytea NOT NULL UNIQUE, check digest length=32; no plaintext |
| generation | bigint NOT NULL default 0, nonnegative |
| credential_version | bigint NOT NULL, user's version at creation |
| created_at / expires_at | timestamptz NOT NULL, expiry > creation; absolute family deadline |
| last_rotated_at | nullable timestamptz |
| revoked_at / revoke_reason | nullable timestamptz / bounded varchar event code |
| client_label | nullable bounded varchar; untrusted support label, no raw UA/IP needed by default |

| refresh_token_history column | Type / purpose |
| --- | --- |
| token_hash | bytea primary key, length=32 |
| session_id | uuid NOT NULL FK refresh_sessions(id) ON DELETE CASCADE |
| generation | bigint NOT NULL; unique(session_id, generation) |
| issued_at / consumed_at | timestamptz NOT NULL |
| expires_at | timestamptz NOT NULL, original family deadline |
| replaced_by_generation | bigint NOT NULL, successor generation reference; never plaintext successor |
| replay_detected_at | nullable timestamptz, sanitized audit metadata |

Add users.credential_version bigint NOT NULL default 0 with reviewed lock/backfill
plan. No passwordHash copies in session tables. Index sessions(user_id),
sessions(user_id, created_at) WHERE revoked_at IS NULL for limits/revocation,
sessions(expires_at) and history(session_id); digest uniqueness supplies lookup
indexes. An unrevoked row may be expired: queries must also compare expires_at,
not assume the partial index means active. Avoid time-dependent NOW() in partial
index predicates. Enforce nonnegative generation/version and hash lengths.

Current schema management is `spring.jpa.hibernate.ddl-auto=update`; pom.xml has
no Flyway/Liquibase dependency. `db/migrations/V2...V13` are manually managed SQL
artifacts; the main README states no migration runner. Their names alone do not
prove execution. Live schema drift/constraints remain unknown because no database
was contacted.

Future authorized rollout: inventory schema on a disposable copy first; choose
an explicit migration mechanism with Daniel; review additive DDL, backups,
permissions, lock budgets and deployment ordering. Create empty tables/indexes
and version column before deploying code that maps them; test against production-
representative schema in disposable PostgreSQL. Do not rely on Hibernate update
to create security indexes/history constraints. Transition production to
ddl-auto=validate only after reconciling existing schema drift; this is not a
blind configuration flip. Existing backend should tolerate unused additive
tables/column. No session backfill from passwords or fabricated tokens. No DDL,
migration file, DB connection or schema validation was executed during this audit.

## 7. Client compatibility matrix and migration

No Android Kotlin or web TypeScript/JavaScript client source is present in this
repository inventory. No external client checkout was inspected. All current
client refresh support is **unverified**, including automatic refresh. The older
integration report is historical evidence only and cannot certify current apps.

| Client | Evidence / risks | Required readiness evidence |
| --- | --- | --- |
| Android (farmer/driver/other roles) | Backend supports shared DTO/aliases; historical Driver report lacked real auth. JWT decoding, nullable expiresIn handling, automatic refresh, storage and retry concurrency unknown. | Retrofit DTOs keep refreshToken as opaque String; seconds units; Keystore-backed secure persistence; atomic pair replacement; OkHttp single-flight; no recursion or refresh request interception; logout body and expiry support; process-death tests. |
| E-commerce | Shared login/refresh endpoint exists; no e-commerce frontend source inspected. Token storage, JWT parsing and refresh scheduler unknown. | Contract tests with opaque fixtures; one refresh across tabs/requests; updated pair persistence; retry once after refresh; avoid replaying unsafe checkout/payment requests without operation-specific idempotency; validate browser storage threat model. |
| Admin | Shared ADMIN JWT role checks; historical Angular report used mocks/localStorage. Current integration unknown. | Real bearer login, opaque persistence, interceptor, route guard, 401 vs 403 handling, cross-tab coordination, logout; all ADMIN server restrictions continue. |
| Existing JWT claims / IDs | Current numeric uid, string sub, ROLE_ enum and Long principal used by backend/test helpers. | Preserve claim types/role values and user.id; test added iss/aud/ver do not break clients; backend authority remains authoritative. |

Wire field names alone do not prove backward compatibility. Clients may decode
refresh JWTs for exp/role, expect no expiry, retain only the access token, discard
replacement refresh tokens, or send no logout body. Fifteen-minute lifetimes
expose any missing automatic refresh. Session caps and mandatory re-login are
also behavior changes. Cookie/HttpOnly browser refresh transport is a possible
later design; it needs explicit CORS/CSRF contracts and is not silently added here.

Recommended coordinated migration:

1. Daniel identifies owners/versions and supplies client source or verified
   contract tests. Fix opaque-token handling, single-flight and seconds units
   before the secure server cutover; these changes can accept old JWT strings too.
2. Stage the additive schema and secure server on disposable infrastructure;
   test all three clients. Approve a release window and communicate one-time login.
3. At cutover stop issuing and accepting legacy refresh JWTs, reject old access
   JWTs lacking new required claims/finite short exp. Replace the legacy signing
   key or enforce an unambiguous version/issuer boundary so legacy tokens cannot
   survive indefinitely. All replicas must share cutoff behavior; drain old nodes
   to prevent bypass. Neither alias retains an insecure fallback.
4. Require re-login to establish new families. Observe sanitized 401/503/rotation
   events and client failures. Do not roll back to non-expiring issuance.

If uninterrupted compatibility is mandatory, design a separately reviewed,
deadline-bounded legacy refresh exchange with hashed legacy-token tracking and
one-time consumption. It cannot retroactively prove device identity or undo
theft; no-exp legacy credentials need a hard deadline, and legacy access tokens
need a separate cutoff. Merely setting new lifetimes does not expire already-
issued tokens. A format/header toggle that permits indefinite legacy auth is
not an acceptable migration. The recommended design avoids this extra bridge.

## 8. Implementation steps and affected files (future work only)

Paths below are relative to `src/main/java/com/kilivana/backend` unless stated.

| Area | Files to modify or add after approval |
| --- | --- |
| JWT and enforcement | security/JwtService, JwtProperties, JwtAuthenticationFilter, RestAuthenticationErrorHandler; config/SecurityConfig; new account security-state helper/Clock bean; CurrentUser stays Long and should need no behavioral change. |
| Auth/session logic | common/service/AuthService; new RefreshSessionService, RefreshTokenHasher/generator; common/controller/AuthController; DTO RefreshTokenRequest validation and new LogoutRequest; AuthTokenResponse only approved additive metadata; ApiResponse/GlobalExceptionHandler and UnauthorizedException as needed for stable codes. |
| Persistence | admin/entity/User credential version; admin/repository/UserRepository locking/security projection; new refresh-session/history entities and repositories; no changes to existing profile ID types or domain entities. |
| Account mutations | admin/service/UserService, AdminSupplierService; farm/service/AdminFarmerService; admin/controller/AdminFarmerController; config/SeedDataRunner optional password enforcement. Inspect caller routes and hook every account-security write centrally. |
| Configuration/docs | src/main/resources/application.properties, .env.example, render.yaml, pom.xml only if an approved migration runner is selected; future reviewed migration under agreed path; README/SETUP stale auth sections; integration docs. |
| Tests | AuthControllerTest, AuthServiceEmailNormalizationTest, JwtTestSupport, ProfileControllerTest, DriverProfileControllerTest and new JwtService/refresh/session/logout/account-state tests. Maintain Phase 1A tests. Client repositories require separately coordinated changes. |

Proposed properties: retain app.jwt.secret/JWT_SECRET and
app.jwt.access-token-expiration/JWT_EXPIRATION with positive 900000 ms default;
deprecate app.jwt.refresh-token-expiration/JWT_REFRESH_EXPIRATION (JWT refresh no
longer issued), replace with app.auth.refresh-session-ttl=14d,
app.auth.max-active-sessions=5, app.auth.session-retention=7d,
app.jwt.issuer, app.jwt.audience, app.jwt.clock-skew=30s and explicit algorithm.
Validate at startup; define environment aliases before implementation. Existing
DB_URL/DB_USERNAME/DB_PASSWORD/DATABASE_URL remain connection mechanisms, not
credentials to copy into documentation. Existing security/CORS policies must be
reviewed only as necessary for selected transport; don't broaden permissions.

Sequence: approve policy/contracts; client readiness; reviewed additive schema;
unit-tested crypto/claims; transactional session repository; login/rotation/
logout; state-version hooks/filter; disposable PG races; MVC/role regression;
client staging; controlled cutover. No part of this sequence is authorized now.

## 9. Testing strategy

No tests were run for this documentation-only phase. Keep Phase 1A's historical
145 results distinct. Use deterministic Clock and fixed test-only signing keys;
never production tokens or DB configuration.

| Cases | Layer / database requirement |
| --- | --- |
| Expired access, missing exp, future iat, skew boundaries, issuer/audience, type confusion, alg/signature, invalid ID/role, sub mismatch | Plain JwtService unit tests, no DB. Test a token issued before cutoff/no exp is rejected. |
| Valid refresh rotates, expiry at boundary, unknown/revoked token, zero plaintext storage, all status/version checks | Service tests with mocked repositories/Clock; supplement with disposable PG persistence tests. |
| Consumed token replay revokes entire family, including current successor; revocation survives 401; rollback/signing failure leaves old token usable | **Disposable PostgreSQL required** for transaction/constraint proof; mocks cannot establish commit semantics. |
| Concurrent same-token refresh, refresh vs logout, password change/suspend vs login/refresh, multiple instances, cleanup vs refresh | **Disposable PostgreSQL required** with separate connections/transactions and barriers; verify lock ordering and only one rotation before replay revocation. |
| Session/user FK, digest uniqueness/length, generation constraints, version column, indexes, deletion/retention, schema upgrade/rollback and validate mode | **Disposable PostgreSQL required**, never existing OptionalFilterQueryTest against configured DB. |
| Logout idempotency, expired access plus refresh-body logout, unknown token non-enumeration, 503 not false success | MVC slices/mocked dependencies; disposable PG for actual family revocation and concurrency. |
| Suspended/INACTIVE/deleted accounts, password/version and role changes, reactivation cannot revive credentials, PENDING_VERIFICATION policy | Unit/MVC plus disposable PG atomic mutation tests. |
| Login/register envelopes and aliases, expiresIn seconds, user Long IDs, ADMIN restrictions, buyer/cart/order permission boundaries, 401/403 mapping, no token leakage/cache headers | Database-free MVC slices and existing relevant unit suites; mock new state reader. End-to-end auth + domain authorization uses disposable PG. |
| Android 401 automatic refresh, concurrent requests, process death during pair save, lost refresh response, offline/503, logout, opaque token, expiry scheduler, one retry only | Client unit/MockWebServer and emulator/device tests using test backend with **disposable PG** for full flow. No unsafe automatic payment retry. |
| E-commerce/Admin multi-tab refresh, correct role guards, logout and re-login, request deduplication, 403 never triggers refresh | Frontend contract/browser tests with mock API; full staging flows use **disposable PG**. |

Disposable DB acceptance requires unique generated database/container, isolated
test credentials, explicit test profile and guard rejecting non-test URLs; never
inherit DATABASE_URL or production secrets. Migrations are applied only there
when implementation is authorized. No unqualified full Maven test run: existing
OptionalFilterQueryTest deliberately targets configured PostgreSQL.

## 10. Rollback strategy

Before rollout record artifact/config versions, reviewed schema backup and client
compatibility. Use a secure previous artifact or disable login/refresh with 503
while fixing a release; keep Phase 1A reset blocked. Do not restore email-only
reset, non-expiring access tokens, reusable JWT refresh or no-op logout. Rolling
back to current a55b56e auth after cutover would reopen these risks and cannot
honor opaque sessions; it is not a safe default rollback.

Retain additive tables/version column during application rollback; destructive
down-migration can erase revocation/replay evidence. Do not re-enable old signing
keys/legacy tokens or reset revoked flags. If rollback loses session compatibility,
revoke families and require re-login after recovery. Preserve client opaque-string
handling and short-lived refresh support. DB restoration must not resurrect
previously revoked credentials; reconcile revocation or revoke all restored
families before serving auth. Any production rollback/DDL requires separate review.

## 11. Decisions for Daniel before implementation

1. Approve 15-minute access + 30-second skew, absolute 14-day refresh deadline,
   five device families and seven-day post-expiry retention, or specify alternatives.
2. Approve coordinated client upgrade and forced re-login/key/claim cutoff;
   identify Android/e-commerce/Admin repositories, owners and release versions.
3. Approve strict rotation/no grace, including re-login after duplicate refresh
   or lost response, or request a separately analyzed idempotency alternative.
4. Confirm refresh-only logout with access valid for up to 15 minutes plus skew,
   or require immediate session-bound access revocation (extra DB session checks).
5. Confirm immediate account/password/role enforcement via DB state reads and
   global session revocation; decide PENDING_VERIFICATION and any Admin restrictions.
6. Choose explicit migration ownership/runner and production schema-validation
   transition. Approve FK deletion/retention policy without altering Long IDs.
7. Confirm browser body-token storage contract vs separately scoped HttpOnly
   cookie/CSRF work, rate limits and error-code additions.
8. Confirm issuer/audience, fixed HMAC algorithm/key encoding, secret provisioning,
   operational key rollover and compromise response owners.

Security references: the rotation/replay pattern is informed by
[RFC 9700 section 4.14](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14);
JWT algorithm/issuer/audience/type validation is informed by
[RFC 8725 section 3](https://www.rfc-editor.org/rfc/rfc8725.html#section-3).
These are design references, not a claim that this custom login API is an OAuth
authorization server. Detailed Kilivana policies above are proposals for review.
