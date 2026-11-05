"""
FASE 2 — Validação das configurações unificadas.

Garante que ``app/config.py`` expõe todas as configurações necessárias para os
bounded contexts Patient e Clinical, mantém os buckets separados, preserva as
variáveis obrigatórias e compartilha o mesmo ``DATABASE_URL``, sem sobrescrever
configurações com valores incompatíveis.
"""
from __future__ import annotations

import os

# Garante que a instância global de ``Settings`` seja criável nos testes,
# mesmo quando a variável não é injetada pelo runner de teste.
os.environ.setdefault(
    "JWT_SECRET_KEY", "test-secret-key-for-patient-clinical-config-tests"
)

import pytest  # noqa: E402
from pydantic import ValidationError  # noqa: E402
from pathlib import Path  # noqa: E402


def test_settings_exposes_required_blocks():
    """As configurações exigidas na Fase 2 devem estar presentes e válidas."""
    from app.config import settings

    # Conexões e segurança
    assert settings.DATABASE_URL.startswith("postgresql+asyncpg://")
    assert "amqp://" in settings.RABBITMQ_URL
    assert settings.JWT_ALGORITHM == "HS256"

    # S3 de documentos e de prescrições
    assert settings.S3_BUCKET_PATIENT_DOCUMENTS
    assert settings.S3_BUCKET_PRESCRIPTIONS

    # Celery
    assert settings.CELERY_BROKER_URL.startswith("redis://")
    assert settings.CELERY_RESULT_BACKEND.startswith("redis://")

    # Regras de negócio (cancelamento + assinatura)
    assert settings.APPOINTMENT_CANCEL_HOURS_MIN >= 0
    assert settings.SIGNATURE_HASH_ALGORITHM == "sha256"

    # Limites de upload
    assert settings.MAX_DOCUMENT_SIZE_MB > 0
    assert len(settings.ALLOWED_DOCUMENT_MIME_TYPES) > 0


def test_buckets_are_kept_separate():
    """patient-documents e prescriptions devem permanecer como buckets distintos."""
    from app.config import settings

    assert settings.S3_BUCKET_PATIENT_DOCUMENTS == "patient-documents"
    assert settings.S3_BUCKET_PRESCRIPTIONS == "prescriptions"
    assert settings.S3_BUCKET_PATIENT_DOCUMENTS != settings.S3_BUCKET_PRESCRIPTIONS


def test_jwt_secret_stays_required(monkeypatch):
    """A única variável obrigatória (JWT_SECRET_KEY) deve continuar obrigatória."""
    from app.config import Settings

    monkeypatch.delenv("JWT_SECRET_KEY", raising=False)
    with pytest.raises(ValidationError):
        Settings(_env_file=None)


def test_settings_can_be_created_with_env_overrides(monkeypatch):
    """Os testes conseguem criar configurações próprias via variáveis de ambiente."""
    from app.config import Settings

    db = "postgresql+asyncpg://pc:pass@db-patient-clinical:5432/patient_clinical_db"
    monkeypatch.setenv("DATABASE_URL", db)
    monkeypatch.setenv("JWT_SECRET_KEY", "some-secret-for-override-test")

    s = Settings(_env_file=None)
    assert s.DATABASE_URL == db


def test_patient_and_clinical_share_same_database_url():
    """Patient e Clinical consomem a MESMA variável/instância de DATABASE_URL.

    Em Python, ``from app.config import settings`` em dois módulos retorna o
    mesmo objeto (singleton do módulo), então a fonte é a MESMA ``DATABASE_URL``.
    """
    from app.config import settings as global_settings

    assert global_settings.DATABASE_URL.startswith("postgresql+asyncpg://")

    # Prova estrutural (não depende de carregar o stack OpenTelemetry/gRPC):
    service_root = Path(__file__).resolve().parents[1]
    for fname in ("patient_service.py", "clinical_service.py"):
        src = (service_root / "app" / "domain" / "services" / fname).read_text(
            encoding="utf-8"
        )
        assert "from app.config import settings" in src

    # Prova runtime (usa importorskip: em ambientes onde a política de
    # aplicativos bloqueia o gRPC do OpenTelemetry o teste é pulado em vez de
    # falhar por motivo ambiental).
    _reason = "dependência OpenTelemetry/gRPC bloqueada por política de aplicativos do host"
    patient_svc = pytest.importorskip(
        "app.domain.services.patient_service", reason=_reason, exc_type=ImportError
    )
    clinical_svc = pytest.importorskip(
        "app.domain.services.clinical_service", reason=_reason, exc_type=ImportError
    )

    assert patient_svc.settings is global_settings
    assert clinical_svc.settings is global_settings
    assert patient_svc.settings.DATABASE_URL == clinical_svc.settings.DATABASE_URL


def test_no_incompatible_overrides():
    """Certifica a coerência entre Patient e Clinical nas chaves compartilhadas."""
    from app.config import settings

    # Credenciais S3 unificadas (não devem divergir entre contexts).
    assert settings.S3_ACCESS_KEY == "promptuario"
    assert settings.S3_SECRET_KEY == "promptuario_pass"
    # Buckets distintos, apontando para nomes estáveis.
    assert settings.S3_BUCKET_PATIENT_DOCUMENTS == "patient-documents"
    assert settings.S3_BUCKET_PRESCRIPTIONS == "prescriptions"