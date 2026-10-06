# Kilivana Backend

Kilivana Backend is the Spring Boot API for the Kilivana platform, covering admin operations, e-commerce features, and logistics workflows. The application exposes REST endpoints, OpenAPI documentation, and health monitoring for local development and deployment on Render.

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- Spring Web
- Spring Data JPA
- PostgreSQL
- Spring Security
- Springdoc OpenAPI / Swagger UI
- Maven

## Project Structure

- `src/main/java/com/kilivana/backend` – application source code
- `src/main/resources/application.properties` – runtime configuration
- `src/test/java` – tests
- `pom.xml` – Maven project configuration

## Prerequisites

Before starting the app, make sure you have:

- Java 21 installed
- Maven installed
- PostgreSQL installed and running
- A database named `kilivana`
- A PostgreSQL user named `kilivana_user`

## Database Setup

The datasource is read from `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, or — on Render — from the platform's `DATABASE_URL`, which `DatabaseUrlEnvironmentPostProcessor` translates into the datasource settings at startup. `DATABASE_URL` wins whenever it is present.

Create the database and user locally if they do not already exist:

```sql
CREATE DATABASE kilivana;
GRANT ALL PRIVILEGES ON DATABASE kilivana TO kilivana_user;
```

A development instance also runs as a Docker container on port 5433 (`docker start kilivana-postgres`; user `kilivana_user`, database `kilivana`). The defaults in `src/main/resources/application.properties` are:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/kilivana}
spring.datasource.username=${DB_USERNAME:}
spring.datasource.password=${DB_PASSWORD:}
```

## Run the Application

From the project root (the Maven wrapper downloads the matching Maven version):

```bash
./mvnw clean install
./mvnw spring-boot:run
```

To run against the local Docker database and seed the demo accounts
(including the administrator) on a fresh database, set the seed variables:

```bash
docker start kilivana-postgres
DB_URL=jdbc:postgresql://localhost:5433/kilivana \
DB_USERNAME=kilivana_user \
DB_PASSWORD='daniel kamweru' \
SEED_DATA=true \
SEED_ADMIN_PASSWORD='Admin@123' \
SEED_DEMO_PASSWORD='Kilivana#2026' \
SEED_ENFORCE_PASSWORDS=true \
./mvnw spring-boot:run
```

The backend will run on:

- Local: http://localhost:8080
- Health check: http://localhost:8080/api/v1/health

### Seeded accounts

With `SEED_DATA=true` the seeder creates these accounts on every boot
(idempotent; `SEED_ENFORCE_PASSWORDS=true` keeps their passwords equal to
the configured values). The same accounts work locally and on the Render
deployment:

| Role | Email | Password |
|---|---|---|
| `ADMIN` | `admin@kilivana.com` | whatever `SEED_ADMIN_PASSWORD` is set to (`Admin@123` in `render.yaml`) |
| `FARMER` | `farmer@kilivana.demo` | `Kilivana#2026` |
| `BUYER` | `buyer@kilivana.demo` | `Kilivana#2026` |
| `SUPPLIER` | `supplier@kilivana.demo` | `Kilivana#2026` |
| `DRIVER` | `driver@kilivana.demo` | `Kilivana#2026` |
| `INSPECTOR` | `inspector@kilivana.demo` | `Kilivana#2026` |

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@kilivana.com","password":"Admin@123"}'
```

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port. Render injects its own value. |
| `DATABASE_URL` | *(empty)* | Full `postgres://user:password@host:port/database` URL, set by Render's attached PostgreSQL service. Takes precedence over `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`. |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local defaults | Datasource settings for local development. |
| `JWT_SECRET` | *(empty)* | HS512 signing key. **Set this in any shared environment** — if unset, every restart invalidates all issued tokens. |
| `JWT_EXPIRATION` | `0` | Access-token lifetime in ms. `0` means no expiry. |
| `JWT_REFRESH_EXPIRATION` | `0` | Refresh-token lifetime in ms. |
| `CORS_ALLOWED_ORIGINS` | `*` | Comma-separated browser origins; wildcard patterns allowed, so `*` permits every origin (the default). Restrict to the KilivanaAdmin2 origin for a locked deployment. |
| `PUBLIC_BASE_URL` | *(empty)* | Public HTTPS base URL used to build image URLs and the OpenAPI server entry, e.g. `https://kilivana-backend-a44w.onrender.com`. |
| `SEED_DATA` | `false` | Populates a fresh database with Kenyan demo data on startup. Idempotent: a restart never duplicates or overwrites records. |
| `SEED_ADMIN_EMAIL` | `admin@kilivana.com` | Email of the seeded initial administrator. |
| `SEED_ADMIN_PASSWORD` | *(empty)* | Password for the seeded administrator. When blank the administrator account is not created — set it here, in the environment, or as a Render secret. |
| `SEED_DEMO_PASSWORD` | `Kilivana#2026` | Shared password of the seeded demo accounts. Change it before anyone else uses the deployment. |
| `SEED_ENFORCE_PASSWORDS` | `false` | Reset a seeded account's password to the configured value when it no longer matches. Use for a demo environment whose seeded accounts are the intended logins; leave `false` in production. |
| `STORAGE_PROVIDER` | `database` | Image storage: `local`, `database`, or `cloudinary`. The Render filesystem is ephemeral, so `database` (the default) is the right choice there. |
| `STORAGE_FALLBACK_PROVIDER` | `database` | Provider used when the primary one fails. `none` fails hard instead. |
| `STORAGE_LOCAL_DIRECTORY` | `./uploads` | Disk directory for the `local` provider. |
| `CLOUDINARY_*` | *(empty)* | Image upload credentials, used only when `STORAGE_PROVIDER=cloudinary`. |
| `SENDGRID_ENABLED` | `false` | Master switch for transactional email. |
| `SENDGRID_API_KEY` | *(empty)* | SendGrid key, read from the environment only. |
| `SENDGRID_FROM_EMAIL` | `no-reply@kilivana.com` | Must match a verified SendGrid sender identity. |
| `SENDGRID_FROM_NAME` | `Kilivana` | Display name on outgoing mail. |

### Image uploads

`STORAGE_PROVIDER` picks where image bytes go:

| Value | Behaviour |
| --- | --- |
| `local` | Writes to `app.storage.local.directory` (`./uploads`) and serves `{PUBLIC_BASE_URL}/uploads/...`. |
| `database` *(default)* | Stores the bytes as `bytea` in `stored_images` and serves `{PUBLIC_BASE_URL}/api/v1/images/{publicId}`. |
| `cloudinary` | Uploads to Cloudinary, which needs the three credentials below. |

With `cloudinary`, a missing or failing Cloudinary account no longer breaks uploads: the image is
stored in PostgreSQL instead, using the `database` provider's storage. Set
`STORAGE_FALLBACK_PROVIDER=none` to fail hard on Cloudinary errors and get
`503 SERVICE_UNAVAILABLE` naming the missing variables.

Both public image URLs are readable without a Bearer token, because they are embedded in `<img>`
tags and mobile payloads that cannot send one: `GET /uploads/**` and `GET /api/v1/images/**`. Every
other endpoint still requires authentication.

```bash
CLOUDINARY_CLOUD_NAME=your_cloud \
CLOUDINARY_API_KEY=your_key \
CLOUDINARY_API_SECRET=your_secret \
mvn spring-boot:run
```

### Database migrations

There is no migration tool; `spring.jpa.hibernate.ddl-auto=update` handles new columns. It does
**not** rewrite existing CHECK constraints when an enum grows, so schema changes that alter an
enum need a manual script:

```bash
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V2__add_super_admin_role.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V3__proof_of_delivery_optional_fields.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V4__one_proof_of_delivery_per_job.sql
docker exec -i kilivana-postgres psql -U kilivana_user -d kilivana \
  < db/migrations/V6__unique_email_case_insensitive.sql
```

`V4` deletes duplicate proof-of-delivery rows before adding its unique index, so read the
comment at the top of that file before running it against anything other than development.

`V6` lowercases stored email addresses and adds a unique index on `lower(email)`, because the
application compares addresses case-insensitively while the generated constraint did not: it let
`Jane@example.com` and `jane@example.com` register as two accounts.

## Swagger Documentation

Swagger UI is available at:
- ngrok: https://either-juvenile-progeny.ngrok-free.dev/swagger-ui/index.html
- Local: http://localhost:8080/swagger-ui/index.html
- Local fallback: http://localhost:8080/swagger-ui.html
- Deployed: https://kilivana-backend.onrender.com/swagger-ui/index.html

API docs JSON is available at:

- Local: http://localhost:8080/api-docs
- Deployed: https://kilivana-backend-a44w.onrender.com/api-docs

## API Contract Coverage

The backend exposes the canonical `/api/v1/...` routes, including:

- Authentication: register, login, logout, refresh, password recovery/reset, and current-user lookup
- Users and profiles: user lookup, own-profile update, addresses, and farmer, buyer, supplier, inspector, and driver profile CRUD
- Marketplace: products, categories, seller products, cart, checkout, orders, payments, and refunds
- Operations: inspections, notifications, logistics assignment/status, tracking locations, and proof of delivery

Profile and business records preserve the existing scalar-ID relationship model. Each profile has a unique `userId`, products retain `sellerId` and `categoryId`, orders retain `buyerId` and `addressId`, and logistics records retain `orderId` and `driverId`. No existing entity package or table structure was moved.

The current development authentication service returns demo access/refresh tokens. Production deployment still requires JWT or secure session-token storage, revocation, and role-based authorization instead of the development-open security configuration.

## Cloudinary Image Management

The backend integrates with Cloudinary for image storage. All image files are uploaded directly to your configured Cloudinary account; PostgreSQL stores only metadata (public IDs, URLs, sort order, primary flag).

### Configuration

Cloudinary credentials are loaded from environment variables. Create a `.env` file (ignored by git) or set them in your shell:

```env
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

The `.env.example` file contains placeholders only. Never commit real credentials.

### Centralized Service

All domains use one centralized service:

```
com.kilivana.backend.common.service.CloudinaryService
```

Operations:

- `uploadImage(file, folder)` — uploads to a Cloudinary folder, returns secure URL, public ID, asset ID
- `deleteImage(publicId)` — deletes a Cloudinary asset by public ID
- `replaceImage(oldPublicId, newFile, folder)` — uploads the new image first, then deletes the old asset only if the upload succeeds

Image validation:

- File must be present and non-empty
- Content-Type must start with `image/` (JPEG, PNG, GIF, etc.)
- File size must be under 10 MB

### Product Image API

Farmers and suppliers who create products own those products. Only the product owner or an ADMIN can modify images.

**Create a product with images:**

```http
POST http://localhost:8080/api/v1/products
Authorization: Bearer <token>
X-User-Id: <user-id>
Content-Type: multipart/form-data

product={"name":"Fresh Mangoes","categoryId":1,"price":80.00,"unit":"kg","stockQty":50,"minimumOrderQty":1,"sellerType":"FARMER"};type=application/json
images=@photo.png
```

The `sellerId` in the response is derived from the authenticated `X-User-Id` header, not from the request body.

**Upload an additional image:**

```http
POST http://localhost:8080/api/v1/products/{productId}/images
X-User-Id: <user-id> (must be product owner or ADMIN)
Content-Type: multipart/form-data

image=@photo.png
sortOrder=1
isPrimary=false
```

**Get product images:**

```http
GET http://localhost:8080/api/v1/products/{productId}/images
```

**Replace an image:**

```http
PUT http://localhost:8080/api/v1/products/{productId}/images/{imageId}
X-User-Id: <user-id (owner or ADMIN)>
Content-Type: multipart/form-data

image=@new-photo.png
```

**Set primary image:**

```http
PUT http://localhost:8080/api/v1/products/{productId}/images/{imageId}/primary
X-User-Id: <user-id (owner or ADMIN)>
```

**Reorder images:**

```http
PUT http://localhost:8080/api/v1/products/{productId}/images/reorder
X-User-Id: <user-id (owner or ADMIN)>
Content-Type: application/json

[10, 12, 11]
```

**Delete an image (removes from Cloudinary and PostgreSQL):**

```http
DELETE http://localhost:8080/api/v1/products/{productId}/images/{imageId}
X-User-Id: <user-id (owner or ADMIN)>
```

### Profile Image API

Farmers, suppliers, and drivers can upload images for their own profiles. Admins can access any profile's images.

Folders in Cloudinary:

- `kilivana/farmers/{userId}/` for farmer profile images
- `kilivana/suppliers/{userId}/` for supplier profile images
- `kilivana/drivers/{userId}/` for driver profile images

**Upload profile image:**

```http
POST http://localhost:8080/api/v1/profiles/farmers/{userId}/images
X-User-Id: <user-id> (must match {userId} or be ADMIN)
Content-Type: multipart/form-data

image=@photo.png
isPrimary=true
```

**Get profile images:**

```http
GET http://localhost:8080/api/v1/profiles/farmers/{userId}/images
X-User-Id: <user-id (must match {userId} or be ADMIN)>
```

**Set primary image:**

```http
PUT http://localhost:8080/api/v1/profiles/farmers/{userId}/images/{imageId}/primary
X-User-Id: <user-id (must match {userId} or be ADMIN)>
```

**Delete profile image (removes from Cloudinary and PostgreSQL):**

```http
DELETE http://localhost:8080/api/v1/profiles/farmers/{userId}/images/{imageId}
X-User-Id: <user-id (must match {userId} or be ADMIN)>
```

Supplier and driver endpoints follow the same pattern at `/suppliers/` and `/drivers/`.

### Authorization Rules

| Role | Create product | Upload product image | Replace/delete product image | Upload profile image | View profile images |
|------|---------------|---------------------|----------------------------|---------------------|---------------------|
| Farmer | own products | own products | own products | own profile | own profile (ADMIN can view any) |
| Supplier | own products | own products | own products | own profile | own profile (ADMIN can view any) |
| Admin | any | any | any | any | any |

The backend identifies users from the JWT bearer token, not from a request header. A user must either own the resource or hold a staff role (`ADMIN` or `SUPER_ADMIN`) to perform mutating operations.

## SendGrid Email

Transactional email is delivered through the SendGrid HTTP API. It is **disabled by default**
so a missing API key can never turn an unrelated business failure into a 500.

```bash
SENDGRID_ENABLED=true \
SENDGRID_API_KEY=your_key \
SENDGRID_FROM_EMAIL=no-reply@kilivana.com \
mvn spring-boot:run
```

Verify the integration:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@kilivana.com","password":"Admin@123"}' \
  | jq -r '.data.accessToken')

curl -X POST http://localhost:8080/api/v1/admin/mail/test \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"to":"you@example.com","subject":"Test"}'
```

The response reports `configured` and `sent`. **`sent: true` means SendGrid accepted the
request, not that a mailbox received it** — check the SendGrid Activity tab for the real
delivery outcome.

Current senders:

| Trigger | Recipient | Content |
|---|---|---|
| A logistics job is created | The buyer on the job's order | The 6-digit delivery handover code |

`SendGridMailService` exposes `send(to, subject, body)` for plain text and
`sendHtml(to, subject, htmlBody)`. Both log delivery failures rather than throwing, so an
unreachable mail provider cannot fail the operation that triggered the email.

The sender address must match a **verified sender identity** in SendGrid, otherwise the API
call succeeds and delivery is silently dropped or quarantined.

> Rotate `SENDGRID_API_KEY` immediately if it is ever pasted into a chat, a log or a commit.
> It is read from the environment only and is never stored in the repository; `.gitignore`
> blocks `.env*` and `sendgrid-secrets.*`.

## Frontend Integration Contract

Use `http://localhost:8080` as the local base URL. The existing versioned routes remain supported, and compatibility aliases are available for the shorter authentication/user paths used by the frontend specification:

| Operation | Preferred route | Compatibility route |
| --- | --- | --- |
| Register | `POST /api/v1/auth/register` | `POST /api/users` |
| Login | `POST /api/v1/auth/login` | `POST /api/auth/login` |
| Refresh | `POST /api/v1/auth/refresh` | `POST /api/auth/refresh` |
| Current user | `GET /api/v1/auth/me` | `GET /api/auth/me` |

Registration body:

```json
{
	"name": "Daniel Kamweru",
	"email": "daniel@example.com",
	"phone": "0712345678",
	"password": "secret123",
	"role": "BUYER"
}
```

Login body:

```json
{
	"email": "daniel@example.com",
	"password": "secret123"
}
```

Every successful response uses `{ "success": true, "message": "Success", "data": ... }`. Validation and business errors use `{ "success": false, "message": ..., "data": ..., "error": { "code": ..., "details": ... } }`.

The current development login response contains `accessToken`, `refreshToken`, `tokenType`, and `user`. The values are development demo tokens, not JWTs. Do not implement frontend JWT refresh or protected-route assumptions until a real JWT provider/filter is added.

For the current development `/me` and own-resource routes, send:

```http
X-User-Id: <user-id>
```

The API documentation is available at `http://localhost:8080/api-docs`, and the interactive Swagger UI is at `http://localhost:8080/swagger-ui/index.html`.

## Security and CORS

Spring Security protects every endpoint that is not listed below. Authentication uses a Bearer access token (`Authorization: Bearer <token>`); `/api/v1/admin/**` additionally requires the `ADMIN` role.

Public without a token:

- Auth: `POST /api/v1/auth/register`, `/login`, `/refresh`, `/forgot-password`, `/reset-password` (plus the `/api/auth/...` compatibility aliases)
- Registration: `POST /api/v1/users`, `POST /api/users`
- Reference data: `GET /api/v1/regions`, `GET /api/v1/regions/**`
- Health: `GET /api/v1/health`, `GET /actuator/health`, `GET /actuator/info`
- Images: `GET /uploads/**`, `GET /api/v1/images/**` (readable from `<img>` tags and mobile clients that cannot attach a token)
- Documentation: `/swagger-ui/**`, `/api-docs/**`, `/v3/api-docs/**`

CORS is configured from `CORS_ALLOWED_ORIGINS`; the local default is:

- http://localhost:4200
- http://localhost:8080
- http://127.0.0.1:4200

## Render Deployment

The repository ships a [`render.yaml`](render.yaml) blueprint: it builds the JAR with Maven, runs it on the port Render assigns, and attaches the PostgreSQL service's `DATABASE_URL` automatically.

1. Push the repository to GitHub.
2. In the Render dashboard, create a **Web Service** from this repository (or import `render.yaml` as a blueprint).
3. Attach a **PostgreSQL** database to the service; Render injects `DATABASE_URL`.
4. Set the remaining values in the service dashboard:
   - `JWT_SECRET` — a long random string (Render can generate it)
   - `SEED_ADMIN_PASSWORD` — the initial administrator's password (a secret)
   - `SEED_DEMO_PASSWORD` — change the shared demo-account password
   - `CORS_ALLOWED_ORIGINS` — the KilivanaAdmin2 origin
   - `PUBLIC_BASE_URL` — the service's own URL
5. The health check is `GET /api/v1/health`; Render polls it to mark the service live.

With `SEED_DATA=true` a fresh database is populated with Kenyan demo data on first boot. The seed is idempotent, so a restart or redeploy never duplicates or overwrites records; set `SEED_DATA=false` once the data you want is in place. Uploaded images default to `STORAGE_PROVIDER=database` because the Render filesystem is ephemeral.

## Development Notes

### Local startup sequence

1. Make sure PostgreSQL is running.
2. Confirm the `kilivana` database exists.
3. Confirm the `kilivana_user` role exists with the correct password.
4. Start the Spring Boot app.
5. Open Swagger or health endpoints in the browser.

### Common checks

```bash
pg_isready -h localhost -p 5433
psql -h localhost -p 5433 -U kilivana_user -d kilivana -c "select 1;"
curl http://localhost:8080/api/v1/health
```

## Useful Commands

```bash
# Exercise every API group against a running backend and print a pass/fail table.
# Bash and curl only. Retries dropped connections so transient
# reported as a broken endpoint.
./scripts/smoke-test.sh
./scripts/smoke-test.sh https://kilivana-backend.onrender.com
```

Test credentials for every role and a per-endpoint verified-status table live in
[`docs/API_TEST_CREDENTIALS.md`](docs/API_TEST_CREDENTIALS.md).

```bash
./mvnw clean install
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

## Git Convention

This repository follows conventional commits for alignment and readability, for example:

- `feat: add new endpoint`
- `fix: resolve database connection issue`
- `docs: add project README`
- `chore: update configuration`

The project stays well under the recommended limit of 20 commits while keeping a clear and consistent history.

## License

This project is currently configured for internal development use. Add a license file if you plan to open-source or distribute it externally.
