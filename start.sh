#!/bin/bash
# Set environment variables
export DB_URL="jdbc:postgresql://localhost:5433/kilivana"
export DB_USERNAME="kilivana_user"
export DB_PASSWORD="daniel kamweru"
export JWT_SECRET="dev-stable-secret-key-for-local-testing"
export SEED_DATA=false
export SEED_ADMIN_EMAIL="admin.test@kilivana.local"
export SEED_ADMIN_PASSWORD="Kilivana#2026"
export CORS_ALLOWED_ORIGINS="http://localhost:4200,https://*.ngrok-free.dev"
export STORAGE_PROVIDER=database

# Run the application
./mvnw spring-boot:run
