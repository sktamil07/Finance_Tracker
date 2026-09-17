# syntax=docker/dockerfile:1

# ---- Stage 1: build the Spring Boot fat JAR ----
# Java 21 (matches <java.version>21</java.version>), Maven for the build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer caches unless pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the application (tests run via Testcontainers and need Docker, so skip
# them in the image build — run them separately with `mvn verify`).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: slim runtime with just a JRE + the JAR ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build /build/target/finance-tracker-*.jar app.jar
RUN chown spring:spring app.jar
USER spring

# The API listens on 8098 (application.yml: ${SERVER_PORT:8098}).
EXPOSE 8098
ENV SERVER_PORT=8098

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
