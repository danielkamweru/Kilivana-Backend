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
CREATE USER kilivana_user WITH PASSWORD 'daniel kamweru';
GRANT ALL PRIVILEGES ON DATABASE kilivana TO kilivana_user;
```

Then confirm the app configuration in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/kilivana
spring.datasource.username=kilivana_user
spring.datasource.password=daniel kamweru
```

## Run the Application

From the project root:

```bash
mvn clean install
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
