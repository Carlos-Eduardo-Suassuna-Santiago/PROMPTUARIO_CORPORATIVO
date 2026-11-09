import asyncio
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import httpx
import pytest
from fastapi import HTTPException
from fastapi.responses import JSONResponse
from fastapi.testclient import TestClient

from gateway.app.main import _build_route_table, app, settings


class FakeRedis:
    def __init__(self):
        self.store = {}
        self.closed = False

    async def incr(self, key):
        self.store[key] = self.store.get(key, 0) + 1
        return self.store[key]

    async def expire(self, key, ttl):
        self.store[f"{key}:ttl"] = ttl

    async def exists(self, key):
        return 1 if key in self.store else 0

    async def get(self, key):
        return self.store.get(key)

    async def setex(self, key, ttl, value):
        self.store[key] = value
        self.store[f"{key}:ttl"] = ttl

    async def aclose(self):
        self.closed = True


@pytest.fixture
def client():
    fake_redis = FakeRedis()
    mock_http = httpx.AsyncClient(
        transport=httpx.MockTransport(lambda request: httpx.Response(200, json={"ok": True}))
    )
    with patch("gateway.app.main.aioredis.from_url", new=AsyncMock(return_value=fake_redis)), patch(
        "gateway.app.main.httpx.AsyncClient", return_value=mock_http
    ):
        with TestClient(app, raise_server_exceptions=False) as test_client:
            # Garante isolamento mesmo se o lifespan real trocar os objetos.
            app.state.redis = fake_redis
            app.state.http_client = mock_http
            app.state.circuit_breakers = {}
            app.state.circuit_breaker_lock = asyncio.Lock()
            yield test_client


def test_cacheable_route_returns_cached_response(client):
    cached_response = JSONResponse(status_code=200, content={"cached": True})
    with patch("gateway.app.main.decode_token", return_value=SimpleNamespace(sub="user-1", role="ADMIN", email="admin@example.com")):
        with patch("gateway.app.main._get_cached_response", new=AsyncMock(return_value=cached_response)) as cached_mock:
            response = client.get(
                "/api/v1/patients/health",
                headers={"Authorization": "Bearer test"},
            )

    assert response.status_code == 200
    assert response.json() == {"cached": True}
    cached_mock.assert_awaited_once()


def test_circuit_breaker_returns_fallback_when_service_fails(client):
    async def failing_forward(*args, **kwargs):
        raise httpx.ConnectError("boom")

    with patch("gateway.app.main.decode_token", return_value=SimpleNamespace(sub="user-1", role="ADMIN", email="admin@example.com")):
        with patch("gateway.app.main._forward_to_service", new=AsyncMock(side_effect=failing_forward)):
            response = client.get(
                "/api/v1/patients/health",
                headers={"Authorization": "Bearer test"},
            )

    assert response.status_code == 503
    assert response.json()["detail"] == "Serviço temporariamente indisponível"


def test_api_key_rate_limit_is_enforced(client):
    async def rate_limited(*args, **kwargs):
        raise HTTPException(status_code=429, detail="too many")

    with patch("gateway.app.main.decode_token", return_value=SimpleNamespace(sub="user-1", role="ADMIN", email="admin@example.com")):
        with patch("gateway.app.main._check_rate_limit", new=AsyncMock(side_effect=rate_limited)):
            response = client.get("/api/v1/patients/health", headers={"X-Api-Key": "demo-key", "Authorization": "Bearer test"})

    assert response.status_code == 429


def test_gzip_response_is_served_when_requested(client):
    large_payload = {"message": "x" * 1500}
    with patch("gateway.app.main.decode_token", return_value=SimpleNamespace(sub="user-1", role="ADMIN", email="admin@example.com")):
        with patch(
            "gateway.app.main._forward_to_service",
            new=AsyncMock(
                return_value=JSONResponse(status_code=200, content=large_payload)
            ),
        ):
            response = client.get(
                "/api/v1/patients/health",
                headers={"Authorization": "Bearer test", "Accept-Encoding": "gzip"},
            )

    assert response.status_code == 200
    assert "gzip" in response.headers.get("content-encoding", "")


def test_unified_routes_use_patient_clinical_service_targets():
    with patch.object(settings, "PATIENT_CLINICAL_SERVICE_URL", "http://patient-clinical:8000"):
        routes = dict((prefix, target) for prefix, target, _ in _build_route_table())

    assert routes["/api/v1/patients"] == "http://patient-clinical:8000"
    assert routes["/api/v1/appointments"] == "http://patient-clinical:8000"
    assert routes["/api/v1/schedules"] == "http://patient-clinical:8000"
    assert routes["/api/v1/records"] == "http://patient-clinical:8000"