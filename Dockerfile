# syntax=docker/dockerfile:1

# Single deployable: the Expo web UI is built first and baked into the Spring Boot
# JAR, so one container serves the app at / and the API at /api/v1. Same origin
# means no CORS config and no second service to cold-start.

# ---- Stage 1: build the Expo web bundle ----
FROM node:20-bookworm-slim AS web
WORKDIR /web

# Install deps first so this layer caches unless the lockfile changes.
COPY mobile/package.json mobile/package-lock.json ./
RUN npm ci

COPY mobile/ ./

# Same-origin API: a relative base URL means the UI talks to whatever host it is
# served from, so the image needs no hardcoded backend URL. Metro inlines
# EXPO_PUBLIC_* at build time, so this must be set HERE, not at runtime.
ENV EXPO_PUBLIC_API_BASE_URL=/api/v1
RUN npx expo export --platform web

# ---- Stage 2: build the Spring Boot fat JAR ----
# Java 21 (matches <java.version>21</java.version>), Maven for the build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer caches unless pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src

# Drop the web bundle into the JAR's static resources. SpaWebConfig serves it.
COPY --from=web /web/dist/ ./src/main/resources/static/

# Tests run via Testcontainers and need Docker, so skip them in the image build —
# run them separately with `mvn verify`.
RUN mvn -B -q clean package -DskipTests

# ---- Stage 3: slim runtime with just a JRE + the JAR ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build /build/target/finance-tracker-*.jar app.jar
RUN chown spring:spring app.jar
USER spring

# Hosts like Render inject PORT and application.yml prefers it; 8099 is the local default.
EXPOSE 8099

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
