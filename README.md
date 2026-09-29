# Kilivana Backend

Kilivana Backend is the Spring Boot API for the Kilivana platform, covering admin operations, e-commerce features, and logistics workflows. The application exposes REST endpoints, Swagger documentation, and health monitoring for local development and public tunneling via ngrok.

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

Create the database and user locally if they do not already exist:

```sql
CREATE DATABASE kilivana;
GRANT ALL PRIVILEGES ON DATABASE kilivana TO kilivana_user;
```

Then confirm the app configuration in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/kilivana
spring.datasource.username=kilivana_user

```

## Run the Application

From the project root:

```bash
mvn clean install
CLOUDINARY_CLOUD_NAME=your_cloud_name \
CLOUDINARY_API_KEY=your_api_key \
CLOUDINARY_API_SECRET=your_api_secret \
mvn spring-boot:run
```

The backend will run on:

- Local: http://localhost:8080
- Health check: http://localhost:8080/actuator/health

## Swagger Documentation

Swagger UI is available at:

- Local: http://localhost:8080/swagger-ui/index.html
- Local fallback: http://localhost:8080/swagger-ui.html
- Public ngrok: https://either-juvenile-progeny.ngrok-free.dev/swagger-ui/index.html

API docs JSON is available at:

- Local: http://localhost:8080/api-docs
- Public ngrok: https://either-juvenile-progeny.ngrok-free.dev/api-docs

## API Contract Coverage

The backend keeps the existing `/api/v1/ecommerce/...` and admin routes and also exposes the documented canonical routes, including:

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
POST http://localhost:8080/api/v1/ecommerce/products
Authorization: Bearer <token>
X-User-Id: <user-id>
Content-Type: multipart/form-data

product={"name":"Fresh Mangoes","categoryId":1,"price":80.00,"unit":"kg","stockQty":50,"minimumOrderQty":1,"sellerType":"FARMER"};type=application/json
images=@photo.png
```

The `sellerId` in the response is derived from the authenticated `X-User-Id` header, not from the request body.

**Upload an additional image:**

```http
POST http://localhost:8080/api/v1/ecommerce/products/{productId}/images
X-User-Id: <user-id> (must be product owner or ADMIN)
Content-Type: multipart/form-data

image=@photo.png
sortOrder=1
isPrimary=false
```

**Get product images:**

```http
GET http://localhost:8080/api/v1/ecommerce/products/{productId}/images
```

**Replace an image:**

```http
PUT http://localhost:8080/api/v1/ecommerce/products/{productId}/images/{imageId}
X-User-Id: <user-id (owner or ADMIN)>
Content-Type: multipart/form-data

image=@new-photo.png
```

**Set primary image:**

```http
PUT http://localhost:8080/api/v1/ecommerce/products/{productId}/images/{imageId}/primary
X-User-Id: <user-id (owner or ADMIN)>
```

**Reorder images:**

```http
PUT http://localhost:8080/api/v1/ecommerce/products/{productId}/images/reorder
X-User-Id: <user-id (owner or ADMIN)>
Content-Type: application/json

[10, 12, 11]
```

**Delete an image (removes from Cloudinary and PostgreSQL):**

```http
DELETE http://localhost:8080/api/v1/ecommerce/products/{productId}/images/{imageId}
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

The backend identifies users via the `X-User-Id` request header. A user must either own the resource or have the ADMIN role to perform mutating operations.

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

The application is configured with Spring Security so the main public routes remain open while other endpoints stay protected. The following endpoints are allowed publicly:

- `/api/v1/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/api-docs/**`
- `/actuator/**`

CORS is enabled for local frontend development and ngrok origins, including:

- http://localhost:3000
- http://localhost:5173
- http://localhost:8080
- https://*.ngrok-free.dev
- https://*.ngrok.app

## Ngrok Public Access

To expose the app publicly, start ngrok in the terminal:

```bash
ngrok http 8080
```

The working public Swagger route is:

- https://either-juvenile-progeny.ngrok-free.dev/swagger-ui/index.html

The working health route is:

- https://either-juvenile-progeny.ngrok-free.dev/actuator/health

If you see a browser warning or 403 page from ngrok, add this header in the browser request or API client:

```http
ngrok-skip-browser-warning: true
```

This is a browser-side warning from ngrok and not an application error.

## Development Notes

### Local startup sequence

1. Make sure PostgreSQL is running.
2. Confirm the `kilivana` database exists.
3. Confirm the `kilivana_user` role exists with the correct password.
4. Start the Spring Boot app.
5. Open Swagger or health endpoints in the browser.

### Common checks

```bash
pg_isready -h localhost -p 5432
psql -h localhost -p 5432 -U kilivana_user -d kilivana -c "select 1;"
curl http://localhost:8080/actuator/health
```

## Useful Commands

```bash
mvn clean install
mvn spring-boot:run
mvn test
mvn clean package
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
