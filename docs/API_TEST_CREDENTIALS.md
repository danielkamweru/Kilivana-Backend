# API Test Credentials & Verified Status

Everything here was executed against a running backend. Re-verify at any time with:

```bash
./scripts/smoke-test.sh                                        # local
./scripts/smoke-test.sh https://either-juvenile-progeny.ngrok-free.dev   # via tunnel
```

Current result: **48 passed, 0 failed** on both.

| | |
|---|---|
| Swagger UI | `https://either-juvenile-progeny.ngrok-free.dev/swagger-ui/index.html` |
| OpenAPI JSON | `https://either-juvenile-progeny.ngrok-free.dev/api-docs` |
| Local | `http://localhost:8080` |
| Test suite | 65 tests, `mvn -o test` |

---

## 1. Using Swagger

Swagger's **401 responses are expected until you Authorize**:

1. `POST /api/v1/auth/login` → Execute
2. Copy `data.accessToken`
3. Click **Authorize** (top right) → paste the **raw token, no `Bearer` prefix**
4. Now the locked padlocks turn unlocked and requests carry the token

---

## 2. Test accounts

Password for all seeded accounts: **`Kilivana#2026`**
Seeded by `DEV_SEED_ENABLED=true`. **Never enable this in a deployment.**

| Role | Email | Ref code | ID | Reaches |
|---|---|---|---|---|
| `ADMIN` | `admin.test@kilivana.local` | `AD-001` | 8 | `/api/v1/admin/**` |
| `SUPER_ADMIN` | `superadmin.test@kilivana.local` | `SA-001` | 12 | `/api/v1/admin/**` (same as ADMIN) |
| `FARMER` | `farmer.test@kilivana.local` | `F-001` | 3 | `/api/v1/profiles/farmers/{id}` |
| `BUYER` | `buyer.test@kilivana.local` | `B-001` | 4 | cart, orders, addresses |
| `SUPPLIER` | `supplier.test@kilivana.local` | `S-001` | 5 | `/api/v1/profiles/suppliers/{id}` |
| `DRIVER` | `driver.test@kilivana.local` | `DA-001` | 6 | driver profile, jobs, proof of delivery |
| `INSPECTOR` | `inspector.test@kilivana.local` | `IN-001` | 7 | `/api/v1/profiles/inspectors/{id}` |

Every role now has a seeded profile row, so the profile screens have data to display.

### Get a token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin.test@kilivana.local","password":"Kilivana#2026"}' | jq -r '.data.accessToken'
```

Or through the tunnel — add `-H "ngrok-skip-browser-warning: true"` to bypass the
interstitial page.

---

## 3. Endpoints that need a token

Only these groups are public; everything else needs the bearer token.

| Group | Public |
|---|---|
| Authentication | `POST /api/v1/auth/login`, `/register`, `/refresh`, `/forgot-password`, `/reset-password` |
| Health & system | `GET /`, `/actuator/health`, `/api-docs` |

---

## 4. Role enforcement

Proven by the smoke test — these are the *expected* non-200 responses:

| Request | Status | Why |
|---|---|---|
| `GET /api/v1/admin/**` with no token | `401` | no token |
| `GET /api/v1/admin/dashboard/stats` with a DRIVER token | `403` | valid token, wrong role |
| `GET /api/v1/admin/mail/test` with a DRIVER token | `403` | valid token, wrong role |
| `GET /api/v1/profiles/drivers/{otherUserId}` with a DRIVER token | `403` | a driver may only read their own profile |

---

## 5. Verified working, by area

Every row below was executed and returned the listed status.

### Authentication
| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/auth/login` (correct) | 200 |
| POST | `/api/v1/auth/login` (wrong password) | 401 |
| GET | `/api/v1/auth/me` (no token) | 401 |
| GET | `/api/v1/auth/me` (with token) | 200 |

### Administration — users and reporting
| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/admin/users` | 200 |
| GET | `/api/v1/admin/users/role/FARMER` | 200 |
| GET | `/api/v1/admin/users/status/ACTIVE` | 200 |
| GET | `/api/v1/admin/dashboard` | 200 |
| GET | `/api/v1/admin/dashboard/stats` | 200 |
| GET | `/api/v1/admin/orders` | 200 |
| GET | `/api/v1/admin/payments` | 200 |
| GET | `/api/v1/admin/logistics/jobs` | 200 |
| GET | `/api/v1/admin/reports` | 200 |
| GET | `/api/v1/admin/audit-logs` | 200 |
| GET | `/api/v1/admin/notifications` | 200 |

### E-commerce
| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/categories` | 200 |
| GET | `/api/v1/categories/active` | 200 |
| GET | `/api/v1/products/search` | 200 |
| GET | `/api/v1/sellers/1/products` | 200 |
| GET | `/api/v1/orders` | 200 |
| GET | `/api/v1/orders/search` | 200 |
| GET | `/api/v1/cart` (as BUYER) | 200 |

### Role profiles
| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/profiles/drivers/6` | 200 |
| GET | `/api/v1/profiles/drivers/6/images` | 200 |
| GET | `/api/v1/profiles/drivers/3` (as DRIVER, not own) | 403 |
| GET | `/api/v1/profiles/farmers/3` | 200 |
| GET | `/api/v1/profiles/buyers/4` | 200 |
| GET | `/api/v1/profiles/suppliers/5` | 200 |
| GET | `/api/v1/profiles/inspectors/7` | 200 |

### Logistics
| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/logistics/jobs` | 200 |
| GET | `/api/v1/logistics/jobs/driver/6` | 200 |
| GET | `/api/v1/logistics/jobs/status/PENDING_ASSIGNMENT` | 200 |
| GET | `/api/v1/logistics/proof-of-delivery/driver/6` | 200 |
| GET | `/api/v1/logistics/tracking-events/driver/6` | 200 |
| POST | `/api/v1/logistics/jobs` | 200 |
| POST | `/api/v1/logistics/jobs/{id}/otp/verify?otp=000000` | 400 wrong code |
| POST | `/api/v1/logistics/jobs/{id}/proof-of-delivery` (no OTP) | 400 |

### Email and addresses
| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/admin/mail/test` (empty `to`) | 400 |
| POST | `/api/v1/admin/mail/test` (DRIVER token) | 403 |
| POST | `/api/v1/admin/mail/test` (no token) | 401 |
| GET | `/api/v1/addresses` | 200 |
| GET | `/api/v1/notifications/user/4` | 200 |

---

## 6. Endpoints that need configuration you have not supplied

These are **not backend bugs**. Both are account credentials only you can provide.

### Image uploads — needs Cloudinary

| Method | Path | Status | Meaning |
|---|---|---|---|
| POST | `/api/v1/profiles/drivers/6/images` (wrong part name) | **400** | correct: part must be named `image` |
| POST | `/api/v1/profiles/drivers/6/images` (valid image) | **503** | `SERVICE_UNAVAILABLE` — storage not configured |

The 400 proves multipart parsing and validation work correctly. The 503 is the only
outstanding step:

```bash
CLOUDINARY_CLOUD_NAME=... CLOUDINARY_API_KEY=... CLOUDINARY_API_SECRET=...
```

Upload part name is exactly **`image`**. Optional `?isPrimary=true`.

### Email — needs SendGrid

`POST /api/v1/admin/mail/test` with a valid `to` sends successfully once configured. It is
currently running with `SENDGRID_ENABLED=true` and a key set, so it will attempt delivery;
`Sent by SendGrid? No` means the API accepted the request. Check the SendGrid **Activity**
tab for actual delivery, and confirm the sender identity is verified.

---

## 7. Things that will still confuse you

**`deliveryOtp` no longer exists on the job response.** The code is BCrypt-hashed and
emailed to the buyer; it is never returned by the API. The driver does not read it out —
the *customer* does. Job reads expose `deliveryOtpExpiresAt` and `deliveryOtpVerified`.

**Status is a query parameter**, not a body:
`PUT /api/v1/logistics/jobs/{id}/status?status=ACCEPTED`

**A job has no `customer` field.** It carries `orderId`; resolve the customer through
`GET /api/v1/orders/{orderId}`. Deliberate — see the integration report §4.3.

**Proof of delivery requires the OTP**, and only one per job. A second attempt is
`409 This delivery has already been confirmed`. `signatureUrl`, `photoUrl` and
`otpReference` are all optional; `recipientName` is the only field required.

**ngrok drops connections intermittently.** A dropped request shows HTTP `000` with no
body. The smoke test retries these automatically; if you see them in Swagger, just Execute
again. Locally all 48 checks pass on the first attempt.

**Every response uses the same envelope:**
`{success, message, data, timestamp, error: {code, details}}`

---

## 8. Database

One container, data persists across restarts:

```bash
docker start kilivana-postgres     # host port 5433
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V2__add_super_admin_role.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V3__proof_of_delivery_optional_fields.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V4__one_proof_of_delivery_per_job.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V5__backfill_reference_codes.sql
```

Read the comment at the top of `V4` before running it anywhere but development — it deletes
duplicate proof-of-delivery rows.

There is also an unused container `e-commerce-postgres-1`, never started. Safe to
`docker rm e-commerce-postgres-1`.