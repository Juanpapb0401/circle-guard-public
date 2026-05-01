---
name: Taller 2 - CI/CD CircleGuard
description: Contexto completo del Taller 2 universitario de CI/CD sobre el proyecto CircleGuard
type: project
originSessionId: 85c732f8-fa44-4fc6-8b4e-2f7067e34a54
---
## Objetivo del taller
Taller universitario de CI/CD para el proyecto CircleGuard (sistema de rastreo de contactos universitario). Requiere:
1. (10%) Configurar Jenkins, Docker y Kubernetes
2. (15%) Pipelines para 6+ microservicios en ambiente dev (build + test + deploy)
3. (30%) Pruebas: 5 unitarias nuevas, 5 integración nuevas, 5 E2E nuevas, Locust de rendimiento
4. (15%) Pipelines para ambiente stage (K8s)
5. (15%) Pipeline master con Release Notes automáticas
6. (15%) Documentación y video (máx 8 min)

## Arquitectura de repos (GitOps) — ACORDADA
Dos repositorios separados:
- **Dev repo**: `circle-guard-public` (este repo, en GitHub del profe: jcmunozf/circle-guard-public) — código fuente, Dockerfiles, tests, Jenkinsfile ligero
- **Ops repo**: `circle-guard-ops` (NUEVO, público en GitHub del usuario) — Terraform, K8s manifests, Jenkinsfiles completos, docker-compose de Jenkins

## Stack tecnológico — DECIDIDO
- **Cloud**: Azure (créditos estudiante: $100)
- **Infra como código**: Terraform → provisiona AKS (Azure Kubernetes Service) + ACR (Azure Container Registry)
- **Jenkins**: corre LOCAL via Docker Compose (en el ops repo), NO en Azure VM (economiza créditos, diferencia mínima en costo)
- **Kubernetes**: AKS con 3 namespaces: circleguard-dev, circleguard-stage, circleguard-prod
- **Patrón**: Spin up (terraform apply) cuando se necesita / Spin down (terraform destroy) al terminar
- **Razón de Jenkins local**: Más simple de debuggear, arranque inmediato, patrón válido en empresas reales

## Estado al cierre de la sesión 2026-04-26
- [x] gh CLI instalado por el usuario
- [x] Directorio `circle-guard-ops/` creado localmente con `git init` (por el usuario, pasos 1 y 2 hechos)
- [x] CLAUDE.md del dev repo creado con documentación del proyecto
- [x] Memoria guardada
- [ ] **PRÓXIMO PASO: crear estructura y archivos del ops repo** — usuario aprobó el plan, listo para ejecutar
- [ ] Subir ops repo a GitHub como público
- [ ] Terraform (AKS + ACR + namespaces)
- [ ] K8s manifests (infrastructure + services)
- [ ] Jenkinsfiles (dev/stage/master)
- [ ] docker-compose.yml de Jenkins en ops repo
- [ ] Dockerfiles en dev repo (6 servicios)
- [ ] Tests nuevos en dev repo
- [ ] Jenkinsfile ligero en dev repo

## Ubicación del ops repo en disco
`/Users/juanpabloparra/OctavoSemestre/IngeneriaSoftwareV/Taller2-Mio/circle-guard-ops/`
(al mismo nivel que circle-guard-public)

## 6 servicios seleccionados
Cadena de comunicación completa:
- Login → Auth (8180) → Identity (8083) → JWT emitido
- Formulario → Form (8086) → Kafka → Promotion (8088) → Redis + Kafka → Notification (8082)
- Entrada campus → Gateway (8087) → Redis (status check)

## Estructura ops repo acordada y aprobada
```
circle-guard-ops/
├── .gitignore               ← ignora terraform.tfvars, .terraform/, *.tfstate
├── terraform/
│   ├── main.tf              ← AKS cluster
│   ├── acr.tf               ← Azure Container Registry
│   ├── namespaces.tf        ← namespaces dev/stage/prod en K8s
│   ├── variables.tf
│   ├── outputs.tf
│   └── terraform.tfvars.example
├── k8s/
│   ├── infrastructure/
│   │   ├── postgres.yaml
│   │   ├── neo4j.yaml
│   │   ├── redis.yaml
│   │   └── kafka.yaml       ← incluye zookeeper
│   └── services/
│       ├── auth-service.yaml
│       ├── identity-service.yaml
│       ├── promotion-service.yaml
│       ├── notification-service.yaml
│       ├── form-service.yaml
│       └── gateway-service.yaml
└── jenkins/
    ├── docker-compose.yml   ← Jenkins corre aquí localmente
    ├── Jenkinsfile.dev      ← build + unit tests + deploy a circleguard-dev
    ├── Jenkinsfile.stage    ← build + unit + integration + deploy a circleguard-stage
    └── Jenkinsfile.master   ← pipeline completo + Release Notes + deploy a circleguard-prod
```

## Flujo de comunicación entre repos
```
Dev repo push
  → Jenkinsfile ligero (build + test + docker push a ACR con tag BUILD_NUMBER)
  → commit en ops repo actualizando imagen tag en k8s/services/*.yaml
    → dispara Ops Jenkinsfile
      → kubectl apply en AKS namespace correspondiente
```

## Pruebas planificadas para el dev repo

### 5 Unitarias nuevas (en servicios, compilables sin Docker)
1. `JwtTokenServiceTest` — auth: token tiene subject=anonymousId y claims de permisos
2. `IdentityVaultServiceUnitTest` — identity: misma identidad → mismo anonymousId (idempotencia)
3. `SymptomMapperEdgeCaseTest` — form: cough/breathing/null/lista vacía
4. `GatewayStatusMappingTest` — gateway: CONTAGIED→RED, null→GREEN, token inválido→RED
5. `NotificationPushContentTest` — notification: push content por estado, metadata URL

### 5 Integración nuevas
1. `QrTokenControllerIntegrationTest` — auth: GET /api/v1/auth/qr/generate con JWT → qrToken
2. `IdentityVaultServiceIntegrationTest` — identity: service+repository con H2
3. `HealthSurveyKafkaIntegrationTest` — form: submit → mensaje en topic survey.submitted (EmbeddedKafka)
4. `GateControllerIntegrationTest` — gateway: POST /api/v1/gate/validate con Redis mockeado
5. `ExposureNotificationListenerIntegrationTest` — notification: Kafka msg → dispatcher.dispatch() invocado

### 5 E2E (Python/pytest, contra servicios corriendo)
1. `test_enrollment_e2e.py` — POST a Identity, obtener anonymousId
2. `test_login_qr_e2e.py` — Login → JWT → generar QR token
3. `test_campus_entry_e2e.py` — QR token → validar en Gateway
4. `test_health_survey_e2e.py` — enviar formulario de salud
5. `test_status_cascade_e2e.py` — actualizar estado en Promotion → verificar Redis

### Locust (rendimiento)
Escenarios concurrentes: login, QR generation, gate validation, form submission, status check

## Cambios necesarios en dev repo
- Agregar `spring-kafka-test` a notification-service/build.gradle.kts
- Crear `src/test/resources/application.yml` en form-service, gateway-service, notification-service
- 6 Dockerfiles (uno por servicio, multi-stage Gradle + JRE Alpine)
- Jenkinsfile ligero en raíz del dev repo

## Why: separación ops/dev
GitOps: cambios de infra independientes del código. El profesor lo pidió explícitamente ("repositorio de ops").

## How to apply
Al iniciar nueva sesión: leer este archivo, verificar estado actual con `ls` en ambos repos, continuar desde donde se quedó. El próximo paso inmediato es crear los archivos del ops repo (usuario ya aprobó el plan completo).
