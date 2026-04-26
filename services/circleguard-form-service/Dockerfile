# ── Stage 1: build ──────────────────────────────────────────────
# Contexto de build: raíz del repo (circle-guard-public/)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-form-service/build.gradle.kts \
     services/circleguard-form-service/build.gradle.kts
RUN chmod +x gradlew

COPY services/circleguard-form-service/src \
     services/circleguard-form-service/src

RUN ./gradlew :services:circleguard-form-service:bootJar --no-daemon -x test

# ── Stage 2: runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build \
     /workspace/services/circleguard-form-service/build/libs/*.jar \
     app.jar

EXPOSE 8086

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
