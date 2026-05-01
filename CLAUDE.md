# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Infrastructure (required before running any service)
```bash
docker-compose -f docker-compose.dev.yml up -d
```
Starts PostgreSQL, Neo4j, Kafka, Zookeeper, Redis, and OpenLDAP. The `init-db.sql` script runs automatically to create per-service PostgreSQL databases.

### Backend (Gradle multi-project)
```bash
# Run all services in parallel
./gradlew bootRun --parallel

# Run a specific service
./gradlew :services:circleguard-promotion-service:bootRun

# Run all tests
./gradlew test

# Run tests for a specific service
./gradlew :services:circleguard-auth-service:test
```
Integration tests use Testcontainers — they require Docker. The promotion service test profile (`application-test.yml`) disables Flyway/SQL init and expects a live Neo4j at `bolt://localhost:7687`.

### Mobile (`/mobile`)
```bash
cd mobile && npm install
npm run start      # Expo Go menu
npm run web        # Browser
npm run android    # Android emulator/device
npm run ios        # iOS simulator (macOS + Xcode required)
npm run test       # Jest unit tests
```

### API Exploration
Every service exposes Swagger UI at: `http://localhost:<port>/swagger-ui/index.html`

## Service Port Map

| Service | Port |
|---|---|
| Notification | 8082 |
| Identity | 8083 |
| Dashboard | 8084 |
| File | 8085 |
| Form | 8086 |
| Gateway | 8087 |
| Promotion | 8088 |
| Auth | 8180 |

Mobile base URLs are defined in [mobile/constants/Config.ts](mobile/constants/Config.ts).

## Architecture

### Monorepo layout
- `services/` — 8 Spring Boot microservices (Java 21, built with Kotlin DSL Gradle)
- `mobile/` — Expo React Native app (iOS, Android, Web from single codebase)
- `docker-compose.dev.yml` — local middleware stack
- `init-db.sql` — creates per-service PostgreSQL databases on first start

### Microservice responsibilities

**Auth Service** (`8180`): Dual-chain authentication — LDAP (university users) + local DB (guests). Issues JWT tokens and short-lived signed QR tokens for campus entry. The `DualChainAuthenticationProvider` tries LDAP first, falls back to local.

**Identity Service** (`8083`): Cryptographic vault — maps real identities to `anonymousId` UUIDs. Real names are AES-encrypted at rest (`IdentityEncryptionConverter`). All other services operate only on `anonymousId` to satisfy FERPA.

**Promotion Service** (`8088`): The core engine. Has a **hybrid dual data layer**:
- **Neo4j** — contact graph (`UserNode`, `CircleNode`, `EncounterRelationship`)
- **PostgreSQL** — spatial/infrastructure entities (buildings, floors, access points) and `SystemSettings`
- **Redis** — L2 cache for fast gate-entry status checks (`user:status:<anonymousId>`)
- **Kafka** — publishes `promotion.status.changed`, `alert.priority`, `circle.fenced` events

The `DataLayerConfig` explicitly partitions JPA repositories (`repository/jpa`) from Neo4j repositories (`repository/graph`) to prevent Spring from conflating the two transaction managers.

**HealthStatusService** performs recursive status propagation in a **single Cypher query**: when a user is marked CONFIRMED, direct contacts become SUSPECT, and their contacts become PROBABLE. Status cascade only travels two hops for non-confirmed promotions. Recovery uses a two-phase "Pulse Release" algorithm that restores contacts to ACTIVE only if they have no other remaining risk paths.

**Health status lifecycle**: `ACTIVE → SUSPECT → PROBABLE → CONFIRMED → RECOVERED`. A mandatory fence window (configurable via `SystemSettings.mandatoryFenceDays`) blocks return to ACTIVE before the window expires.

**Notification Service** (`8082`): Kafka consumer for `promotion.status.changed`. Dispatches Push/Email/SMS via strategy pattern (`NotificationDispatcher`). Also listens on `circle.fenced` for room reservation cancellation and `alert.priority` for administrative alerting.

**Form Service** (`8086`): Dynamic health questionnaire engine. Publishes completed surveys to Kafka, which the Promotion Service's `SurveyListener` consumes to trigger status evaluation.

**Gateway Service** (`8087`): Campus entry validation. Validates signed QR tokens (shared secret with Auth Service) and checks Redis for real-time user status — no Neo4j call on the hot path.

**Dashboard Service** (`8084`): Geospatial analytics with k-anonymity filtering (`KAnonymityFilter`) — suppresses data points below a minimum group size to prevent re-identification.

**File Service** (`8085`): Secure document/certificate storage, placeholder for S3/MinIO integration.

### Mobile app structure

Expo Router with file-based routing under `mobile/app/`:
- `(tabs)/` — authenticated tab group (requires `anonymousId` + JWT in `AuthContext`)
- `login.tsx`, `enroll.tsx` — public onboarding screens
- `visitor.tsx` — guest flow
- `questionnaire.tsx` — health form submission
- `modal.tsx` — guidelines/info modal

Auth state lives in `AuthContext` (persisted via `expo-secure-store` through `utils/storage`). The root layout redirects unauthenticated users to `/login` and authenticated users away from auth screens.

Background BLE proximity scanning runs as an `expo-task-manager` background task (`utils/proximityTask.ts`). The `useQrToken` hook rotates campus-entry QR tokens every 60 seconds by polling the Auth Service.

### Shared JWT secret
All services share the same JWT secret (`my-super-secret-dev-key-32-chars-long-12345678` in dev). Each service has its own `JwtAuthenticationFilter` — there is no API gateway; clients call services directly.

### Testing conventions
- Unit tests use H2 in-memory database (auth, identity services) — no Docker needed
- Promotion service tests require live Neo4j (configured via `application-test.yml` pointing to `localhost:7687`)
- Test classes follow the `*Test` suffix convention
- Mock beans are created with `@MockBean` (Spring) or Mockito

## Taller 2 CI/CD

Este repo es el **dev repo** del Taller 2. El **ops repo** es `circle-guard-ops` (mismo directorio padre `Taller2-Mio/`), que contiene Terraform, K8s manifests y Jenkinsfiles.

### 6 servicios seleccionados para el taller
Auth (8180) → Identity (8083) → Promotion (8088) → Notification (8082), Form (8086) → Kafka → Promotion, Gateway (8087) → Redis

### Pendiente en este repo (por implementar en orden)
1. **Dockerfiles** — uno por cada uno de los 6 servicios (`services/<nombre>/Dockerfile`), multi-stage con Gradle + eclipse-temurin JRE Alpine
2. **Jenkinsfile** en la raíz — pipeline ligero: build + test + docker push a ACR + notifica al ops repo
3. **5 pruebas unitarias nuevas**:
   - `services/circleguard-auth-service/src/test/.../JwtTokenServiceTest.java`
   - `services/circleguard-identity-service/src/test/.../IdentityVaultServiceUnitTest.java`
   - `services/circleguard-form-service/src/test/.../SymptomMapperEdgeCaseTest.java`
   - `services/circleguard-gateway-service/src/test/.../GatewayStatusMappingTest.java`
   - `services/circleguard-notification-service/src/test/.../NotificationPushContentTest.java`
4. **5 pruebas de integración nuevas**:
   - `QrTokenControllerIntegrationTest` (auth) — GET /api/v1/auth/qr/generate con JWT válido
   - `IdentityVaultServiceIntegrationTest` (identity) — service + repository con H2
   - `HealthSurveyKafkaIntegrationTest` (form) — submit survey → mensaje en Kafka (EmbeddedKafka)
   - `GateControllerIntegrationTest` (gateway) — POST /api/v1/gate/validate
   - `ExposureNotificationListenerIntegrationTest` (notification) — Kafka msg → dispatcher invocado
5. **Pruebas E2E** en `tests/e2e/` (Python/pytest, 5 escenarios)
6. **Locust** en `tests/performance/locustfile.py`
7. **Cambio en `notification-service/build.gradle.kts`** — agregar `spring-kafka-test` como dependencia de test
8. **`src/test/resources/application.yml`** en form-service, gateway-service y notification-service

### Workflow de aprobación obligatorio
Claude explica → discuten → usuario aprueba explícitamente → Claude ejecuta. Nunca hacer cambios sin aprobación.
