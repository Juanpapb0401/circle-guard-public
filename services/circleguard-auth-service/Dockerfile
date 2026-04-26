# ── Stage 1: build ──────────────────────────────────────────────
# Contexto de build: raíz del repo (circle-guard-public/)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copiar archivos de build primero (capa cacheada si no cambian)
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-auth-service/build.gradle.kts \
     services/circleguard-auth-service/build.gradle.kts
RUN chmod +x gradlew

# Copiar fuentes del servicio
COPY services/circleguard-auth-service/src \
     services/circleguard-auth-service/src

# Compilar y empaquetar — tests corren en el pipeline antes de este paso
RUN ./gradlew :services:circleguard-auth-service:bootJar --no-daemon -x test

# ── Stage 2: runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build \
     /workspace/services/circleguard-auth-service/build/libs/*.jar \
     app.jar

EXPOSE 8180

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
