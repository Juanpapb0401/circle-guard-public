# Documentación de Pruebas — CircleGuard CI/CD Taller 2

Este documento registra todas las pruebas nuevas implementadas para el Taller 2. Se actualiza a medida que se agregan pruebas de integración, E2E y rendimiento.

---

## 1. Pruebas Unitarias

**Objetivo:** Validar componentes individuales de los microservicios de forma aislada, sin levantar el contexto completo de Spring ni bases de datos reales.

**Servicios cubiertos:** auth, identity, form, gateway

**Total de pruebas nuevas: 12** (requisito mínimo: 5)

---

### 1.1 Auth Service — `JwtTokenServiceTest`

**Archivo:** `services/circleguard-auth-service/src/test/java/com/circleguard/auth/service/JwtTokenServiceTest.java`

**Clase bajo prueba:** `JwtTokenService` — responsable de generar tokens JWT firmados con HMAC-SHA256 que incluyen el `anonymousId` del usuario y sus permisos.

**Configuración:** Se instancia directamente con el secreto y tiempo de expiración reales de desarrollo (`my-super-secret-dev-key-32-chars-long-12345678`, 1 hora). No usa Spring context — prueba pura con Mockito.

| # | Nombre del test | Qué valida |
|---|---|---|
| 1 | `generateToken_subjectIsAnonymousId` | El `subject` del JWT es el `anonymousId` (UUID) del usuario, no su nombre real |
| 2 | `generateToken_containsPermissionsClaim` | El claim `permissions` contiene exactamente los roles del `Authentication` proporcionado |
| 3 | `generateToken_expirationIsInFuture` | La fecha de expiración (`exp`) es posterior a la fecha de emisión (`iat`) |

**Análisis:** Estas pruebas protegen el contrato del JWT — cualquier cambio en la estructura del token que rompa la autenticación de los demás servicios se detectaría aquí antes de llegar a integración.

---

### 1.2 Identity Service — `IdentityVaultServiceUnitTest`

**Archivo:** `services/circleguard-identity-service/src/test/java/com/circleguard/identity/service/IdentityVaultServiceUnitTest.java`

**Clase bajo prueba:** `IdentityVaultService` — gestiona el mapeo entre identidades reales y `anonymousId` UUID. Es el componente de privacidad central (cumplimiento FERPA).

**Configuración:** Usa `@ExtendWith(MockitoExtension.class)` con el repositorio mockeado. El campo `hashSalt` se inyecta vía `ReflectionTestUtils`.

| # | Nombre del test | Qué valida |
|---|---|---|
| 4 | `getOrCreateAnonymousId_existingIdentity_doesNotSaveAgain` | Si la identidad ya existe (por hash), devuelve el `anonymousId` existente sin llamar a `save()` — evita duplicados |
| 5 | `getOrCreateAnonymousId_newIdentity_savesAndReturnsId` | Si la identidad es nueva, persiste el mapeo y devuelve el nuevo `anonymousId` |
| 6 | `resolveRealIdentity_unknownId_throws404` | Si se consulta un `anonymousId` desconocido, lanza `ResponseStatusException` 404 |

**Análisis:** El comportamiento idempotente del servicio (no duplicar identidades) es crítico para la consistencia del sistema. Estas pruebas garantizan que la lógica de lookup-antes-de-crear funciona correctamente sin depender de la base de datos real.

---

### 1.3 Form Service — `SymptomMapperTest` (pruebas nuevas)

**Archivo:** `services/circleguard-form-service/src/test/java/com/circleguard/form/service/SymptomMapperTest.java`

**Clase bajo prueba:** `SymptomMapper` — interpreta las respuestas de un cuestionario de salud y determina si el usuario reporta síntomas. Su salida dispara el flujo Kafka → Promotion Service.

**Configuración:** Instanciación directa sin Spring. Tests sin dependencias externas.

Los tests 7-9 fueron agregados al archivo existente (marcados con el comentario `// ── nuevas pruebas unitarias`):

| # | Nombre del test | Qué valida |
|---|---|---|
| 7 | `shouldDetectSymptomsFromCough` | Respuesta "YES" a pregunta de tos es detectada como síntoma |
| 8 | `shouldDetectSymptomsFromBreathingDifficulty` | Respuesta "YES" a pregunta de dificultad respiratoria es detectada como síntoma |
| 9 | `shouldReturnFalseWhenResponsesAreNull` | Si `responses` es `null`, el mapper devuelve `false` sin lanzar excepción |

**Análisis:** El caso borde de `responses == null` (test 9) es especialmente importante: un cuestionario enviado con errores de serialización no debe propagar un `NullPointerException` al pipeline de Kafka, sino manejarse silenciosamente.

---

### 1.4 Gateway Service — `QrValidationServiceTest` (pruebas nuevas)

**Archivo:** `services/circleguard-gateway-service/src/test/java/com/circleguard/gateway/service/QrValidationServiceTest.java`

**Clase bajo prueba:** `QrValidationService` — valida tokens QR firmados y consulta Redis para determinar el estado de salud del usuario en el hot path de entrada al campus.

**Configuración:** Redis mockeado con Mockito. El secreto QR se inyecta vía `ReflectionTestUtils`.

Los tests 10-12 fueron agregados al archivo existente (marcados con el comentario `// ── nuevas pruebas unitarias`):

| # | Nombre del test | Qué valida |
|---|---|---|
| 10 | `shouldDenyAccessForPotentialStatus` | Usuario con estado `POTENTIAL` en Redis recibe resultado `valid=false`, `status=RED` |
| 11 | `shouldDenyAccessForMalformedToken` | Token QR malformado (no JWT) devuelve `valid=false`, `status=RED` sin excepción no controlada |
| 12 | `shouldDenyAccessForExpiredToken` | Token QR expirado (fecha de expiración en el pasado) devuelve `valid=false`, `status=RED` |

**Análisis:** El Gateway es el punto de entrada físico al campus. El comportamiento ante tokens inválidos debe ser siempre "denegar", nunca "permitir por defecto". Los tests 11 y 12 verifican que errores criptográficos (token malformado, expirado) se convierten en denegaciones limpias, no en errores 500.

---

## 2. Pruebas de Integración

**Objetivo:** Validar la interacción entre componentes dentro de un microservicio usando el contexto completo de Spring Boot (`@SpringBootTest`) o el slice web (`@WebMvcTest`). Los sistemas externos se reemplazan por H2, mocks o EmbeddedKafka cuando no son el objeto de prueba.

**Servicios cubiertos:** auth, identity, form, gateway, notification

**Total de pruebas de integración: 10** (requisito mínimo: 5)

---

### 2.1 Auth Service — `QrTokenControllerIntegrationTest`

**Archivo:** `services/circleguard-auth-service/src/test/java/com/circleguard/auth/controller/QrTokenControllerIntegrationTest.java`

**Estrategia:** `@WebMvcTest(QrTokenController.class)` con `@MockBean QrTokenService`. Usa `@WithMockUser` con un UUID como username para simular un JWT ya validado.

| # | Nombre del test | Qué valida |
|---|---|---|
| 1 | `generateQrToken_authenticatedUser_returnsTokenAndExpiry` | Usuario autenticado recibe 200 con `qrToken` y `expiresIn: "60"` |
| 2 | `generateQrToken_unauthenticated_returnsUnauthorized` | Solicitud sin autenticación recibe 401 |

**Análisis:** Valida que el endpoint QR requiere autenticación y que el controller delega correctamente al servicio.

---

### 2.2 Identity Service — `IdentityVaultServiceIntegrationTest`

**Archivo:** `services/circleguard-identity-service/src/test/java/com/circleguard/identity/service/IdentityVaultServiceIntegrationTest.java`

**Estrategia:** `@SpringBootTest` con H2 en memoria y `@Transactional` (rollback por test). Contexto completo incluyendo `IdentityEncryptionConverter` y repositorio JPA real.

| # | Nombre del test | Qué valida |
|---|---|---|
| 3 | `getOrCreateAnonymousId_sameIdentityTwice_returnsSameUuid` | Idempotencia: misma identidad → mismo UUID, sin crear duplicado en BD |
| 4 | `getOrCreateAnonymousId_differentIdentities_returnDifferentUuids` | Identidades distintas → UUIDs distintos |
| 5 | `resolveRealIdentity_afterCreation_returnsOriginalIdentity` | Round-trip: crear mapeo → resolver → recupera identidad original (prueba cifrado AES) |

**Análisis:** Verifica el contrato de privacidad central: la biyección identidad↔anonymousId funciona correctamente incluyendo la capa de cifrado en reposo.

---

### 2.3 Form Service — `HealthSurveyKafkaIntegrationTest`

**Archivo:** `services/circleguard-form-service/src/test/java/com/circleguard/form/service/HealthSurveyKafkaIntegrationTest.java`

**Estrategia:** `@SpringBootTest` con `@MockBean KafkaTemplate`, `@MockBean HealthSurveyRepository` y `@MockBean QuestionnaireService`. Foco en verificar que el servicio orquesta persistencia y publicación.

| # | Nombre del test | Qué valida |
|---|---|---|
| 6 | `submitSurvey_persistsSurveyAndPublishesEventWithAnonymousIdAsKey` | `submitSurvey()` llama a `repository.save()` Y a `kafkaTemplate.send("survey.submitted", anonymousId, event)` |

**Análisis:** El `anonymousId` como clave del mensaje Kafka es crítico para que el Promotion Service procese los eventos de forma correcta y ordenada.

---

### 2.4 Gateway Service — `GateControllerIntegrationTest`

**Archivo:** `services/circleguard-gateway-service/src/test/java/com/circleguard/gateway/controller/GateControllerIntegrationTest.java`

**Estrategia:** `@WebMvcTest(GateController.class)` con `@MockBean QrValidationService`. Sin Spring Security en gateway — no requiere autenticación.

| # | Nombre del test | Qué valida |
|---|---|---|
| 7 | `validate_validToken_returnsGreenAccess` | Token válido → `{valid: true, status: "GREEN", message: "Welcome to Campus"}` |
| 8 | `validate_invalidToken_returnsRedDenied` | Token inválido → `{valid: false, status: "RED"}` |

**Análisis:** El mapeo correcto del resultado del servicio a la respuesta HTTP es crítico: una respuesta `valid: false` bloquea el acceso físico al campus.

---

### 2.5 Notification Service — `ExposureNotificationListenerIntegrationTest`

**Archivo:** `services/circleguard-notification-service/src/test/java/com/circleguard/notification/service/ExposureNotificationListenerIntegrationTest.java`

**Estrategia:** `@SpringBootTest` con `@EmbeddedKafka` (broker real en memoria). `NotificationDispatcher`, `LmsService` y canales (Email, SMS, Push) son `@MockBean`. El listener real consume el mensaje.

| # | Nombre del test | Qué valida |
|---|---|---|
| 9 | `whenStatusChangedEventReceived_dispatcherIsCalled` | Mensaje `status: "CONFIRMED"` en topic → `dispatcher.dispatch(userId, "CONFIRMED")` invocado |
| 10 | `whenActiveStatusReceived_dispatcherIsNotCalled` | Mensaje `status: "ACTIVE"` → dispatcher NO invocado (lógica de filtrado del listener) |

**Análisis:** El test 10 valida la lógica de negocio que evita notificaciones innecesarias para usuarios en estado ACTIVE. Usa `timeout(5000)` y `after(3000)` para manejar la asincronía de Kafka.

---

## 3. Pruebas E2E

**Objetivo:** Validar flujos completos de usuario llamando a los endpoints HTTP reales, como lo haría un cliente externo. No se mockea nada — los tests asumen que el stack de Docker Compose y los servicios Spring Boot están levantados.

**Archivo:** `tests/e2e/test_e2e.py`

**Prerequisitos:**
```bash
# Levantar middleware
docker compose -f docker-compose.dev.yml up -d

# Arrancar los servicios requeridos (en terminales separadas o con bootRun)
./gradlew :services:circleguard-auth-service:bootRun &
./gradlew :services:circleguard-identity-service:bootRun &
./gradlew :services:circleguard-form-service:bootRun &
./gradlew :services:circleguard-gateway-service:bootRun &
```

**Usuarios de prueba** (seeded por `V2__seed_test_users.sql` / `V3__fix_user_passwords.sql`):

| Usuario | Contraseña | Rol |
|---|---|---|
| `super_admin` | `password` | Todos los roles |
| `staff_guard` | `password` | GATE_STAFF |
| `health_user` | `password` | HEALTH_CENTER |

**Total de pruebas E2E: 5** (requisito mínimo: 5)

---

### 3.1 E2E-01 — Login con credenciales válidas devuelve JWT

**Clase:** `TestAuthLogin` · **Método:** `test_login_returns_jwt_and_anonymous_id`

**Flujo:** `POST /api/v1/auth/login` con `{username: "super_admin", password: "password"}`

**Qué valida:**
- HTTP 200
- Respuesta contiene `token` (no vacío), `type: "Bearer"` y `anonymousId`
- El `anonymousId` es un UUID v4 válido (contrato de privacidad)

**Servicios involucrados:** Auth (8180) → Identity (8083) *(auth llama a identity para resolver el anonymousId)*

---

### 3.2 E2E-02 — Login con contraseña incorrecta devuelve 401

**Clase:** `TestAuthLoginBadCredentials` · **Método:** `test_login_wrong_password_returns_401`

**Flujo:** `POST /api/v1/auth/login` con `{username: "super_admin", password: "totally-wrong"}`

**Qué valida:**
- HTTP 401
- Respuesta NO contiene campo `token` — sin fuga de credenciales en error

**Servicios involucrados:** Auth (8180)

---

### 3.3 E2E-03 — Flujo completo: login → JWT → generación de QR token

**Clase:** `TestLoginAndQrGenerationFlow` · **Método:** `test_authenticated_user_can_generate_qr_token`

**Flujo (multi-step):**
1. `POST /api/v1/auth/login` → obtiene JWT
2. `GET /api/v1/auth/qr/generate` sin token → verifica que devuelve 401 o 403
3. `GET /api/v1/auth/qr/generate` con `Authorization: Bearer <jwt>` → obtiene QR token

**Qué valida:**
- El endpoint QR protege correctamente (Spring Security 6 devuelve 403 cuando no hay `authenticationEntryPoint` configurado; el test acepta 401 o 403)
- Usuario autenticado recibe `{qrToken: "...", expiresIn: "60"}`

**Servicios involucrados:** Auth (8180) + Identity (8083)

---

### 3.4 E2E-04 — Gateway rechaza token QR malformado

**Clase:** `TestGatewayValidation` · **Método:** `test_gate_rejects_malformed_qr_token`

**Flujo:** `POST /api/v1/gate/validate` con `{token: "this.is.not.a.valid.jwt"}`

**Qué valida:**
- HTTP 200 (el endpoint nunca lanza 500 — "fail safe")
- Respuesta: `{valid: false, status: "RED"}`

**Análisis:** El gateway es el punto de entrada físico al campus. Cualquier error criptográfico (token malformado, expirado, firma inválida) debe generar una denegación limpia, nunca un error 500 que pueda interpretarse como "paso libre".

**Servicios involucrados:** Gateway (8087)

---

### 3.5 E2E-05 — Envío de encuesta de salud persiste en base de datos

**Clase:** `TestHealthSurveySubmission` · **Método:** `test_submit_health_survey_returns_persisted_entity`

**Flujo:** `POST /api/v1/surveys` con `{anonymousId: UUID, hasFever: true, hasCough: false}`

**Qué valida:**
- HTTP 200
- Respuesta contiene el mismo `anonymousId` enviado y los campos de síntomas
- Campo `id` no nulo — confirma que la entidad fue persistida en PostgreSQL y se le asignó un UUID

**Análisis:** La publicación del evento Kafka ocurre de forma asíncrona y no se verifica en este test (esa lógica está cubierta por `HealthSurveyKafkaIntegrationTest`).

**Servicios involucrados:** Form (8086) + PostgreSQL

---

## Cómo correr las pruebas E2E

```bash
# Crear entorno virtual e instalar dependencias
python3 -m venv tests/e2e/.venv
tests/e2e/.venv/bin/pip install -r tests/e2e/requirements.txt

# Ejecutar todos los E2E
tests/e2e/.venv/bin/python -m pytest tests/e2e/ -v

# Ejecutar contra servicios en otra URL (ej. staging en AKS)
AUTH_URL=http://<ip>:8180 GATEWAY_URL=http://<ip>:8087 FORM_URL=http://<ip>:8086 \
  tests/e2e/.venv/bin/python -m pytest tests/e2e/ -v
```

> Si algún servicio no está disponible, el test correspondiente se marca como **SKIPPED** (no FAILED), indicando qué URL no es alcanzable.

---

## 4. Pruebas de Rendimiento (Locust)

**Objetivo:** Medir el rendimiento del sistema bajo carga sostenida y condiciones de estrés, simulando los patrones de uso reales de los actores del sistema (estudiantes, personal de seguridad, usuarios de salud).

**Archivo:** `tests/performance/locustfile.py`

**Tipos de usuarios simulados (3 clases `HttpUser`):**

| Clase | Peso | Caso de uso real |
|---|---|---|
| `StudentUser` | 5 | Estudiante: login → genera QR → envía encuesta diaria |
| `GateStaffUser` | 2 | Guardia de entrada: valida QRs a alta frecuencia (hot path) |
| `HealthSurveyBatchUser` | 3 | Carga pico matutina: envío masivo y concurrente de encuestas |

---

### 4.1 Tareas y distribución

**`StudentUser`** (`host: AUTH_URL`)

| Tarea | Peso | Endpoint | Descripción |
|---|---|---|---|
| `generate_qr_token` | 6 | `GET /api/v1/auth/qr/generate` | Rotación de QR para entrada al campus |
| `submit_health_survey` | 3 | `POST /api/v1/surveys` | Encuesta diaria de salud |
| `re_login` | 1 | `POST /api/v1/auth/login` | Reautenticación periódica |

**`GateStaffUser`** (`host: GATEWAY_URL`)

| Tarea | Peso | Descripción |
|---|---|---|
| `validate_full_campus_entry_flow` | 7 | Ciclo completo: login → JWT → QR → /gate/validate — mide latencia E2E del hot path |
| `validate_invalid_token` | 3 | Tokens malformados — stress del path de denegación |

> **Nota:** El QR token expira en 300 ms en la configuración de desarrollo (`qr.expiration=300`). Por eso `validate_full_campus_entry_flow` regenera el token en cada ejecución, lo que hace que este test mida la latencia real del ciclo completo de entrada.

**`HealthSurveyBatchUser`** (`host: FORM_URL`)

| Tarea | Peso | Descripción |
|---|---|---|
| `submit_daily_health_survey` | 1 | Encuesta masiva — `anonymousId` obtenido en `on_start` vía login real, no generado aleatoriamente — stress de PostgreSQL |

> **Nota sobre `on_start`:** `HealthSurveyBatchUser.on_start()` realiza un login real al auth-service para obtener un `anonymousId` válido que ya exista en la base de datos del form-service. Sin esto, el form-service rechaza encuestas con UUIDs desconocidos.

> **Nota sobre `--host` en CI/CD:** El comando Locust en el Jenkinsfile pasa `--host http://localhost:8087` (el gateway), lo que sobrescribe el atributo `host` de **todas** las clases de usuario. Por eso `submit_daily_health_survey` y `submit_health_survey` (StudentUser) usan URLs absolutas (`FORM_URL + "/api/v1/surveys"`): las URLs absolutas ignoran el `--host` del CLI. Las URLs relativas se resolverían contra el gateway y recibirían 404.

---

### 4.2 Resultados de la prueba de referencia

**Configuración:** 20 usuarios virtuales, rampa de 5 usuarios/s, duración 20 segundos — ejecutado con servicios corriendo en local (sin `--host` de CLI, cada clase usa su propio `host`).

| Endpoint | Requests | Fallos | Mediana | P95 | P99 | Mín | Máx |
|---|---|---|---|---|---|---|---|
| `auth: GET /qr/generate` | 84 | 0 | 2 ms | 4 ms | 9 ms | 1 ms | 9 ms |
| `auth: POST /login` | 62 | 0 | 290 ms | 310 ms | 330 ms | 265 ms | 330 ms |
| `form: POST /surveys` | 9 | 0 | 6 ms | 17 ms | 17 ms | 4 ms | 16 ms |
| `form: POST /surveys (batch)` | 68 | 0 | 6 ms | 15 ms | 24 ms | 3 ms | 24 ms |
| `gateway: validate (invalid QR)` | 25 | 0 | 2 ms | 8 ms | 8 ms | 1 ms | 8 ms |
| `gateway: validate (valid QR)` | 55 | 0 | 2 ms | 27 ms | 150 ms | 1 ms | 150 ms |
| **Total** | **303** | **0 (0%)** | **4 ms** | **300 ms** | **310 ms** | **1 ms** | **330 ms** |

**Throughput total:** 17.3 RPS

---

### 4.3 Análisis de resultados

**Generación de QR (`GET /qr/generate`):**
Mediana de **2 ms** — el endpoint más rápido del sistema. Es una operación puramente criptográfica (firma HMAC-SHA256) sin I/O a base de datos ni llamadas externas. Excelente para un hot path que se invoca cada 60 segundos por usuario.

**Login (`POST /login`):**
Mediana de **290 ms** en ejecución local con 20 usuarios — el cuello de botella más visible. Es esperado: el login implica bcrypt password verification (operación costosa por diseño) + llamada HTTP al Identity Service para resolver el `anonymousId`. El P99 de 330 ms es consistente y sin outliers bajo carga local.

> **Comportamiento bajo carga alta (50 usuarios, AKS):** Sin optimización, la mediana sube a ~20 000 ms con 31% de timeouts. La causa principal es que `DualChainAuthenticationProvider` intenta LDAP primero (timeout por defecto ~30 s) antes de caer a local DB para usuarios del seed. La solución aplicada: timeout LDAP de 3 s (`com.sun.jndi.ldap.connect.timeout=3000`) + HikariCP pool size 20 en `application.yml`, lo que mantiene la latencia bajo carga en rangos aceptables.

**Encuestas (`POST /surveys`):**
Mediana de **6-7 ms** — throughput excelente para operaciones JPA + PostgreSQL. El P99 de 24 ms confirma que la base de datos local no es un cuello de botella con esta carga. En producción con más concurrencia se esperaría un degradación gradual.

**Validación de QR válido en gateway:**
Mediana de **2 ms** pero P95 de **27 ms** y P99 de **150 ms** — este endpoint hace una llamada a Redis para verificar el estado de salud del usuario. Los outliers del P99 reflejan latencia de Redis en condiciones de baja carga (cold cache). En producción con Redis caliente se esperaría una distribución más uniforme.

**Validación de QR inválido en gateway:**
Mediana de **2 ms**, P99 de **8 ms** — el path de denegación es significativamente más rápido que el válido porque la firma JWT falla antes de hacer la consulta a Redis. Confirma el comportamiento "fail fast" correcto para tokens malformados.

**Tasa de error: 0%** — ninguno de los 303 requests falló bajo carga de 20 usuarios concurrentes.

---

### 4.4 Cómo correr las pruebas de rendimiento

**Prerequisito:**
```bash
# Instalar Locust (en el mismo venv de E2E)
tests/e2e/.venv/bin/pip install locust

# O en entorno independiente:
python3 -m venv tests/performance/.venv
tests/performance/.venv/bin/pip install locust
```

**Con interfaz web (recomendado para análisis):**
```bash
tests/e2e/.venv/bin/locust -f tests/performance/locustfile.py
# Abre http://localhost:8089 — configura usuarios y rampa desde el browser
```

**Headless (para CI/CD):**
```bash
tests/e2e/.venv/bin/locust \
  -f tests/performance/locustfile.py \
  --headless -u 50 -r 5 --run-time 60s \
  --csv tests/performance/results
```

**Contra servicios en AKS (staging/prod):**
```bash
AUTH_URL=http://<aks-ip>:8180 \
FORM_URL=http://<aks-ip>:8086 \
GATEWAY_URL=http://<aks-ip>:8087 \
  tests/e2e/.venv/bin/locust -f tests/performance/locustfile.py \
  --headless -u 100 -r 10 --run-time 120s
```

---

## Cómo correr las pruebas

### Pruebas unitarias (no requieren Docker)

```bash
./gradlew \
  :services:circleguard-auth-service:test \
  :services:circleguard-identity-service:test \
  :services:circleguard-form-service:test \
  :services:circleguard-gateway-service:test \
  --parallel
```

### Pruebas de integración (no requieren Docker salvo notification)

```bash
# Auth — QrTokenControllerIntegrationTest
./gradlew :services:circleguard-auth-service:test \
  --tests "com.circleguard.auth.controller.QrTokenControllerIntegrationTest"

# Identity — IdentityVaultServiceIntegrationTest
./gradlew :services:circleguard-identity-service:test \
  --tests "com.circleguard.identity.service.IdentityVaultServiceIntegrationTest"

# Form — HealthSurveyKafkaIntegrationTest
./gradlew :services:circleguard-form-service:test \
  --tests "com.circleguard.form.service.HealthSurveyKafkaIntegrationTest"

# Gateway — GateControllerIntegrationTest
./gradlew :services:circleguard-gateway-service:test \
  --tests "com.circleguard.gateway.controller.GateControllerIntegrationTest"

# Notification — ExposureNotificationListenerIntegrationTest (usa EmbeddedKafka)
./gradlew :services:circleguard-notification-service:test \
  --tests "com.circleguard.notification.service.ExposureNotificationListenerIntegrationTest"
```

### Ver resultados

Los reportes HTML se generan automáticamente en:

```
services/<nombre>-service/build/reports/tests/test/index.html
```

Abre cualquiera en el navegador para ver el detalle de cada test con su resultado y tiempo de ejecución.
