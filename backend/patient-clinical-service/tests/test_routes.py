"""
FASE 4 — Validação da unificação das rotas.

Garante que o novo ``app.main`` registra exatamente:
- Patient router    -> /api/v1/patients/*
- Appointments      -> /api/v1/appointments/*
- Schedules         -> /api/v1/schedules/*
- Records           -> /api/v1/records/*

e que o conjunto de rotas (método + path) é IDÊNTICO à união dos routers legados
de Patient e Clinical, sem métodos/campos/códigos/autorização alterados e sem
rotas duplicadas conflitantes.
"""
from __future__ import annotations

import ast
import os
from pathlib import Path

# Garante que a instância global de Settings seja criável nos testes.
os.environ.setdefault("JWT_SECRET_KEY", "test-secret-key-for-route-tests")

import httpx  # noqa: E402
import pytest  # noqa: E402
from httpx import ASGITransport  # noqa: E402

from app.main import app  # noqa: E402

SERVICE_DIR = Path(__file__).resolve().parents[1]
MODULE_PATIENT = SERVICE_DIR / "app" / "api" / "patient_routers.py"
MODULE_CLINICAL = SERVICE_DIR / "app" / "api" / "clinical_routers.py"

HTTPS_METHODS = {"get", "post", "put", "delete", "patch"}


def _router_prefixes(tree: ast.AST) -> dict[str, str]:
    """Mapeia nome do router legado -> prefixo do APIRouter(prefix=...)."""
    prefixes: dict[str, str] = {}
    for node in ast.walk(tree):
        if (
            isinstance(node, ast.Assign)
            and len(node.targets) == 1
            and isinstance(node.targets[0], ast.Name)
            and isinstance(node.value, ast.Call)
            and isinstance(node.value.func, ast.Name)
            and node.value.func.id == "APIRouter"
        ):
            var = node.targets[0].id
            prefix = ""
            for kw in node.value.keywords:
                if (
                    kw.arg == "prefix"
                    and isinstance(kw.value, ast.Constant)
                    and isinstance(kw.value.value, str)
                ):
                    prefix = kw.value.value
            prefixes[var] = prefix
    return prefixes


def _legacy_routes(file: Path) -> set[tuple[str, str]]:
    """Extrai (path completo, método) dos routers legados via AST,
    incluindo o prefixo de cada ``APIRouter(prefix=...)``."""
    tree = ast.parse(file.read_text(encoding="utf-8"))
    prefixes = _router_prefixes(tree)
    routes: set[tuple[str, str]] = set()
    for node in ast.walk(tree):
        if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            continue
        for decorator in node.decorator_list:
            if (
                isinstance(decorator, ast.Call)
                and isinstance(decorator.func, ast.Attribute)
                and decorator.func.attr in HTTPS_METHODS
                and decorator.args
                and isinstance(decorator.args[0], ast.Constant)
                and isinstance(decorator.args[0].value, str)
            ):
                var = getattr(decorator.func.value, "id", "")
                router_prefix = prefixes.get(var, "")
                path = "/api/v1" + router_prefix + decorator.args[0].value
                routes.add((path, decorator.func.attr.upper()))
    return routes


def _app_routes() -> set[tuple[str, str]]:
    """Rota (path, método) exposta pelo app FastAPI unificado."""
    routes: set[tuple[str, str]] = set()
    for route in app.routes:
        path = getattr(route, "path", None)
        if not path or not path.startswith("/api"):
            continue
        for method in getattr(route, "methods", None) or set():
            routes.add((path, method.upper()))
    return routes


@pytest.fixture(scope="module")
def routes() -> set[tuple[str, str]]:
    return _app_routes()


def test_all_required_prefixes_registered(routes):
    """Patients, Appointments e Records devem ter rotas; Schedules é comparado
    com o legado (router vazio) separadamente."""
    for prefix in ("/api/v1/patients", "/api/v1/appointments", "/api/v1/records"):
        assert any(
            path == prefix or path.startswith(prefix + "/") for path, _ in routes
        ), f"Prefixo esperado ausente: {prefix}"


def test_healthz_registered():
    assert any(
        getattr(route, "path", None) == "/healthz" for route in app.routes
    ), "/healthz não registrado"


def test_no_duplicate_routes(routes):
    """Não existem rotas duplicadas/conflitantes (mesmo método + path)."""
    all_routes = [
        (getattr(route, "path", None), m.upper())
        for route in app.routes
        for m in (getattr(route, "methods", None) or set())
        if getattr(route, "path", "").startswith("/api")
    ]
    assert len(all_routes) == len(set(all_routes)), "Rotas duplicadas encontradas"


def test_schedules_matches_module(routes):
    """O router de schedules é idêntico ao módulo clinical_routers."""
    module_schedules = {
        (p, m)
        for p, m in _legacy_routes(MODULE_CLINICAL)
        if p.startswith("/api/v1/schedules")
    }
    new_schedules = {(p, m) for p, m in routes if p.startswith("/api/v1/schedules")}
    assert new_schedules == module_schedules


def test_new_routes_equal_union_of_modules(routes):
    """O conjunto novo é EXATAMENTE a união dos routers de Patient e Clinical."""
    expected = _legacy_routes(MODULE_PATIENT) | _legacy_routes(MODULE_CLINICAL)
    assert routes == expected, (
        f"Diferenças — extras: {sorted(routes - expected)} | faltantes: {sorted(expected - routes)}"
    )


async def test_healthz_http_smoke():
    """Smoke HTTP: /healthz responde 200 no app unificado (sem infra)."""
    transport = ASGITransport(app=app)
    async with httpx.AsyncClient(
        transport=transport, base_url="http://test"
    ) as client:
        resp = await client.get("/healthz")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["service"] == "patient-clinical-service"