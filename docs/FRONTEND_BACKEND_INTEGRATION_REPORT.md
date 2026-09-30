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

> **`expiresIn` is `null`, not a number.** Tokens are configured to never expire
> (`JWT_EXPIRATION=0`), which is convenient for local testing but means a client must
> declare this field nullable. If the backend is later given a real expiry it becomes a
> millisecond count.

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
| D2 | Job has `pickupLat`, `pickupLon`, `dropoffLat`, `dropoffLon` | `LogisticsJob` has **no coordinates at all** — only `pickupAddress` / `destinationAddress` text | **Map and OSRM routing cannot work.** The app builds a route from coordinates; the backend never sends any. |
| D3 | Job has `deliveryOtp` (comment says backend generates it and SMSes the buyer) | `LogisticsJob` has no OTP field. `ProofOfDelivery.otpReference` exists but is only written at proof time, so the driver has nothing to show the recipient | **Cannot confirm delivery** |
| D4 | Job has `cargo`, `quantity`, `payoutKsh`, `customer`, `distanceKm`, `estimatedTime`, `pickupTime`, `dropoffTime` | None of these exist on `LogisticsJob`; it carries only `orderId` and the two address strings | Jobs list cannot be populated |
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
| `POST` | `/api/v1/logistics/jobs/{jobId}/proof-of-delivery` | Body = `ProofOfDelivery`: `logisticsJobId`, `recipientName`, `signatureUrl`, `photoUrl`, `otpReference`. |
| `GET` | `/api/v1/logistics/proof-of-delivery/job/{jobId}` | |
| `GET` | `/api/v1/logistics/tracking-events/job/{jobId}` | |
| `GET` | `/api/v1/notifications/user/{userId}` | |

> Note the status endpoint takes `?status=` in the query string. The driver app will send it
> in a JSON body and get a 400.

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
| A5 | `code: string` (`'B-001'`, `'F-001'`, `'S-001'`, `'DA-001'`) | No such field on any entity | Needs a backend field or a client-side display convention |
| A6 | `role: 'admin' \| 'super-admin'` | `UserRole` = `FARMER, BUYER, SUPPLIER, INSPECTOR, DRIVER, ADMIN` | **There is no SUPER_ADMIN.** Either collapse to `ADMIN`, or a role is needed in the backend |
| A7 | `fullName`, `initials` | `name`; no initials | Map `name`; derive initials client-side |
| A8 | `status: 'verified' \| 'pending' \| 'suspended'` | `UserStatus` = `ACTIVE, INACTIVE, SUSPENDED` and separately `VerificationStatus` = `PENDING, VERIFIED, REJECTED` | The app collapses two backend concepts into one field — needs splitting |
| A9 | `KycStatus` on driver, farmer, supplier models | No KYC/verification entity beyond `User.verificationStatus` | Needs a decision (see §4) |
| A10 | `region`, `crops[]`, `farms`, `rating`, `revenue`, `ordersCount`, `totalSpend`, `disputesCount` | None of these are stored | Most aggregation fields do not exist server-side |
| A11 | `DashboardStats` with `totalFarmers`, `monthlyRevenue`, `avgOrderValue`, `OrderTrendPoint`, `CropSlice`, `ActivityItem` | No statistics/analytics endpoint | Needs a new backend reporting endpoint |
| A12 | 12 of 19 models are **empty interfaces**: orders, products, disputes, logistics, farms-crops, inspectors, kyc, notifications, payments, reports, settings, users | Full APIs exist for all of these | The contracts have to be written before the screens can be wired |
| A13 | `vehicle: { type, capacity, plateNumber, make }` | `DriverProfile` = `licenseNumber, vehicleType, vehicleNumber, vehicleDetails, availabilityStatus` | No capacity, make or plate field |
| A14 | `licenceNumber`, `licenceExpiry`, `idType`, `idNumber` | `licenseNumber` only; no expiry, ID type or ID number | Needs backend fields |
| A15 | `username` on driver/supplier models | `email` only; no username column | Needs a backend field or client-side removal |
| A16 | `contractEnd` on supplier | No such field | Needs a backend field |
| A17 | Password reset expects a `forgot-password` flow that fails for unknown emails | `POST /api/v1/auth/forgot-password?email=...` | Use it, and always resolve (never reveal whether an email exists) |

### 3.3 Endpoints the admin app should consume

Admin routes require the `ADMIN` role; a non-admin token gets **403**.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/login`, `/refresh`, `/logout`, `/forgot-password?email=`, `/reset-password`; `GET /api/v1/auth/me` |
| Users & roles | `GET/POST /api/v1/admin/users`, `GET /api/v1/admin/users/role/{role}`, `/status/{status}`, `GET /api/v1/admin/users/{id}`, `PUT/PATCH /api/v1/admin/users/{id}/status`, `GET /api/v1/admin/audit-logs` |
| Addresses | `GET/POST /api/v1/addresses`, `GET/PUT/DELETE /api/v1/addresses/{id}` |
| Farmers | `GET/POST /api/v1/profiles/farmers/{userId}`, `PUT`, `DELETE`; images at `.../images` |
| Buyers | `GET/POST /api/v1/profiles/buyers/{userId}`, `PUT`, `DELETE` |
| Suppliers | `GET/POST /api/v1/profiles/suppliers/{userId}`, `PUT`, `DELETE`; images at `.../images` |
| Inspectors | `GET/POST /api/v1/profiles/inspectors/{userId}`, `PUT`, `DELETE`; images at `.../images` |
| Products | `GET/POST /api/v1/admin/products`, `/admin/products/{id}/status`, images, categories |
| Orders | `GET/POST /api/v1/admin/orders`, `/admin/orders/{id}/status`, disputes |
| Payments | `GET /api/v1/admin/payments`, `/admin/payments/{id}/status`, refunds |
| Inspections | `GET/POST /api/v1/admin/inspections`, `/admin/inspections/{id}/result`, `/status` |
| Notifications | `GET/POST /api/v1/admin/notifications`, `/admin/notifications/user/{userId}`, `/read-all` |
| Logistics | `GET /api/v1/logistics/jobs`, `/jobs/status/{status}`, `/jobs/{id}`, `PUT /jobs/{id}/assign`, `/cancel` |

As an admin, **every** profile is accessible, not just your own. A non-admin calling an
admin route gets 403.

---

## 4. Backend gaps — decisions needed before the apps can be finished

These are the items where the frontend cannot be unblocked by frontend work alone.

1. **Job coordinates (D2).** The driver app's map and routing are central to the product and
   need pickup and drop-off latitude/longitude. `Address` already stores `latitude` and
   `longitude`, but `LogisticsJob` stores addresses as free text and `createLogisticsJob`
   takes strings, so there is nothing to derive coordinates from. This needs a decision:
   link jobs to `Address` rows, or add coordinate columns to `LogisticsJob` and populate them
   at creation.

2. **Delivery OTP (D3).** The app expects the backend to generate a delivery code and send it
   to the buyer, so the driver can confirm hand-off. No OTP is generated anywhere today;
   `ProofOfDelivery.otpReference` is only stored at proof time. Needs generating, storing and
   a verification step.

3. **Job payload (D4).** `cargo`, `quantity`, `payout`, `customer` and timestamps are not on
   `LogisticsJob`. Today a job references only `orderId`; the items live under the order.
   Decide whether the job response should embed order/customer details, or whether the app
   makes a second call to the order endpoint.

4. **SUPER_ADMIN (A6).** The admin app has `super-admin`; the backend has only `ADMIN`.
   Either the app drops the concept or the backend gains a role.

5. **Statistics endpoint (A11).** The dashboard needs aggregation the backend does not expose.

6. **Display codes (A5).** `code` fields like `B-001` appear on four admin models and have no
   backend equivalent.

7. **Denormalised profile fields (A10, A13, A14, A15, A16).** Region, rating, revenue, order
   counts, vehicle make/capacity/plate, licence expiry, username, contract end. Each needs
   either a new column or a decision to derive them.

---

## 5. Local setup

```
# database
docker start kilivana-postgres        # host port 5433

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

Seeded accounts, password `Kilivana#2026`:
`admin`, `farmer`, `buyer`, `supplier`, `driver`, `inspector`, each at `@kilivana.local`
(e.g. `driver.test@kilivana.local`). Enabled by `DEV_SEED_ENABLED=true`; never enable in a
deployment.

Security note: with `JWT_SECRET` unset the backend generates a random signing key at startup,
so every token is invalidated by a restart. Set `JWT_SECRET` to a stable value. Tokens also
currently never expire (`JWT_EXPIRATION=0`); set it to a millisecond duration to restore
expiry. Both warnings are logged at startup.
