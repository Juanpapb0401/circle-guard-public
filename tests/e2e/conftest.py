"""
E2E test configuration for CircleGuard.

Run against the full dev stack:
  docker compose -f docker-compose.dev.yml up -d
  # then start auth, identity, form and gateway services (bootRun or from IDE)

Environment overrides:
  AUTH_URL     default http://localhost:8180
  FORM_URL     default http://localhost:8086
  GATEWAY_URL  default http://localhost:8087
"""
import os
import pytest
import requests

AUTH_URL    = os.getenv("AUTH_URL",    "http://localhost:8180")
FORM_URL    = os.getenv("FORM_URL",    "http://localhost:8086")
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8087")

_CONNECT_TIMEOUT = 3  # seconds — fail fast if service is unreachable


def _is_reachable(url: str) -> bool:
    try:
        requests.post(url, json={}, timeout=_CONNECT_TIMEOUT)
        return True
    except requests.exceptions.ConnectionError:
        return False
    except requests.exceptions.Timeout:
        return False


@pytest.fixture(scope="session")
def auth_base():
    if not _is_reachable(f"{AUTH_URL}/api/v1/auth/login"):
        pytest.skip(f"Auth service not reachable at {AUTH_URL}")
    return AUTH_URL


@pytest.fixture(scope="session")
def gateway_base():
    if not _is_reachable(f"{GATEWAY_URL}/api/v1/gate/validate"):
        pytest.skip(f"Gateway service not reachable at {GATEWAY_URL}")
    return GATEWAY_URL


@pytest.fixture(scope="session")
def form_base():
    if not _is_reachable(f"{FORM_URL}/api/v1/surveys"):
        pytest.skip(f"Form service not reachable at {FORM_URL}")
    return FORM_URL
