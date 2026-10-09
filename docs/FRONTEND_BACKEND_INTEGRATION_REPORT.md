# Frontend ↔ Backend Integration Report

Comparison of the two frontend applications against this backend. No frontend code was
changed; this document lists what each app must implement and what the backend still owes
them.

Backend: Spring Boot 3.2.0, Java 21, PostgreSQL. Base path `/api/v1`. JWT (HS512, Bearer).
Live OpenAPI document: `/api-docs` (259 operations). Swagger UI: `/swagger-ui/index.html`.

| App | Stack | Path | Backend integration today |
|---|---|---|---|
| Admin | Angular 20 | `Fay-chebby/KilivanaAdmin2` | **None.** No `HttpClient` at all; every screen reads mock arrays. |
| Driver | Android (Kotlin/Retrofit) | `brianmuigai2-stack/kilivana-driver` | **One** live call, which returns HTTP 401. |

---

## 1. The blocking issue: neither app authenticates

This is the single reason nothing works end to end. Everything else in this report is
downstream of it.

### 1.1 What the backend requires

Every endpoint except health, auth and the OpenAPI document needs
`Authorization: Bearer <accessToken>`. The token comes from:

```
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "driver.test@kilivana.local", "password": "Kilivana#2026" }
```

Response (`data`):

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": null,
  "user": { "id": 6, "name": "...", "email": "...", "phone": "...",
            "role": "DRIVER", "status": "ACTIVE",
            "verificationStatus": "VERIFIED", "createdAt": "..." }
}
```

> **`expiresIn` is a millisecond count, default 30 minutes.** Tokens are configured to expire
> after 1,800,000 ms by default; override with `JWT_EXPIRATION`. A value of `0` issues tokens
> with no expiry, which is convenient for local testing but means a client must declare this
> field nullable.

### 1.2 What each app does instead

**Driver app** sends a plain `X-User-Id` header and no token:

```
GET api/v1/profiles/drivers/{userId}/images
  headers: X-User-Id: 42, ngrok-skip-browser-warning: true
```

This returns **HTTP 401**. The backend deliberately does not trust `X-User-Id`: an earlier
version did, and any logged-in user could then read and rewrite any other user's profile
by changing the id (fixed in backend commit `02216c4`). The app must send a JWT instead.
Its `userId` is currently the hardcoded constant `42L` in `ProfileViewModel.kt`, carrying a
`TODO: source this from the login/session response`.

**Admin app** never calls the network at all. `login()` compares against two hardcoded
accounts in `features/auth/services/auth.service.ts` and writes the user object to
`localStorage` under `kilivana_admin_session`:

| Email | Password |
|---|---|
| `admin@kilivana.com` | `Admin@123` |
| `superadmin@kilivana.com` | `SuperAdmin@123` |

### 1.3 What the driver app must add

1. A login call against `POST /api/v1/auth/login`.
2. Persistent token storage (the file has a `TODO` for DataStore; use EncryptedSharedPreferences
   or the Android Keystore — a plain-text refresh token in SharedPreferences is not acceptable).
3. An OkHttp interceptor that attaches `Authorization: Bearer <accessToken>` to every
   request. Reuse the existing `ngrokHeaderInterceptor` pattern in `ApiClient.kt`.
4. Refresh on 401 using `POST /api/v1/auth/refresh` with body `{"refreshToken": "..."}`.
   Note this endpoint changed recently — it previously required a full token object.
5. `GET /api/v1/auth/me` to recover identity on cold start, instead of the constant `42L`.
6. Clear the session on logout.

### 1.4 What the admin app must add

1. `provideHttpClient()` in `app.config.ts` — it is currently absent, which is why no service
   can make a request.
2. A real `login()` in `auth.service.ts` calling `POST /api/v1/auth/login`, and a real
   `requestPasswordReset()` (the backend has `POST /api/v1/auth/forgot-password?email=...`).
3. Implement `authInterceptor` — it is currently `return next(req)`, a no-op that attaches
   nothing.
4. Replace the localStorage session with the token pair, and keep the `AdminUser` shape
   derived from `data.user`.
5. Implement `roleGuard` — it currently returns `true` unconditionally, so nothing is
   actually protected on the client either.

---

## 2. Driver app: what matches and what does not

### 2.1 Matches

These are already correct — do not change them:

- Base URL `https://either-juvenile-progeny.ngrok-free.dev/` with trailing slash; Retrofit
  paths are written without a leading slash, which is correct for that base.
- `ApiResponse<T>` = `{ success, message, data, timestamp, error }` — matches the backend
  envelope exactly.
- `ApiError` = `{ code, details }` — matches.
- `DriverImage` = `{ id, url, publicId, assetId, sortOrder, isPrimary, createdAt, updatedAt }`
  — matches `ImageResponse` field for field, camelCase.
- Path `api/v1/profiles/drivers/{userId}/images` — exists and returns 200 with a token.
- The `ngrok-skip-browser-warning` header — correct and still needed for the tunnel interstitial.

### 2.2 Mismatches

| # | App expects | Backend actually provides | Impact |
|---|---|---|---|
| D1 | `X-User-Id` header identifies the caller | `Authorization: Bearer` JWT | **401 on every call** |
| D2 | Job has `pickupLat`, `pickupLon`, `dropoffLat`, `dropoffLon` | **Fixed.** `pickupLatitude`/`pickupLongitude`/`destinationLatitude`/`destinationLongitude`, all nullable — see §4.1 | **Renamed, not identical.** Add a mapping layer; do not assume the app's names. |
| D3 | Job has `deliveryOtp` (comment says backend generates it and SMSes the buyer) | **Fixed.** `deliveryOtp` + `deliveryOtpExpiresAt`, emailed to the buyer via SendGrid. Proof of delivery now requires it — see §4.2 | It arrives by **email**, not SMS. |
| D4 | Job has `cargo`, `quantity`, `payoutKsh`, `customer`, `distanceKm`, `estimatedTime`, `pickupTime`, `dropoffTime` | **Mostly fixed** — see §4.3. `cargoDescription`, `quantity`, `payoutAmount`, `distanceKm`, `estimatedMinutes`, `scheduledPickupAt`, `scheduledDropoffAt` all exist | **`customer` does not exist.** Fetch the order separately or ask for it to be embedded. |
| D5 | `JobStatus` of the app's own design | Backend `DeliveryStatus`: `PENDING_ASSIGNMENT, ASSIGNED, ACCEPTED, EN_ROUTE_TO_PICKUP, ARRIVED_AT_PICKUP, PICKED_UP, IN_TRANSIT, ARRIVED_AT_DESTINATION, DELIVERED, CANCELLED, FAILED` | Needs a mapping table |
| D6 | Login screen validates `password.length >= 4` locally | Backend validates credentials | Cosmetic, but the local check is meaningless once real auth lands |
| D7 | `AppNotification` has `timeLabel`, `isEarlier`, `type` | `NotificationResponse` has `id, userId, type, title, message, readAt, createdAt` | Derive `timeLabel` from `createdAt`; `isEarlier` is a local grouping concern |

### 2.3 Endpoints the driver app should consume

All require a JWT. The `{userId}` in profile paths **must be the logged-in driver's own id**;
another user's id returns 403.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Public. Returns tokens + user. |
| `POST` | `/api/v1/auth/refresh` | Public. Body `{"refreshToken":"..."}`. |
| `POST` | `/api/v1/auth/logout` | Requires JWT. |
| `GET` | `/api/v1/auth/me` | Requires JWT. Use to recover identity on cold start. |
| `GET` | `/api/v1/profiles/drivers/{userId}` | Own profile, includes `images`. |
| `POST` | `/api/v1/profiles/drivers/{userId}` | Create. Body requires `licenseNumber`, `vehicleType`, `vehicleNumber`, `availabilityStatus`. |
| `PUT` | `/api/v1/profiles/drivers/{userId}` | Update. |
| `DELETE` | `/api/v1/profiles/drivers/{userId}` | Deletes profile and images. |
| `POST` | `/api/v1/profiles/drivers/{userId}/images` | `multipart/form-data`, part name **`image`**, optional `?isPrimary=true`. |
| `GET` | `/api/v1/profiles/drivers/{userId}/images` | Already implemented. |
| `DELETE` | `/api/v1/profiles/drivers/{userId}/images/{imageId}` | |
| `GET` | `/api/v1/logistics/jobs/driver/{driverId}` | The driver's jobs — this is what the jobs list should call. |
| `GET` | `/api/v1/logistics/jobs/{id}` | |
| `POST` | `/api/v1/logistics/jobs/{id}/accept` | **No body.** |
| `PUT` | `/api/v1/logistics/jobs/{id}/status?status=ACCEPTED` | **Status is a query parameter, not a body.** |
| `POST` | `/api/v1/logistics/jobs/{jobId}/tracking` | Tracking events for a job. |
| `POST` | `/api/v1/logistics/jobs/{jobId}/proof-of-delivery` | Body = `ProofOfDelivery`: `logisticsJobId`, `recipientName`, `signatureUrl`, `photoUrl`, plus **`?otp=`** — required when the job holds a code. See §4.2. |
| `POST` | `/api/v1/logistics/jobs/{id}/otp/verify?otp=123456` | Verifies and consumes the handover code. Optional if you pass `?otp=` straight to proof-of-delivery. |
| `GET` | `/api/v1/logistics/proof-of-delivery/job/{jobId}` | |
| `GET` | `/api/v1/logistics/tracking-events/job/{jobId}` | |
| `GET` | `/api/v1/notifications/user/{userId}` | |

> Note the status endpoint takes `?status=` in the query string. The driver app will send it
> in a JSON body and get a 400.

> Proof of delivery is **gated on the OTP**. Passing a wrong, missing or expired code is a
> `400`. If the app posts proof without an `otp` param against a job that has one, it will be
> rejected — this is the D3 fix, so it is expected to change that flow. A second proof for the
> same job is a `409`.

---

## 3. Admin app: what matches and what does not

### 3.1 Matches

- `LoginRequest { email, password }` — matches `AuthLoginRequest`.
- `ApiResponse<T>` is conceptually right, but see A2 below.
- Error handling is a non-starter: there is no `error-interceptor` implementation and the
  `ApiError` interface has the wrong shape (A3).

### 3.2 Mismatches

| # | Admin app | Backend | Impact |
|---|---|---|---|
| A1 | No `provideHttpClient()`, no `HttpClient` injected anywhere; `ApiService` is an empty class | REST API | **Nothing can be requested.** This is the first thing to fix. |
| A2 | `ApiResponse<T> = { success, message?, data }` | `{ success, message, data, timestamp, error }` | Add `timestamp` and `error`; `message` is always present on the backend |
| A3 | `ApiError { status, message, errors? }` | `error: { code, details }` inside the body, plus the HTTP status | Restructure to read `error.code` / `error.details` |
| A4 | `id: string` on every model (`'1'`, `'ADM-001'`) | `Long` | Will not bind in TypeScript; use `number` |
| A5 | `code: string` (`'B-001'`, `'F-001'`, `'S-001'`, `'DA-001'`) | **Fixed.** `User.referenceCode`, e.g. `F-014`, assigned at registration — see §4.6 | Read `referenceCode`, not `code`. |
| A6 | `role: 'admin' \| 'super-admin'` | **Fixed.** `SUPER_ADMIN` added; both staff roles get identical access — see §4.4 | Requires the one-time SQL migration. |
| A7 | `fullName`, `initials` | `name`; no initials | Map `name`; derive initials client-side |
| A8 | `status: 'verified' \| 'pending' \| 'suspended'` | `UserStatus` = `ACTIVE, INACTIVE, SUSPENDED` and separately `VerificationStatus` = `PENDING, VERIFIED, REJECTED` | The app collapses two backend concepts into one field — needs splitting |
| A9 | `KycStatus` on driver, farmer, supplier models | No KYC/verification entity beyond `User.verificationStatus` | Needs a decision (see §4) |
| A10 | `region`, `crops[]`, `farms`, `rating`, `revenue`, `ordersCount`, `totalSpend`, `disputesCount` | **Partly fixed.** `region` now exists; `rating`, `revenue`, `ordersCount`, `crops`, `farms` deliberately still absent — see §4.7 | Treat the aggregates as display-only until a rating data model exists. |
| A11 | `DashboardStats` with `totalFarmers`, `monthlyRevenue`, `avgOrderValue`, `OrderTrendPoint`, `CropSlice`, `ActivityItem` | **Fixed.** `GET /api/v1/admin/dashboard/stats` — see §4.5 | Field is `averageOrderValue`, not `avgOrderValue`. There is no `ActivityItem`; `topCategories` replaces `CropSlice`. |
| A12 | 12 of 19 models are **empty interfaces**: orders, products, disputes, logistics, farms-crops, inspectors, kyc, notifications, payments, reports, settings, users | Full APIs exist for all of these | The contracts have to be written before the screens can be wired |
| A13 | `vehicle: { type, capacity, plateNumber, make }` | **Fixed.** `vehicleType`, `vehicleCapacityKg`, `vehicleNumber` (plate), `vehicleMake` — see §4.7 | Note `vehicleCapacityKg` is an `Integer`, not a string. |
| A14 | `licenceNumber`, `licenceExpiry`, `idType`, `idNumber` | **Fixed.** `licenseNumber`, `licenseExpiryDate`, `idType`, `idNumber` — see §4.7 | Spelling is `license`, not `licence`. Expiry is a `LocalDate`. |
| A15 | `username` on driver/supplier models | **Fixed.** `User.username` — see §4.7 | Optional field; also on `User.region`. |
| A16 | `contractEnd` on supplier | **Fixed.** `SupplierProfile.contractEndDate` — see §4.7 | |
| A17 | Password reset expects a `forgot-password` flow that fails for unknown emails | `POST /api/v1/auth/forgot-password?email=...` | Use it, and always resolve (never reveal whether an email exists) |

### 3.3 Endpoints the admin app should consume

Admin routes require the `ADMIN` **or** `SUPER_ADMIN` role; any other token gets **403**.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/login`, `/refresh`, `/logout`, `/forgot-password?email=`, `/reset-password`; `GET /api/v1/auth/me` |
| Users & roles | `GET/POST /api/v1/admin/users`, `GET /api/v1/admin/users/role/{role}`, `/status/{status}`, `GET /api/v1/admin/users/{id}`, `PUT/PATCH /api/v1/admin/users/{id}/status`, `GET /api/v1/admin/audit-logs` |
| Addresses | `GET/POST /api/v1/addresses`, `GET/PUT/DELETE /api/v1/addresses/{id}` |
| Farmers | `GET/POST /api/v1/profiles/farmers/{userId}`, `PUT`, `DELETE`; images at `.../images` |
| Buyers | `GET/POST /api/v1/profiles/buyers/{userId}`, `PUT`, `DELETE` |
| Suppliers | `GET/POST /api/v1/profiles/suppliers/{userId}`, `PUT`, `DELETE`; images at `.../images` |
| Inspectors | `GET/POST /api/v1/profiles/inspectors/{userId}`, `PUT`, `DELETE`; images at `.../images` |
| Drivers | `GET /api/v1/admin/drivers`; `PUT /api/v1/admin/drivers/{userId}/suspend?reason=` and `.../unsuspend` (§4.10); profiles and images at `/api/v1/profiles/drivers/{userId}` |
| Products | `GET/POST /api/v1/admin/products`, `/admin/products/{id}/status`, images, categories |
| Orders | `GET/POST /api/v1/admin/orders`, `/admin/orders/{id}/status`, `POST /admin/orders/{id}/assign?driverId=` (dispatch to a driver, §4.10), `POST /api/v1/orders/{id}/cancel?reason=` (§4.10), disputes |
| Payments | `GET /api/v1/admin/payments`, `/admin/payments/{id}/status`, refunds |
| Inspections | `GET/POST /api/v1/admin/inspections`, `/admin/inspections/{id}/result`, `/status` |
| Notifications | `GET /api/v1/admin/notifications` (optional `?unread=true`), `/admin/notifications/user/{userId}`, `/user/{userId}/unread`, `/read-all` |
| Logistics | `GET /api/v1/logistics/jobs`, `/jobs/status/{status}`, `/jobs/{id}`, `PUT /jobs/{id}/assign`, `/cancel` |
| Dashboard | `GET /api/v1/admin/dashboard/stats` (see §4.5), `GET /api/v1/admin/dashboard` |
| Email | `POST /api/v1/admin/mail/test` (see §4.8) |

Seeded staff accounts, password `Kilivana#2026`:
`admin.test@kilivana.local` (`ADMIN`) and `superadmin.test@kilivana.local` (`SUPER_ADMIN`).

As an admin, **every** profile is accessible, not just your own. A non-admin calling an
admin route gets 403.

---

## 4. Backend changes — now implemented

All seven items below were resolved on the backend. This section is the contract the apps
should code against. Everything here is live and verified against a running backend.

### 4.1 Job coordinates (was D2)

`LogisticsJob` gained four nullable columns. The map and OSRM routing can now plot a route
instead of guessing from address text.

| Field | Type | Notes |
| --- | --- | --- |
| `pickupLatitude` / `pickupLongitude` | `Double` | |
| `destinationLatitude` / `destinationLongitude` | `Double` | |

All four are optional. A job created without them still works, it just has no pin. Send them
on `POST /api/v1/logistics/jobs`; they are echoed on every job read.

### 4.2 Delivery OTP (was D3)

A 6-digit numeric code is generated on job creation with `SecureRandom`, valid for 24 hours.
The buyer is emailed it through SendGrid at the moment the job is created.

New endpoint:

```
POST /api/v1/logistics/jobs/{id}/otp/verify?otp=123456
```

The code is **single-use**: a successful verify clears it, so a captured code cannot be
replayed. Proof of delivery now *requires* it — `POST /api/v1/logistics/proof-of-delivery`
and `POST /api/v1/logistics/jobs/{jobId}/proof-of-delivery` take an optional `?otp=` param,
and if the job holds a code it must match or the request is rejected.

Job reads expose `deliveryOtpExpiresAt` and `deliveryOtpVerified` but **never the code
itself**. The driver app cannot display the code, and must not try to: the code is what the
*customer* reads out to the driver at handover. If your screen currently expects a
`deliveryOtp` field to render, delete that expectation.

The code is emailed to the buyer and held nowhere else — it is stored as a BCrypt hash, so a
database read cannot reveal a code that is still live.

Rejections:

| Status | Meaning |
|---|---|
| `400 Incorrect delivery code` | Wrong code; attempts incremented |
| `400 The delivery code has expired` | Past the 24-hour window |
| `400 This job has no delivery code to verify` | Legacy job created before OTP existed |
| `429 Too many incorrect delivery codes…` | Locked after 5 wrong guesses; 15-minute cooldown |

The lockout **locks** rather than regenerates the code, so a genuine driver cannot be denied
by someone else burning the attempts. After the cooldown the same code still works.

**One proof per job.** A second proof of delivery for a job is `409 This delivery has already
been confirmed`, enforced by a `deliveryOtpVerified` flag plus a unique index on
`logistics_job_id`. The app can therefore treat a filed proof as final.

`signatureUrl`, `photoUrl` and `otpReference` are all **optional** — they were `NOT NULL`
before, which rejected every proof that omitted a signature and made the OTP flow impossible
to complete. Send whatever you have; `recipientName` is the only field the driver must supply.

### 4.3 Job payload (was D4)

Added to `LogisticsJob`, all nullable:

| Field | Type | Maps to app's |
| --- | --- | --- |
| `cargoDescription` | `String` | `cargo` |
| `quantity` | `Integer` | `quantity` |
| `payoutAmount` | `BigDecimal` | `payoutKsh` |
| `scheduledPickupAt` | `LocalDateTime` | `pickupTime` |
| `scheduledDropoffAt` | `LocalDateTime` | `dropoffTime` |
| `distanceKm` | `Double` | `distanceKm` |
| `estimatedMinutes` | `Integer` | `estimatedTime` |

Note the naming differences — the backend uses `scheduledPickupAt`/`scheduledDropoffAt` and
`estimatedMinutes`, and money is `BigDecimal`, not a number.

`customer` is **still not** on the job. It needs resolving through `orderId → Order → buyerId →
User`, which is a second join per job in a list endpoint. That is a deliberate call for the
frontend: either call `GET /api/v1/orders/{orderId}` for the customer, or ask for it to be
embedded. Do not assume a `customer` field exists.

### 4.4 SUPER_ADMIN (was A6)

`SUPER_ADMIN` is now a real role and is granted full access to `/api/v1/admin/**`, the same as
`ADMIN`. Role checks use a single `isStaff()` helper, so both roles behave identically in
product-edit permission and cross-profile access.

- Seeded account: `superadmin.test@kilivana.local`
- Registration still refuses to self-register either staff role.
- Reference-code prefix: `SA-` (`ADMIN` is `AD-`).

> **Migration required.** `db/migrations/V2__add_super_admin_role.sql` must be run once
> against every database that predates this change, or startup fails on the
> `users_role_check` constraint. Fresh databases are fine.

### 4.5 Dashboard statistics (was A11)

```
GET /api/v1/admin/dashboard/stats      # ADMIN or SUPER_ADMIN
```

```jsonc
{
  "totalFarmers": 128,
  "totalBuyers": 76,
  "activeOrders": 24,
  "monthlyRevenue": 1450000.00,
  "monthlyOrders": 41,
  "pendingVerifications": 5,
  "openDisputes": 2,
  "averageOrderValue": 35200.00,
  "updatedAt": "2026-10-01T09:40:52.445",
  "orderTrend": [ { "date": "...", "orders": 6, "revenue": 45000.00 } ],  // 14 days
  "topCategories": [ { "category": "Vegetables", "orders": 18 } ]        // up to 6
}
```

Definitions chosen here, so the app does not have to guess:

- `activeOrders` — `PENDING`, `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`, `PICKED_UP`,
  `IN_TRANSIT`. Excludes cancelled and failed.
- `monthlyRevenue` / `monthlyOrders` / `averageOrderValue` — **settled orders only**
  (`DELIVERED`, `COMPLETED`), so cancelled baskets never count as turnover.
  `monthly*` are calendar-month to date; `averageOrderValue` is all-time settled.
- `orderTrend` — dense: every one of the last 14 days appears exactly once, zero-filled, so a
  chart needs no gap-filling.
- `topCategories` — category name to order count from `OrderItem → Product → Category`.

The older `GET /api/v1/admin/dashboard` still works and now counts in the database instead of
loading every order and payment into memory.

### 4.6 Display codes (was A5)

`User.referenceCode` is a stable human-facing code assigned at registration, never reused,
distinct from the numeric `id`. Returned by every user read.

| Role | Prefix | Example |
| --- | --- | --- |
| `FARMER` | `F-` | `F-014` |
| `BUYER` | `B-` | `B-001` |
| `SUPPLIER` | `S-` | `S-003` |
| `DRIVER` | `DA-` | `DA-007` |
| `INSPECTOR` | `IN-` | `IN-002` |
| `ADMIN` | `AD-` | `AD-001` |
| `SUPER_ADMIN` | `SA-` | `SA-001` |

Caveat worth knowing: the sequence is derived from the current role count, so two
simultaneous registrations can collide. Fine at current volume; move to a database sequence
before a launch spike.

### 4.7 Profile fields (was A10, A13–A16)

Added:

| Entity | Fields |
| --- | --- |
| `User` | `username`, `region`, `referenceCode` |
| `DriverProfile` | `vehicleMake`, `vehicleCapacityKg`, `licenseExpiryDate`, `idType`, `idNumber` |
| `SupplierProfile` | `contractEndDate` |

`registration` accepts `username` and `region`; both are also returned by login, `/auth/me`
and admin user reads.

**Still not added, and the app should not expect them:** `rating`, `revenue`, `ordersCount`,
`farms count`, `crops`. These are aggregates — computed from ratings, orders and products
rather than stored. None of that data is collected anywhere in the backend today. Adding the
columns would mean inventing numbers. Treat these as frontend-side display concerns until a
rating/settlement data model exists.

### 4.8 SendGrid email

Transactional mail is wired but **off by default**, so a missing key can never turn an
unrelated business failure into a 500.

| Env var | Default | Purpose |
| --- | --- | --- |
| `SENDGRID_ENABLED` | `false` | Master switch |
| `SENDGRID_API_KEY` | *(empty)* | Read from the environment only, never from the repo |
| `SENDGRID_FROM_EMAIL` | `no-reply@kilivana.com` | Must match a verified sender identity |
| `SENDGRID_FROM_NAME` | `Kilivana` | |

```
POST /api/v1/admin/mail/test          # ADMIN or SUPER_ADMIN
{ "to": "you@example.com", "subject": "Test" }
```

Returns `{configured, sent, from, to, subject}`. `sent: true` means SendGrid **accepted the
request**, not that a mailbox received it — check the SendGrid Activity tab for the real
delivery result.

Delivery failures are logged, never thrown: an unreachable mail provider must not fail the
operation that triggered the email. The only current sender is the delivery OTP.

### 4.9 Image uploads require Cloudinary credentials

`POST /api/v1/profiles/drivers/{userId}/images` (and every other image endpoint) depends on
Cloudinary. With no credentials the SDK threw `cloud_name is disabled` from inside its HTTP
layer, which reached clients as an opaque `500` — and over ngrok as an apparently dropped
connection, which was mistaken for a multipart parsing fault.

It is now a deliberate, self-explanatory failure:

```json
HTTP 503
{
  "success": false,
  "message": "Image storage is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET, then restart.",
  "error": { "code": "SERVICE_UNAVAILABLE", "details": "..." }
}
```

A warning naming the same variables is logged at startup. **Until the credentials are
supplied, no image upload in either app will succeed.** This is configuration, not code —
nothing else in the upload path is broken.

### 4.10 Admin order dispatch, driver suspension, cancel reason

- **Assign an order to a driver** — `POST /api/v1/admin/orders/{orderId}/assign?driverId=`,
  with optional `pickupAddress` and `destinationAddress` query parameters. One
  call does the whole dispatch: it reuses the order's pending delivery job or
  creates one (pickup defaults to the first seller's region, destination to the
  buyer's delivery address), assigns the driver, and the order becomes
  `confirmed` through that assignment. Returns the `LogisticsJob`.
- **Suspend / reinstate a driver** — `PUT /api/v1/admin/drivers/{userId}/suspend?reason=`
  (a reason is required) and `PUT /api/v1/admin/drivers/{userId}/unsuspend`.
  The account stays; the roster reads the driver as `suspended` with the reason
  on the profile until they are reinstated.
- **Cancel with a reason** — `POST /api/v1/orders/{id}/cancel?reason=`; the
  reason is stored on the order and returned as `cancellationReason`.
- **The order response is self-contained** — `OrderResponse` now carries
  `logisticsJobId` and `deliveryStatus`, the buyer (`buyerName`, `buyerPhone`,
  `buyerCounty`, `deliveryAddress`), the first line item's seller (`sellerName`,
  `sellerPhone`, `sellerCounty`, `sellerLocation`), the payment (`paymentMethod`,
  `paymentReference`, `paidAt`) and the assigned driver (`driverId`, `driverName`,
  `driverPhone`, `driverVehicle`, `driverPlate`), so the order detail view needs
  no second round trip per name.
- **A driver's job list** — `GET /api/v1/logistics/jobs/driver/{driverId}`,
  newest first (already listed in §2.3).

---

## 5. Local setup

```
# database
docker start kilivana-postgres        # host port 5433

# one-time, on any database predating SUPER_ADMIN
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V2__add_super_admin_role.sql

# one-time, for proof of delivery
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V3__proof_of_delivery_optional_fields.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V4__one_proof_of_delivery_per_job.sql

# backend
mvn spring-boot:run                   # http://localhost:8080

# public tunnel
ngrok http 8080                       # free hostname changes on every restart
```

The tunnel hostname rotates, so two things must be updated when it changes:

- `PUBLIC_BASE_URL=<tunnel-url>` so `/api-docs` advertises the tunnel and Swagger's
  "Try it out" does not post to a stale `http://localhost:8080`.
- The app's base URL.

CORS already allows `https://*.ngrok-free.dev` and `https://*.ngrok.app`, so no change is
needed per hostname.

> **ngrok free tier breaks browser preflight requests.** Any cross-origin browser client
> (the Angular admin panel) sends an `OPTIONS` preflight for every request carrying
> `Authorization`. ngrok free answers those with `502 ERR_NGROK_8012` — no CORS headers,
> and the `ngrok-skip-browser-warning: true` header cannot help because the *browser*
> generates the preflight and the app cannot attach headers to it. The admin panel then
> reports a "CORS preflight error" and shows an empty list, while Swagger UI (same-origin)
> and the Android driver app (native, no CORS, non-browser user agent) keep working.
> For browser clients use a Cloudflare quick tunnel instead (no account needed):
> `cloudflared tunnel --url http://localhost:8080` and point the panel at the
> `*.trycloudflare.com` URL — preflights return 200 with the backend's CORS headers.
> A paid ngrok plan also removes the block.

**Set `JWT_SECRET`.** With it unset the backend generates a random signing key at every
startup, so every token issued before a restart is rejected — a page refresh after a
restart then looks like the data "disappeared" (it is a 401, not missing data). The
local run command should include a stable value, e.g.
`JWT_SECRET=<base64 string of 32+ bytes> mvn spring-boot:run`. Access tokens expire after
30 minutes by default (`JWT_EXPIRATION=1800000`); set `JWT_EXPIRATION=0` for a never-expiring
token, useful for local testing.

Seeded accounts, password `Kilivana#2026`:
`admin`, `farmer`, `buyer`, `supplier`, `driver`, `inspector`, each at `@kilivana.local`
(e.g. `driver.test@kilivana.local`). Enabled by `DEV_SEED_ENABLED=true`; never enable in a
deployment.

Security note: with `JWT_SECRET` unset the backend generates a random signing key at startup,
so every token is invalidated by a restart. Set `JWT_SECRET` to a stable value. Access tokens
expire after 30 minutes by default (`JWT_EXPIRATION=1800000`); set `JWT_EXPIRATION=0` to issue
never-expiring tokens. Both warnings are logged at startup.

Email note: `SENDGRID_API_KEY` must be supplied through the environment and must never be
committed. Rotate the key if it is ever pasted into a chat, a log or a commit — that includes
the keys used for local verification. `.gitignore` blocks `.env*` and `sendgrid-secrets.*`.

## API Test Credentials

### Development Seeder

Set  to seed demo buyer-side products on startup:
- Farmer account (email: ): Products include Tomatoes and Sweet Potatoes
- Supplier account (email: ): Products include DAP Fertilizer and Hybrid Maize Seeds

All seeded products have  status and appear in .


## API Test Credentials

### Development Seeder

Set `DEV_SEED_ENABLED=true` to seed demo buyer-side products on startup:
- Farmer account (email: `farmer@kilivana.demo`): Products include Tomatoes and Sweet Potatoes
- Supplier account (email: `supplier@kilivana.demo`): Products include DAP Fertilizer and Hybrid Maize Seeds

All seeded products have `ACTIVE` status and appear in `GET /api/v1/products`.
