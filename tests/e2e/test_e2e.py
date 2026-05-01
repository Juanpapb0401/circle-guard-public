"""
End-to-end tests for CircleGuard — Taller 2.

Test users seeded by V2__seed_test_users.sql / V3__fix_user_passwords.sql:
  super_admin / password  (all roles)
  staff_guard / password  (GATE_STAFF)
  health_user / password  (HEALTH_CENTER)
"""
import uuid
import pytest
import requests

TIMEOUT = 10  # seconds per HTTP call


# ─────────────────────────────────────────────────────────────────────────────
# E2E-01  Auth service — login with valid credentials
# ─────────────────────────────────────────────────────────────────────────────
class TestAuthLogin:
    def test_login_returns_jwt_and_anonymous_id(self, auth_base):
        """
        POST /api/v1/auth/login with correct credentials must return:
          - HTTP 200
          - JSON body with 'token' (non-empty), 'type': 'Bearer', and 'anonymousId' (valid UUID)

        Covers the full Auth → Identity chain: auth service issues JWT only after
        resolving (or creating) an anonymousId in the identity service.
        """
        response = requests.post(
            f"{auth_base}/api/v1/auth/login",
            json={"username": "super_admin", "password": "password"},
            timeout=TIMEOUT,
        )

        assert response.status_code == 200, (
            f"Expected 200 but got {response.status_code}: {response.text}"
        )

        body = response.json()
        assert body.get("type") == "Bearer", "JWT type must be 'Bearer'"
        assert body.get("token"), "Response must include a non-empty 'token'"
        assert body.get("anonymousId"), "Response must include 'anonymousId'"

        # anonymousId must be a valid UUID (privacy contract)
        parsed = uuid.UUID(body["anonymousId"])
        assert parsed.version == 4, "anonymousId must be a UUID v4"


# ─────────────────────────────────────────────────────────────────────────────
# E2E-02  Auth service — login with wrong password
# ─────────────────────────────────────────────────────────────────────────────
class TestAuthLoginBadCredentials:
    def test_login_wrong_password_returns_401(self, auth_base):
        """
        POST /api/v1/auth/login with an incorrect password must return HTTP 401
        and must NOT return a token — ensures no credential leak on failure.
        """
        response = requests.post(
            f"{auth_base}/api/v1/auth/login",
            json={"username": "super_admin", "password": "totally-wrong"},
            timeout=TIMEOUT,
        )

        assert response.status_code == 401, (
            f"Expected 401 but got {response.status_code}: {response.text}"
        )

        body = response.json()
        assert "token" not in body, "Response must not contain a token on failed login"


# ─────────────────────────────────────────────────────────────────────────────
# E2E-03  Multi-step flow: login → JWT → QR token generation
# ─────────────────────────────────────────────────────────────────────────────
class TestLoginAndQrGenerationFlow:
    def test_authenticated_user_can_generate_qr_token(self, auth_base):
        """
        Full multi-step flow:
          1. POST /api/v1/auth/login  →  JWT
          2. GET  /api/v1/auth/qr/generate (Bearer token)  →  qrToken + expiresIn

        Validates end-to-end: auth service issues JWT, the QR endpoint enforces
        authentication, and the QR token service signs a short-lived campus-entry token.
        Also asserts that the same endpoint returns 401 without a token, confirming
        the security boundary is enforced.
        """
        # Step 1 — authenticate
        login_resp = requests.post(
            f"{auth_base}/api/v1/auth/login",
            json={"username": "staff_guard", "password": "password"},
            timeout=TIMEOUT,
        )
        assert login_resp.status_code == 200, (
            f"Login step failed ({login_resp.status_code}): {login_resp.text}"
        )
        jwt = login_resp.json()["token"]

        # Step 2a — unauthenticated call must be rejected
        unauth_resp = requests.get(
            f"{auth_base}/api/v1/auth/qr/generate",
            timeout=TIMEOUT,
        )
        # Spring Security 6 returns 403 (Forbidden) when no authenticationEntryPoint
        # is configured — both 401 and 403 indicate the endpoint is protected.
        assert unauth_resp.status_code in (401, 403), (
            f"QR endpoint must require authentication, got {unauth_resp.status_code}"
        )

        # Step 2b — authenticated call must succeed
        qr_resp = requests.get(
            f"{auth_base}/api/v1/auth/qr/generate",
            headers={"Authorization": f"Bearer {jwt}"},
            timeout=TIMEOUT,
        )
        assert qr_resp.status_code == 200, (
            f"QR generation failed ({qr_resp.status_code}): {qr_resp.text}"
        )

        body = qr_resp.json()
        assert body.get("qrToken"), "Response must contain a non-empty 'qrToken'"
        assert body.get("expiresIn") == "60", (
            f"Expected expiresIn='60', got '{body.get('expiresIn')}'"
        )


# ─────────────────────────────────────────────────────────────────────────────
# E2E-04  Gateway service — malformed QR token is denied
# ─────────────────────────────────────────────────────────────────────────────
class TestGatewayValidation:
    def test_gate_rejects_malformed_qr_token(self, gateway_base):
        """
        POST /api/v1/gate/validate with a non-JWT string must return:
          - HTTP 200 (the endpoint always responds, never throws 500)
          - JSON body: {valid: false, status: 'RED'}

        The gateway is the physical campus entry point. Any cryptographic error
        (malformed token, bad signature, expiry) must result in a clean denial,
        never in an uncontrolled exception that could let someone through.
        """
        response = requests.post(
            f"{gateway_base}/api/v1/gate/validate",
            json={"token": "this.is.not.a.valid.jwt"},
            timeout=TIMEOUT,
        )

        assert response.status_code == 200, (
            f"Gateway must always return 200; got {response.status_code}"
        )

        body = response.json()
        assert body.get("valid") is False, "Malformed token must yield valid=false"
        assert body.get("status") == "RED", "Denied entry must have status='RED'"


# ─────────────────────────────────────────────────────────────────────────────
# E2E-05  Form service — health survey submission
# ─────────────────────────────────────────────────────────────────────────────
class TestHealthSurveySubmission:
    def test_submit_health_survey_returns_persisted_entity(self, form_base):
        """
        POST /api/v1/surveys with a valid payload must return:
          - HTTP 200
          - JSON body echoing the anonymousId and symptom fields

        Verifies the full form-service stack: HTTP → service layer → PostgreSQL persistence.
        The Kafka event is published asynchronously and is not asserted here.
        """
        anonymous_id = str(uuid.uuid4())
        payload = {
            "anonymousId": anonymous_id,
            "hasFever": True,
            "hasCough": False,
        }

        response = requests.post(
            f"{form_base}/api/v1/surveys",
            json=payload,
            timeout=TIMEOUT,
        )

        assert response.status_code == 200, (
            f"Survey submission failed ({response.status_code}): {response.text}"
        )

        body = response.json()
        assert body.get("anonymousId") == anonymous_id, (
            "Returned anonymousId must match the submitted one"
        )
        assert body.get("hasFever") is True
        assert body.get("hasCough") is False
        assert body.get("id") is not None, (
            "Persisted entity must have a database-assigned UUID id"
        )
