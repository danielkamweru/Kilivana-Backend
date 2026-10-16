# Local shared-backend farmer demonstration — verified 2026-10-14

> Review copy: historical evidence from the tested feature branch based on `a55b56e`, not an audit of current upstream `main`. Workstation paths/device identifiers are omitted. Original local documents are preserved outside this commit scope. See [review guide](README.md).

## Database and runtime

The user approved only 03-local-schema-proposal.sql and
04-local-categories-proposal.sql for the previously empty local development
127.0.0.1:5432/kilivana database. Both files were checked against their reviewed
SHA-256 fingerprints before execution and remain unchanged:

- Schema: 2DAB1C56B76B36B2C4AA2D94A83DC1B778259532406625A1BFCBBA2EDAE3042A
- Categories: 0795CAD32E7ADB1F214AEB0645254606D819B89637B6348D4ABD6E4FF72A77D5

Authenticated only as kilivana_app; CONNECT, public USAGE and CREATE passed.
The account is not a superuser. Both transactions committed. Verified 35 tables,
35 primary keys, 18 mapped unique constraints, 28 checks, 61 total indexes,
uk_users_email_lower, ux_proof_of_deliveries_job and the driver suspension check.
All 35 tables are owned by kilivana_app. Category counts are 11 FARMER, 8 SUPPLIER.
No historical migrations, other databases, administrator changes or seeders ran.

The prepared launcher started canonical Java process <local-process> on 127.0.0.1:8081 with
Hibernate validate, SQL initialization disabled, both seeders disabled, encrypted
credentials, database image storage and the proposed finite local token lifetimes.
Reference process <local-process> remained untouched on 8080. Actual GET /, /actuator/health,
/api/v1/health and both category-type endpoints returned 200. Health is UP.

## Android changes

FarmApp uses CanonicalBackendApi, CanonicalAuthService and CanonicalFarmService
through ApiClient, with the earlier canonical DTO/envelope/precision layer.
The default development URL is http://127.0.0.1:8081/api/v1/ and the connected device
uses adb reverse tcp:8081 tcp:8081. No public listener was introduced. MockAuthService
and buyer/supplier/inspector demo screen source were preserved.

- Email login; register -> login -> existing farmer profile creation.
- Numeric identity and origin-marked encrypted sessions; old reference-origin
  sessions are cleared rather than reused against a different backend.
- Refresh expiry remains unknown (blank legacy display field plus canonical marker).
  The server decides refresh validity. Local sign-out does not claim token revocation.
- Canonical seller types serialize as lowercase farmer/supplier. Live testing caught
  the earlier uppercase-only fixture assumption; mapper and fixtures now support the
  actual wire values explicitly and reject unknown types.
- Own products and active FARMER categories, exact prices and integer quantities.
  Editing sellable quantity reads current reservations and writes total stock as
  sellable + reserved. This is not a server-side concurrency/locking upgrade.
- Pending moderation is displayed, never auto-approved. Product removal is disabled.
  Seller orders and supply purchasing show unavailable; no general order feed is used.
- Multipart image field image; explicit order/primary selection. Missing canonical
  filename/MIME/size metadata stays null. Public downloads use a bearer-free client;
  same-origin API/uploads and HTTPS res.cloudinary.com are allowed. Redirects and
  automatic connection retries are disabled. Lost upload responses keep an uncertain
  draft checkpoint and block automatic retry until manual reconciliation.

## Verification

- Backend: 41 selected database-free auth, health, profile and datasource tests passed.
- Android: 138 unit tests passed, zero failures/errors/skips.
- Debug APK, debug device-test compilation and release Kotlin compilation passed.
- Samsung SM-A155F, Android 16: actual instrumented Android requests passed farmer
  registration, login, profile read/phone update, categories, pending product create,
  two PNG uploads, byte-for-byte image retrieval, own catalogue read and product edit.
- A separate Android process restored the persisted Keystore-encrypted session,
  requested the profile, refreshed against the real backend, and persisted the pair.
- Gradle removed its connected-test installation afterward; direct ADB installation
  was used for the two-process persistence check. The isolated demo application ID is
  com.example.farmapp.canonicaldemo, preserving the original installed app's data.

Final development fixture counts: 3 farmer accounts/profiles (including the initial
wire-mismatch test), 2 pending products, 4 product images/stored images, 0 orders.
These are deliberate API-created local test records, not startup seeding. No records
were deleted. Public product browsing returned zero ACTIVE products, confirming that
pending records were not promoted for the demonstration.

## Limits and operation

This is a development demonstration, not a production authentication rollout.
Existing reusable refresh/no-op server logout and access-account enforcement risks
remain. Secure seller orders, safe unlisting and decimal quantity support are still
separate work. Partial registration can leave an account needing profile completion;
the app reports that explicitly and does not blindly register again. Reservation
edits need server atomicity before concurrent marketplace use.

Database-provider image upload/retrieval was tested live. Local/Cloudinary URL policies
were unit-tested, but those providers were not exercised live. Real web/admin clients
were not tested; their canonical wire contracts and backend Java sources are unchanged.
USB reverse must be restored after reconnection. Start the canonical backend using
scripts/Start-LocalCanonical.ps1 and the existing Windows-encrypted credentials file;
never put its contents in Git or chat. No commit, push or branch reset occurred.
