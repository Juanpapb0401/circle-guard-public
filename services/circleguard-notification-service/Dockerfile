# ── Stage 1: build ──────────────────────────────────────────────
# Contexto de build: raíz del repo (circle-guard-public/)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-notification-service/build.gradle.kts \
     services/circleguard-notification-service/build.gradle.kts
RUN chmod +x gradlew

COPY services/circleguard-notification-service/src \
     services/circleguard-notification-service/src

RUN ./gradlew :services:circleguard-notification-service:bootJar --no-daemon -x test

# ── Stage 2: runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build \
     /workspace/services/circleguard-notification-service/build/libs/*.jar \
     app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
