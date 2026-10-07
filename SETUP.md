# Kilivana Backend - Setup Requirements
# ======================================

## Prerequisites
- Java 21 (JDK 21+)
- Maven 3.9+ (or use Maven wrapper: ./mvnw)
- PostgreSQL 16
- Node.js 18+ (for optional frontend)

## Database Configuration
PostgreSQL connection details:
- Host: localhost
- Port: 5432
- Database: kilivana
- Username: kilivana_user
- Password: daniel kamweru (note the space)

### Create database and user:
```sql
CREATE USER kilivana_user WITH PASSWORD 'daniel kamweru';
CREATE DATABASE kilivana OWNER kilivana_user;
```

## Environment Variables
Required for startup:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/kilivana` | PostgreSQL connection URL |
| `DB_USERNAME` | `kilivana_user` | Database username |
| `DB_PASSWORD` | - | Database password (has space) |
| `JWT_SECRET` | - | JWT signing secret (min 32 bytes) |
| `SEED_DATA` | `false` | Set to `true` to seed test data |
| `SEED_ADMIN_EMAIL` | - | Seeded admin email |
| `SEED_ADMIN_PASSWORD` | - | Seeded admin password |
| `CORS_ALLOWED_ORIGINS` | `*` | Comma-separated CORS origins |
| `STORAGE_PROVIDER` | `database` | `database`, `local`, or `cloudinary` |
| `STORAGE_LOCAL_DIRECTORY` | `./uploads` | Local storage directory |
| `PORT` | `8080` | Server port |

## Optional: Cloudinary Configuration
For production image storage:
```
cloudinary.cloud-name=YOUR_CLOUD_NAME
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET
```

## Startup Command
```bash
DB_URL="jdbc:postgresql://localhost:5432/kilivana" \
DB_USERNAME="kilivana_user" \
DB_PASSWORD="daniel kamweru" \
JWT_SECRET="dev-stable-secret-key-for-local-testing" \
SEED_DATA=true \
SEED_ADMIN_EMAIL="admin.test@kilivana.local" \
SEED_ADMIN_PASSWORD="Kilivana#2026" \
CORS_ALLOWED_ORIGINS="http://localhost:4200,https://*.ngrok-free.dev" \
STORAGE_PROVIDER=database \
./mvnw spring-boot:run
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/register` - Register user
- `GET /api/v1/auth/me` - Get current user

### Admin - Users & Roles
- `GET /api/v1/admin/users` - List all users
- `GET /api/v1/admin/users/{id}` - Get user by id
- `POST /api/v1/admin/users` - Create user
- `PUT /api/v1/admin/users/{id}` - Update user
- `DELETE /api/v1/admin/users/{id}` - Delete user
- `PATCH /api/v1/admin/users/{id}/status` - Update user status

### Admin - Suppliers
- `GET /api/v1/admin/suppliers` - List suppliers
- `POST /api/v1/admin/suppliers` - Register supplier
- `PUT /api/v1/admin/suppliers/{userId}` - Edit supplier
- `PUT /api/v1/admin/suppliers/{userId}/suspend` - Suspend supplier
- `PUT /api/v1/admin/suppliers/{userId}/unsuspend` - Unsuspend supplier
- `DELETE /api/v1/admin/suppliers/{userId}` - Delete supplier

### Admin - Farmers
- `GET /api/v1/admin/farmers` - List farmers
- `GET /api/v1/admin/farmers/{id}` - Get farmer details
- `POST /api/v1/admin/farmers/{id}/approve` - Approve farmer
- `POST /api/v1/admin/farmers/{id}/reject` - Reject farmer
- `PUT /api/v1/admin/farmers/{userId}/suspend` - Suspend farmer
- `PUT /api/v1/admin/farmers/{userId}/unsuspend` - Unsuspend farmer

### Admin - Drivers
- `GET /api/v1/admin/drivers` - List drivers
- `POST /api/v1/admin/drivers` - Register driver
- `PUT /api/v1/admin/drivers/{userId}/suspend` - Suspend driver
- `PUT /api/v1/admin/drivers/{userId}/unsuspend` - Unsuspend driver

### Admin - Orders & Payments
- `GET /api/v1/admin/orders` - List orders
- `GET /api/v1/admin/payments` - List payments
- `GET /api/v1/admin/reports` - Get reports

### Admin - Categories
- `POST /api/v1/categories` - Create category
- `GET /api/v1/categories` - List categories (public)
- `GET /api/v1/categories/{id}` - Get category
- `PUT /api/v1/categories/{id}` - Update category
- `DELETE /api/v1/categories/{id}` - Delete category

### Admin - Crops
- `GET /api/v1/admin/crops` - List crops
- `POST /api/v1/admin/crops` - Create crop
- `PUT /api/v1/admin/crops/{id}` - Update crop
- `DELETE /api/v1/admin/crops/{id}` - Delete crop
- `PUT /api/v1/admin/crops/{id}/status` - Update crop status
- `GET /api/v1/admin/crops/summary` - Crop summary
- `GET /api/v1/admin/crops/types` - Crop types

### Admin - Farms
- `GET /api/v1/admin/farms` - List farms
- `POST /api/v1/admin/farms` - Create farm
- `GET /api/v1/admin/farms/{id}` - Get farm
- `PUT /api/v1/admin/farms/{id}` - Update farm

### Admin - Inspections
- `GET /api/v1/admin/inspections` - List inspections
- `POST /api/v1/admin/inspections` - Create inspection
- `PUT /api/v1/admin/inspections/{id}/status` - Update status
- `POST /api/v1/inspections/{id}/evidence/images` - Upload evidence image
- `GET /api/v1/inspections/{id}/evidence/images` - List evidence images
- `DELETE /api/v1/inspections/{id}/evidence/images/{imageId}` - Delete evidence image

### Admin - KYC Documents
- `GET /api/v1/admin/kyc` - List all KYC documents
- `GET /api/v1/admin/kyc/pending` - List pending KYC documents
- `POST /api/v1/admin/kyc/{id}/approve` - Approve KYC document
- `POST /api/v1/admin/kyc/{id}/reject` - Reject KYC document

### Admin - Notifications
- `POST /api/v1/notifications` - Create notification
- `GET /api/v1/notifications` - List all notifications
- `GET /api/v1/notifications/{id}` - Get notification
- `POST /api/v1/notifications/{id}/read` - Mark as read
- `GET /api/v1/notifications/user/{userId}/unread` - List unread for user
- `DELETE /api/v1/notifications/{id}` - Delete notification

### Ecommerce Notifications
- `POST /api/v1/ecommerce/notifications/orders/{orderId}` - Order notification
- `POST /api/v1/ecommerce/notifications/payments/{paymentId}` - Payment notification
- `POST /api/v1/ecommerce/notifications/disputes/{disputeId}` - Dispute notification
- `POST /api/v1/ecommerce/notifications/inventory/{productId}` - Inventory notification
- `GET /api/v1/ecommerce/notifications` - List user notifications
- `POST /api/v1/ecommerce/notifications/read-all` - Mark all as read

### Products
- `GET /api/v1/products` - List products (public)
- `GET /api/v1/products/{id}` - Get product
- `POST /api/v1/products` - Create product (multipart for images)
- `PUT /api/v1/products/{id}` - Update product
- `DELETE /api/v1/products/{id}` - Delete product
- `GET /api/v1/products/{id}/images` - List product images
- `POST /api/v1/products/{id}/images` - Upload product image
- `GET /api/v1/sellers/{sellerId}/products` - Products by seller

### Orders
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders/{id}` - Get order
- `GET /api/v1/orders` - List orders
- `GET /api/v1/orders/buyer/{buyerId}` - Orders by buyer
- `POST /api/v1/orders/{id}/status` - Update order status
- `POST /api/v1/orders/{id}/confirm-receipt` - Confirm receipt
- `POST /api/v1/orders/{id}/cancel` - Cancel order
- `GET /api/v1/orders/search` - Search orders
- `GET /api/v1/orders/{id}/timeline` - Order timeline

### Payments
- `POST /api/v1/payments` - Create payment
- `GET /api/v1/payments/{id}` - Get payment
- `GET /api/v1/payments/order/{orderId}` - Payments by order
- `POST /api/v1/payments/initiate` - Initiate payment
- `PUT /api/v1/payments/{id}/status` - Update payment status
- `POST /api/v1/payments/{id}/refund` - Refund payment
- `POST /api/v1/payments/webhook` - M-Pesa webhook

### Cart
- `GET /api/v1/cart` - Get cart
- `POST /api/v1/cart/items` - Add item to cart
- `PUT /api/v1/cart/items/{itemId}` - Update cart item
- `DELETE /api/v1/cart/items/{itemId}` - Remove item
- `DELETE /api/v1/carts/{cartId}` - Delete cart

### Checkout
- `POST /api/v1/checkout` - Checkout
- `POST /api/v1/checkout/validate` - Validate checkout

### Disputes
- `POST /api/v1/disputes` - Create dispute
- `GET /api/v1/disputes/{id}` - Get dispute
- `GET /api/v1/disputes/order/{orderId}` - Disputes by order
- `GET /api/v1/disputes/user/{userId}` - Disputes by user
- `PUT /api/v1/disputes/{id}/status` - Update dispute status
- `PUT /api/v1/disputes/{id}/resolution` - Set dispute resolution

### Categories
- `GET /api/v1/categories` - List categories
- `GET /api/v1/categories/{id}` - Get category
- `GET /api/v1/categories/type/{type}` - Category by type
- `GET /api/v1/categories/active` - Active categories

### Logistics Jobs
- `GET /api/v1/logistics/jobs` - List delivery jobs
- `POST /api/v1/logistics/jobs` - Create delivery job
- `GET /api/v1/logistics/jobs/{id}` - Get job
- `PUT /api/v1/logistics/jobs/{id}/status` - Update status
- `POST /api/v1/logistics/jobs/{id}/assign` - Assign driver
- `POST /api/v1/logistics/jobs/{jobId}/proof-of-delivery` - Upload POD
- `GET /api/v1/logistics/jobs/driver/{driverId}` - Jobs by driver

### Profiles
- `GET /api/v1/profiles/drivers/{userId}` - Driver profile
- `POST /api/v1/profiles/drivers/{userId}` - Create driver profile
- `POST /api/v1/profiles/drivers/{userId}/images` - Upload driver images
- `GET /api/v1/profiles/farmers/{userId}` - Farmer profile
- `PUT /api/v1/profiles/farmers/{userId}` - Update farmer profile
- `POST /api/v1/profiles/farmers/{userId}/images` - Upload farmer images
- Similar endpoints for suppliers, buyers, inspectors

### Addresses
- `GET /api/v1/addresses` - List addresses
- `POST /api/v1/addresses` - Create address
- `PUT /api/v1/addresses/{id}` - Update address
- `DELETE /api/v1/addresses/{id}` - Delete address

### Reference Data
- `GET /api/v1/crop-types` - Active crop types (public)
- `GET /api/v1/regions` - Kenyan counties (public)
- `GET /api/v1/regions/currency` - Currency info (public)

### Image Retrieval
- `GET /api/v1/images/{publicId}` - Serve stored image (database provider)
- `GET /uploads/**` - Serve local files (local storage provider)

### Health
- `GET /api/v1/health` - Health check (public)
- `GET /` - Basic health (public)

### Auth Endpoints (no token required)
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

## Key Configuration Files
- `src/main/resources/application.properties` - Main config
- `src/main/java/com/kilivana/backend/config/CorsConfig.java` - CORS config
- `src/main/java/com/kilivana/backend/config/SecurityConfig.java` - Security config
- `src/main/java/com/kilivana/backend/config/CloudinaryConfig.java` - Cloudinary config

## Testing with ngrok
To test with ngrok, set CORS allowed origins to include your ngrok URL or wildcard pattern:
```
CORS_ALLOWED_ORIGINS="http://localhost:4200,https://*.ngrok-free.dev"
```
