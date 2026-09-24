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

- http://localhost:8080/swagger-ui/index.html
- http://localhost:8080/swagger-ui.html

API docs JSON is available at:

- http://localhost:8080/api-docs

## Security and CORS

The project is configured to allow public access to Swagger, actuator, and API endpoints while keeping other routes protected by Spring Security.

CORS is enabled for local frontend development and ngrok origins such as:

- http://localhost:3000
- http://localhost:5173
- https://*.ngrok-free.dev
- https://*.ngrok.app

## Ngrok Public Access

To expose the app publicly:

```bash
ngrok http 8080
```

Then use the generated public URL, for example:

- https://your-random-domain.ngrok-free.dev

Swagger UI through ngrok:

- https://your-random-domain.ngrok-free.dev/swagger-ui/index.html

If you see a browser warning from ngrok, add this header in the request:

```http
ngrok-skip-browser-warning: true
```

This is a browser-side warning from ngrok, not a backend failure.

## Useful Commands

```bash
mvn test
mvn clean package
./mvnw spring-boot:run
```

## Git Convention

This repository follows conventional commits for alignment and readability, for example:

- `feat: add new endpoint`
- `fix: resolve database connection issue`
- `docs: add project README`
- `chore: update configuration`

The project currently stays well under the recommended limit of 20 commits while keeping a clean, readable history.

## License

This project is currently configured for internal development use. Add a license file if you plan to open-source or distribute it externally.
