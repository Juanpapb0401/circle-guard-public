# Release Notes -- v10

**Fecha:** 2026-05-02
**Build:** 10
**Branch:** main

## Cambios incluidos

- d9ba67e fix(perf): absolute URL in batch surveys + LDAP timeout + HikariCP pool (e5jparra1)
- 676cfa8 fix(auth): declare identity.service.url in application.yml for reliable env-var override (e5jparra1)
- 4f511ba fix(locust): skip survey if login fails, remove null otherSymptoms field (e5jparra1)
- ee9dc93 fix(locust): use real anonymousId in HealthSurveyBatchUser (e5jparra1)
- 5e8dc10 fix(auth): make identity service URL configurable via env var (e5jparra1)
- abdff27 fix: permit actuator health in identity, disable kafka/mail health in notification (e5jparra1)
- a16b993 fix: add spring-boot-starter-actuator to all 6 services (e5jparra1)
- 8c18783 fix(promotion): create system_settings table in V2 migration (e5jparra1)
- f25f950 perf(ci): copy pre-built JARs in Dockerfiles, eliminate Gradle under QEMU (e5jparra1)
- eb826ce fix(ci): build Docker images for linux/amd64 platform (e5jparra1)
- 8ac3d63 fix(notification-tests): mock JavaMailSender to fix SpringBoot context loading) (e5jparra1)
- aa674f6 fix: notification tests - add kafka bootstrap fallback and auto-startup=false for unit tests (e5jparra1)
- 5e9b1f1 chore: ignore proyect files (e5jparra1)
- d34c758 feat: tests (e5jparra1)
- a4971cb ci: add root Jenkinsfile for dev pipeline (e5jparra1)
- 0bf29f6 test: add 12 new unit tests across auth, identity, form, and gateway services (e5jparra1)
- bc3fe88 feat: add multi-stage Dockerfiles for 6 microservices (e5jparra1)
- 9653aba Merge remote-tracking branch 'upstream/master' (e5jparra1)
- 538bd0f complement front (Juan Carlos Muñoz)
- a1d5f41 startup and tests fixed (Juan Carlos Muñoz)
- dce49ac Add implementation on front and back (Juan Carlos Muñoz)
- 06133f8 Merge branch 'master' (e5jparra1)
- f959b8b first commit (Juan Carlos Muñoz)
- adc5261 Initial commit (Juan C. Muñoz-Fernández)

## Servicios desplegados

| Servicio        | Puerto | Imagen                                                        |
|-----------------|--------|---------------------------------------------------------------|
| Auth            | 8180   | circleguardacr.azurecr.io/circleguard-auth:10          |
| Identity        | 8083   | circleguardacr.azurecr.io/circleguard-identity:10      |
| Promotion       | 8088   | circleguardacr.azurecr.io/circleguard-promotion:10     |
| Notification    | 8082   | circleguardacr.azurecr.io/circleguard-notification:10  |
| Form            | 8086   | circleguardacr.azurecr.io/circleguard-form:10          |
| Gateway         | 8087   | circleguardacr.azurecr.io/circleguard-gateway:10       |
