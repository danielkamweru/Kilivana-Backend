# Phase 1A — Password reset security

**Daniel's approval: pending.** Implementation is prepared on
`feature/android-backend-integration`, from baseline `16e3292`.

## Vulnerability and fix

The public reset endpoint previously accepted `email` and `newPassword`, looked
up the account, encoded the supplied password and saved it without proof of
account ownership. Knowing an email address was sufficient to take over an account.

`AuthService.resetPassword` now immediately throws `ServiceUnavailableException`.
It performs no account lookup, password encoding, persistence or token operation.
The transaction annotation was removed from this disabled method, avoiding even
transaction acquisition for the rejection. Direct service callers are also denied.
There is no enablement flag or alternate email-only reset path introduced here.

Both POST aliases retain their request DTO and validation:

- `/api/v1/auth/reset-password`
- `/api/auth/reset-password`

Valid JSON with a valid email and password of at least six characters returns
HTTP `503`, `success: false`, `error.code: SERVICE_UNAVAILABLE` and
`message: Password reset is currently unavailable.` through the existing error
handler. The outcome does not depend on account existence. Invalid DTO fields
still return `400 VALIDATION_ERROR`; missing or malformed JSON follows the existing
400 handling. Authentication is not a means to enable this disabled operation.

## Changed files

- `src/main/java/com/kilivana/backend/common/service/AuthService.java`: remove the unsafe write and fail closed.
- `src/main/java/com/kilivana/backend/common/controller/AuthController.java`: mark reset disabled in OpenAPI.
- `src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java`: rejection for multiple email inputs and null requests; zero interactions with repository, encoder, JWT service or reference generator.
- `src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java`: anonymous rejection on both aliases, uniform known/unknown email responses and retained validation; existing registration and login tests retained.
- `docs/integration/README.md` and this document: integration handoff.

## Test safety and validation

Inspected `pom.xml`, `src/main/resources/application.properties`, selected test
classes and test resource configuration before running tests. There are no test
datasource resource overrides. Application configuration defaults to local
PostgreSQL and can accept environment database settings. `OptionalFilterQueryTest`
uses `@DataJpaTest` with `replace = NONE` and explicitly targets configured
PostgreSQL: do not run it for this phase.

Only the following tests are selected: `PasswordResetSecurityTest` (plain Mockito),
`AuthServiceEmailNormalizationTest` (Mockito with BCrypt), and `AuthControllerTest`
(`@WebMvcTest` with mocked service and security configuration). The MVC slice does
not enable datasource/JPA auto-configuration. No full application context or backend
server is started. No database test, migration or schema operation is selected.

The initial attempt used installed Maven 3.9.9 and JDK 21, offline, with an explicit selector:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
& 'C:\Users\AppleFi\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\AppleFi\.m2\repository' -o -B -ntp '-Dtest=PasswordResetSecurityTest,AuthControllerTest,AuthServiceEmailNormalizationTest' test
```

The initial command failed before compilation: offline mode (`-o`) prohibited
fetching the uncached Spring Boot `spring-boot-starter-parent:3.2.0` POM. The
online retry removed `-o`, but the sandbox could not create files under the
owner's Maven cache (`AccessDeniedException`). An approved retry outside the
sandbox resolved the parent and began downloading dependencies. No global Maven
settings were changed and no tools were installed.

Environment checks confirmed Maven 3.9.9 with Oracle Java 21.0.1. No user
`C:\Users\AppleFi\.m2\settings.xml` was found; Maven's installation settings
have no active offline mode, proxy or profile and only the standard external
HTTP blocker (which does not block HTTPS Maven Central). The project has no
repository override or `.mvn/maven.config`. Dependencies use Maven Central.

Online validation command (approved cache/network access):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
& 'C:\Users\AppleFi\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\AppleFi\.m2\repository' -B -ntp '-Dtest=PasswordResetSecurityTest,AuthControllerTest,AuthServiceEmailNormalizationTest' test
```

Execution result on 2026-10-08: **BUILD SUCCESS**, 15 tests passed, 0 failures,
0 errors, 0 skipped. `AuthControllerTest`: 7; `AuthServiceEmailNormalizationTest`:
3; `PasswordResetSecurityTest`: 5. All application and test sources compiled
with Java release 21. Selected tests verify reset denial and existing
registration/login behavior; they do not certify every application feature.
The MVC tests mock the service; the separate service tests exercise the real
reset method and verify zero dependency interactions. Together they cover HTTP
rejection and absence of password writes without using a database.

Additional database-free regression command (ran successfully inside the
sandbox using the now-populated cache):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
& 'C:\Users\AppleFi\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\AppleFi\.m2\repository' -o -B -ntp '-Dtest=UserServiceUsernameRegionTest,DriverCapacityFormatterTest,DriverEnumParsingTest,EnumWireFormatTest,PaymentMethodTest,KenyaLocaleTest,HealthControllerTest,CloudinaryServiceTest,DatabaseUrlEnvironmentPostProcessorTest' test
```

Result: **BUILD SUCCESS**, 130 passed, 0 failures, 0 errors, 0 skipped:

| Test class | Passed |
| --- | ---: |
| UserServiceUsernameRegionTest | 4 |
| DriverCapacityFormatterTest | 5 |
| DriverEnumParsingTest | 5 |
| EnumWireFormatTest | 45 |
| PaymentMethodTest | 21 |
| KenyaLocaleTest | 31 |
| HealthControllerTest | 1 |
| CloudinaryServiceTest | 13 |
| DatabaseUrlEnvironmentPostProcessorTest | 5 |

Across both runs: **145 passed, 0 failures, 0 errors, 0 skipped**. Surefire
evidence is under `target/surefire-reports/` (ignored build output). The
Cloudinary failure-path tests deliberately log mocked IOException errors and
use mocked storage; they do not connect to Cloudinary or a database. The database
URL test only transforms strings in a mock environment. MVC slices initialize
a mock servlet test context, not a listening backend server.

Review found no compilation errors or unintended route/DTO changes. The existing
specific exception handler returns the intended 503 envelope without exposing
account details. Existing compiler warnings about Lombok builder defaults and
unchecked Cloudinary operations are outside this patch; Java 21 also reports
Mockito dynamic-agent warnings. No unrelated application code was changed.

Remaining limits: no PostgreSQL integration tests, deployment tests, real Android
UI tests or end-to-end verified recovery tests were run. Registration/login
compatibility is supported by selected mocked HTTP tests and real-service unit
tests, not production-database testing. A secondary shell environment inventory
failed with a PowerShell duplicate-key error; Maven's own version output and
successful builds directly confirmed the Java/Maven runtime and dependency access.

Final handoff: validated reset denial on both public aliases, no password writes,
and selected compatibility regressions. Only these two documentation files were
updated during this validation continuation. The prepared source/test changes
were uncommitted at validation completion; the final checkpoint authorizes one
local commit of the six Phase 1A files on the same feature branch. The baseline
remains `16e3292`. Daniel must review
the diff and coordinate Android's unavailable-recovery behavior. **Approval is
pending; Phase 1A is not approved and no handoff message was sent.**

## Compatibility and Daniel's review

Reset clients that expected success will now receive 503. Android must show
recovery as unavailable, avoid success messaging and avoid automatic retries or
fallback to the legacy alias. Login, registration, refresh, route mappings,
DTO validation, JWT architecture and schema are unchanged by this patch.

The existing forgot-password implementation remains unchanged: it looks up an
email, can reveal nonexistence via 404, and returns an instructions-sent message
without implementing delivery. It is not a working secure recovery flow. Daniel
should review this separately before presenting end-to-end recovery as available.
This patch does not revoke previously issued tokens or repair accounts that may
already have been compromised.

Daniel's review checklist:

- Review removal of all reset lookup/write operations and both endpoint aliases.
- Review selected test evidence and Android handling of the 503 response.
- Confirm deployment coordination and communicate recovery unavailability.
- Keep approval pending until Daniel explicitly approves; no deployment occurs here.
- Scope future recovery around proof of ownership, expiring single-use recovery
  credentials, abuse controls and non-enumerating responses before enabling resets.

## Rollback

No database or schema rollback is needed. Preserve the diff and any later work;
do not use a blanket reset or checkout. If this change causes a release issue,
retain the fail-closed guard and disable the recovery UI while investigating.
Restoring the baseline reset method reopens account takeover and must not be a
routine rollback. If a code rollback is separately approved, reverse only this
phase's changes in a reviewed patch while keeping both reset aliases blocked
until verified recovery exists. Do not change JWT secrets or database contents
as part of this phase's rollback.
