"""
CircleGuard — Pruebas de rendimiento y estrés con Locust
=========================================================

Simula tres tipos de usuarios reales del sistema:

  StudentUser        (peso 5) — estudiante: login, generación QR, encuesta diaria
  GateStaffUser      (peso 2) — guardia de entrada: valida QRs a alta frecuencia
  HealthSurveyBatch  (peso 3) — carga masiva de encuestas (stress de form + PostgreSQL)

Prerequisitos:
  pip install locust

Ejecución con UI:
  locust -f tests/performance/locustfile.py

Ejecución headless (50 usuarios, rampa de 5/s, 90 segundos):
  locust -f tests/performance/locustfile.py \\
    --headless -u 50 -r 5 --run-time 90s \\
    --csv tests/performance/results

URLs configurables por variable de entorno:
  AUTH_URL     (default: http://localhost:8180)
  FORM_URL     (default: http://localhost:8086)
  GATEWAY_URL  (default: http://localhost:8087)

Nota sobre el QR token:
  La configuración de dev usa qr.expiration=300 (300 ms). Por eso GateStaffUser
  regenera el token en cada tarea de validación para garantizar que siempre sea fresco.
"""

import os
import random
import uuid

from locust import HttpUser, between, events, task

# ─── Service base URLs ────────────────────────────────────────────────────────
AUTH_URL    = os.getenv("AUTH_URL",    "http://localhost:8180")
FORM_URL    = os.getenv("FORM_URL",    "http://localhost:8086")
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8087")

# Seeded test users (V2__seed_test_users.sql + V3__fix_user_passwords.sql)
_TEST_USERS = [
    {"username": "super_admin", "password": "password"},
    {"username": "staff_guard", "password": "password"},
    {"username": "health_user", "password": "password"},
]


def _pick_user() -> dict:
    return random.choice(_TEST_USERS)


# ─── Helper: login and return (jwt, anonymous_id) ────────────────────────────
def _login(client, creds: dict) -> tuple[str | None, str | None]:
    with client.post(
        AUTH_URL + "/api/v1/auth/login",
        json=creds,
        catch_response=True,
        name="auth: POST /login",
    ) as resp:
        if resp.status_code == 200:
            body = resp.json()
            resp.success()
            return body.get("token"), body.get("anonymousId")
        resp.failure(f"login {resp.status_code}")
        return None, None


def _generate_qr(client, jwt: str) -> str | None:
    with client.get(
        AUTH_URL + "/api/v1/auth/qr/generate",
        headers={"Authorization": f"Bearer {jwt}"},
        catch_response=True,
        name="auth: GET /qr/generate",
    ) as resp:
        if resp.status_code == 200:
            resp.success()
            return resp.json().get("qrToken")
        resp.failure(f"qr/generate {resp.status_code}")
        return None


# ═════════════════════════════════════════════════════════════════════════════
# TIPO 1 — Estudiante universitario
# Caso de uso: abre la app → recibe JWT → genera QR → envía encuesta diaria
# weight=5 porque es el usuario más frecuente
# ═════════════════════════════════════════════════════════════════════════════
class StudentUser(HttpUser):
    """
    Simula un estudiante que usa CircleGuard diariamente:
      - Se autentica al abrir la app (login con credenciales locales)
      - Genera un QR token para presentar en la entrada del campus
      - Envía su encuesta de salud diaria al Form Service

    Distribución de tareas:
      60% QR generation  (acción más frecuente — rotación cada ~60s)
      30% health survey  (acción diaria por estudiante)
      10% re-login       (simula reabrir la app / expiración de sesión)
    """

    host = AUTH_URL
    wait_time = between(1, 4)

    _jwt: str | None = None
    _anonymous_id: str | None = None

    def on_start(self):
        self._jwt, self._anonymous_id = _login(self.client, _pick_user())

    @task(6)
    def generate_qr_token(self):
        """Generar QR de entrada al campus — acción más frecuente del estudiante."""
        if not self._jwt:
            self._jwt, self._anonymous_id = _login(self.client, _pick_user())
            return
        qr = _generate_qr(self.client, self._jwt)
        if qr is None:
            # Token posiblemente expirado; forzar re-login en la próxima vuelta
            self._jwt = None

    @task(3)
    def submit_health_survey(self):
        """Enviar encuesta de salud diaria — flujo Form Service → Kafka."""
        if not self._anonymous_id:
            return
        with self.client.post(
            FORM_URL + "/api/v1/surveys",
            json={
                "anonymousId": self._anonymous_id,
                "hasFever":  random.choice([True, False, False, False]),  # 25% fiebre
                "hasCough":  random.choice([True, False, False, False]),  # 25% tos
            },
            catch_response=True,
            name="form: POST /surveys",
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"survey {resp.status_code}: {resp.text[:80]}")

    @task(1)
    def re_login(self):
        """Reautenticación periódica (simula reabrir la app o token expirado)."""
        self._jwt, self._anonymous_id = _login(self.client, _pick_user())


# ═════════════════════════════════════════════════════════════════════════════
# TIPO 2 — Guardia de seguridad en la entrada del campus
# Caso de uso: escanea QRs de estudiantes a alta frecuencia (hot path del sistema)
# weight=2
# ═════════════════════════════════════════════════════════════════════════════
class GateStaffUser(HttpUser):
    """
    Simula al personal de seguridad que valida QR codes en la entrada del campus.

    Es el escenario de mayor carga puntual: en hora pico (entrada a clases),
    el gateway recibe decenas de validaciones por minuto.

    Porque el QR expira en 300 ms (config de dev), la tarea de validación
    válida hace el ciclo completo login → QR → validate en cada ejecución,
    reflejando el rendimiento end-to-end del hot path.

    Distribución de tareas:
      70% validate_full_flow  (ciclo completo: login→QR→gate — mide E2E latency)
      30% validate_invalid    (tokens malformados — stress del path de denegación)
    """

    host = GATEWAY_URL
    wait_time = between(0.5, 2)

    @task(7)
    def validate_full_campus_entry_flow(self):
        """
        Flujo completo de entrada al campus:
          1. login  →  JWT + anonymousId
          2. /qr/generate con JWT  →  QR token
          3. /gate/validate con QR  →  resultado de acceso

        Este es el hot path crítico del sistema: la latencia total de este
        ciclo determina si la entrada al campus es fluida o genera colas.
        """
        creds = _pick_user()

        # Paso 1 — autenticar
        jwt, _ = _login(self.client, creds)
        if not jwt:
            return

        # Paso 2 — generar QR token
        qr_token = _generate_qr(self.client, jwt)
        if not qr_token:
            return

        # Paso 3 — validar en el gateway
        with self.client.post(
            "/api/v1/gate/validate",
            json={"token": qr_token},
            catch_response=True,
            name="gateway: POST /gate/validate (valid QR)",
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"gate/validate {resp.status_code}")

    @task(3)
    def validate_invalid_token(self):
        """
        Stress del path de denegación: tokens malformados o manipulados.
        El gateway debe responder rápido con {valid: false, status: RED}
        sin lanzar excepciones ni hacer llamadas a servicios externos.
        """
        fake_token = f"invalid.tampered.{uuid.uuid4().hex[:8]}"
        with self.client.post(
            "/api/v1/gate/validate",
            json={"token": fake_token},
            catch_response=True,
            name="gateway: POST /gate/validate (invalid QR)",
        ) as resp:
            if resp.status_code == 200 and not resp.json().get("valid"):
                resp.success()  # denegación limpia — comportamiento correcto
            else:
                resp.failure(f"expected denial, got {resp.status_code}: {resp.text[:80]}")


# ═════════════════════════════════════════════════════════════════════════════
# TIPO 3 — Carga masiva de encuestas (stress de Form Service + PostgreSQL)
# Caso de uso: todos los estudiantes envían su encuesta al inicio del día
# weight=3
# ═════════════════════════════════════════════════════════════════════════════
class HealthSurveyBatchUser(HttpUser):
    """
    Simula la carga de pico matutina: muchos estudiantes envían su encuesta
    de salud casi al mismo tiempo (al entrar al campus entre 7:00 y 9:00 am).

    Cada usuario tiene un anonymousId único generado al inicio para
    garantizar datos realistas en la base de datos.

    Prueba el límite de:
      - Throughput del Form Service (HTTP → JPA → PostgreSQL)
      - Publicación paralela de eventos Kafka (survey.submitted)
    """

    host = FORM_URL
    wait_time = between(0.5, 3)

    _anonymous_id: str = ""

    def on_start(self):
        # Cada usuario virtual representa un estudiante distinto
        self._anonymous_id = str(uuid.uuid4())

    @task
    def submit_daily_health_survey(self):
        """Envío de encuesta matutina — máxima concurrencia, datos únicos por usuario."""
        with self.client.post(
            "/api/v1/surveys",
            json={
                "anonymousId": self._anonymous_id,
                "hasFever":  False,
                "hasCough":  False,
                "otherSymptoms": None,
            },
            catch_response=True,
            name="form: POST /surveys (batch)",
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"survey batch {resp.status_code}: {resp.text[:80]}")


# ─── Event hooks para logging de inicio/fin de prueba ─────────────────────────
@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    print("\n[CircleGuard Locust] Iniciando prueba de rendimiento")
    print(f"  AUTH_URL    = {AUTH_URL}")
    print(f"  FORM_URL    = {FORM_URL}")
    print(f"  GATEWAY_URL = {GATEWAY_URL}\n")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    stats = environment.stats.total
    print("\n[CircleGuard Locust] Prueba finalizada")
    print(f"  Requests   : {stats.num_requests}")
    print(f"  Failures   : {stats.num_failures}")
    print(f"  Median (ms): {stats.median_response_time}")
    print(f"  95th % (ms): {stats.get_response_time_percentile(0.95)}")
    print(f"  RPS        : {stats.current_rps:.1f}\n")
