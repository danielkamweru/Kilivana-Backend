# ---- Build stage: resolve dependencies, then package the JAR ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only the build definition first so Maven downloads the
# dependencies once; a code-only change reuses this layer.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---- Runtime stage: run the executable JAR on a JRE ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# The glob matches the repackaged executable JAR only; the
# Spring Boot plugin's *.jar.original side file does not end in .jar.
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
# Render injects PORT; Spring Boot reads it via server.port=${PORT:8080}.
ENTRYPOINT ["java", "-jar", "app.jar"]
