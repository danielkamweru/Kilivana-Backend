# Kilivana Backend - Architecture & Development Guide

> **For AI agents**: This file describes the codebase structure, conventions, and key patterns.
> Read this before making changes to ensure consistency with existing patterns.

## Project Overview

**Kilivana Backend** - Spring Boot 3.2.0 Java API for a marketplace platform connecting farmers, suppliers, buyers, and logistics drivers.

**Package Structure:**
```
com.kilivana.backend
├── admin/          # Admin endpoints (users, suppliers, farmers, drivers, inspections, KYC, notifications)
├── ecommerce/      # E-commerce (products, orders, payments, cart, categories)
├── farm/           # Farm-specific entities (crops, farms, farm images, KYC documents)
├── logistics/      # Delivery logistics (jobs, tracking, drivers, proof of delivery)
├── mail/           # Email/SMS services
├── config/         # Spring configuration (CORS, Security, Cloudinary)
├── security/       # JWT authentication
├── common/         # Shared DTOs, exceptions, enums, base classes
└── [root]          # Application entry point
```

## Key Technologies
- Java 21
- Spring Boot 3.2.0
- Spring Security + JWT
- Hibernate/JPA (ddl-auto=update)
- PostgreSQL 16
- Cloudinary (for image storage in production)

## Authentication
- JWT tokens: `{"alg": "HS256", "sub": "<userId>", "role": "ADMIN", "type": "access"}`
- Login: `POST /api/v1/auth/login` with `{email, password}` returns `{accessToken, refreshToken, user}`
- Header: `Authorization: Bearer <token>`
- `ngrok-skip-browser-warning: true` header required for ngrok requests

## API Response Format
All responses use `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Success",
  "data": { /* response data */ },
  "timestamp": "2026-10-07T10:24:24.123+00:00",
  "error": null
}
```

Error responses:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": { "field": "message" },
  "error": { "code": "VALIDATION_ERROR", "details": "..." }
}
```

Error codes: `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`, `EMAIL_TAKEN`, `USERNAME_TAKEN`, `PHONE_TAKEN`, `CONFLICT`, `UNAUTHORIZED`, `METHOD_NOT_ALLOWED`

## Key Entities

### User
- `id` (PK), `name`, `email`, `phone`, `username`, `passwordHash`, `role` (BUYER/FARMER/SUPPLIER/DRIVER/INSPECTOR/ADMIN), `status` (ACTIVE/INACTIVE/SUSPENDED/PENDING_VERIFICATION), `region`
- Related tables: `supplier_profiles`, `farmer_profiles`, `buyer_profiles`, `driver_profiles`

### Product
- `id`, `sellerId`, `sellerType` (FARMER/SUPPLIER), `categoryId`, `name`, `description`, `pricePerUnit`, `unit`, `stockQty`, `status`
- Images stored in `product_images` table

### Order
- `id`, `buyerId`, `totalAmount`, `status` (PENDING/PROCESSING/...), `paymentStatus`
- Events stored in `order_events` table

## Common Patterns

### Entity → Response DTO
Use `fromEntity()` static method pattern:
```java
public static InventoryResponse fromEntity(Inspection inspection) {
    return InventoryResponse.builder()
        .id(inspection.getId())
        // ... fields
        .build();
}
```

### Service layer
- Service classes in `service/` subpackages
- Use `@RequiredArgsConstructor` for DI
- `@Transactional` on write operations
- Throw `ResourceNotFoundException("EntityName", id)` for not found
- Return DTOs, not entities

### Controllers
- Base path: `/api/v1/...`
- Use `ApiResponse<T>` wrapper for responses
- Use `@AuthenticationPrincipal Long userId` for authenticated user
- `List<>` response type for list endpoints (not `Page<>`)

### Validation
- Use `@Valid` on request bodies
- Error messages: field name → error message map
- 400 status for validation errors

## Image Upload Pattern
1. Controller endpoint: `@PostMapping(value = "/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)`
2. Service: `cloudinaryService.uploadImage(image, folder)` returns `Map<String, Object>`
3. Save entity with URL from `cloudinaryService.getSecureUrl(result)` and publicId from `getPublicId(result)`
4. Storage provider: `STORAGE_PROVIDER` env var (default: `database`)

## CORS Configuration
- `CorsConfig.java` handles all CORS
- Default: `*` allows all origins with credentials for ngrok patterns
- Explicit origins: set `CORS_ALLOWED_ORIGINS` env var
- Always allows: `http://localhost:4200`, `https://*.ngrok-free.dev`, `https://*.ngrok.io`
- Required header: `ngrok-skip-browser-warning` for ngrok requests

## Public (Unauthenticated) Endpoints
- `GET /api/v1/crop-types` - List active crop types
- `GET /api/v1/regions` - List Kenyan counties
- `GET /api/v1/regions/currency` - Get currency info
- `GET /api/v1/categories` - List categories
- `GET /api/v1/products` - Public product search
- `GET /api/v1/products/{id}` - Get single product (public)
- `GET /api/v1/categories/active` - Active categories
- Health: `GET /`, `GET /api/v1/health`
- Auth: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`

## Startup Commands
```bash
# Local development with seeded data
DB_URL="jdbc:postgresql://localhost:5432/kilivana" \
DB_USERNAME="kilivana_user" \
DB_PASSWORD="daniel kamweru" \
JWT_SECRET="dev-stable-secret-key-for-local-testing" \
SEED_DATA=true \
SEED_ADMIN_EMAIL="admin.test@kilivana.local" \
SEED_ADMIN_PASSWORD="Kilivana#2026" \
CORS_ALLOWED_ORIGINS="http://localhost:4200,https://*.ngrok-free.dev" \
./mvnw spring-boot:run

# With local storage instead of database
STORAGE_PROVIDER=local ./mvnw spring-boot:run

# With Cloudinary
STORAGE_PROVIDER=cloudinary \
CLOUDINARY_CLOUD_NAME=your-cloud \
CLOUDINARY_API_KEY=your-key \
CLOUDINARY_API_SECRET=your-secret \
./mvnw spring-boot:run
```

## Testing
- Run: `./mvnw test`
- Tests use `@DataJpaTest` with H2 in-memory database
- Test classes mirror production package structure (e.g., `admin.service` → `admin.service` test)

## Code Conventions
- Lombok: `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- JPA: `@Entity`, `@Table(name = "snake_case")`, `@CreationTimestamp`/`@UpdateTimestamp`
- Enums: `@Enumerated(EnumType.STRING)` for persistence
- Exception handling: `GlobalExceptionHandler` catches all exceptions
- Response: Always wrap in `ApiResponse.success(data)` or `ApiResponse.successMessage("...")`
- No comments in code unless necessary
- Use `snake_case` for database columns

## Frontend Integration Notes
- Frontend API base: `https://either-juvenile-progeny.ngrok-free.dev/api/v1`
- Frontend file: `src/app/core/services/api.service.ts` (currently stubbed)
- Key frontend files: `farmer.model.ts`, `farmer.service.ts`, `farmer-details.ts`
- KYC documents fetched from `GET /admin/kyc`, filtered by `userId` on frontend

## Environment Variables Reference
```
DB_URL=jdbc:postgresql://localhost:5432/kilivana
DB_USERNAME=kilivana_user
DB_PASSWORD=daniel kamweru
JWT_SECRET=<32+ char secret>
SEED_DATA=true/false
SEED_ADMIN_EMAIL=admin.test@kilivana.local
SEED_ADMIN_PASSWORD=Kilivana#2026
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://*.ngrok-free.dev
STORAGE_PROVIDER=database|local|cloudinary
PORT=8080
JWT_EXPIRATION=0 (dev no-expiry)
JWT_REFRESH_EXPIRATION=604800000 (7 days)
```

## Database Schema Notes
- `users` table: core user data (role column: BUYER, FARMER, SUPPLIER, DRIVER, INSPECTOR, ADMIN)
- `supplier_profiles`, `farmer_profiles`, `driver_profiles`, `buyer_profiles`: role-specific profiles
- `kyc_documents`: verification documents (document_type, document_status, url)
- `notifications`: in-app notifications
- `orders`, `order_events`, `order_items`: order management
- `payments`: payment records
- `categories`, `products`, `product_images`: product catalogue
- `farms`, `farm_images`, `crops`, `crop_types`: farm data
- `logistics_jobs`, `tracking_events`, `proof_of_deliveries`: delivery tracking
- `addresses`: user addresses
- Image tables use `BaseImageEntity` (id, publicId, assetId, url, sortOrder, isPrimary, timestamps)
