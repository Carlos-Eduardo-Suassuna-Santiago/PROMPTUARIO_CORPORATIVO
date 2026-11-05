from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException

from app.config import settings
from app.domain.models.schemas import AppointmentCancelRequest, AppointmentCreate, MedicalRecordCreate
from app.domain.services.clinical_service import AppointmentService, MedicalRecordService
from app.infrastructure.events.consumers import _make_user_deactivated_handler


ROOT = Path(__file__).resolve().parents[2]


def test_patient_and_clinical_use_one_database():
    assert settings.DATABASE_URL.startswith("postgresql+asyncpg://")
    for name in ("patient_service.py", "clinical_service.py"):
        source = (ROOT / "patient-clinical-service" / "app" / "domain" / "services" / name).read_text(encoding="utf-8")
        assert "from app.config import settings" in source


@pytest.mark.asyncio
async def test_appointment_preserves_patient_id_and_publishes_event():
    session = MagicMock()
    publisher = AsyncMock()
    service = AppointmentService(session, publisher)
    service.repo.check_slot_conflict = AsyncMock(return_value=False)
    service.repo.create = AsyncMock(side_effect=lambda value: value)
    with patch("app.domain.services.clinical_service.log_operation", new=AsyncMock()):
        appointment = await service.create(
            AppointmentCreate(
                patient_id="patient-1",
                doctor_id="doctor-1",
                scheduled_at=datetime.now(timezone.utc) + timedelta(days=2),
            ),
            "doctor-1",
        )
    assert appointment.patient_id == "patient-1"
    event = publisher.publish.await_args.args[0]
    assert event.patient_id == "patient-1"
    assert service.repo.session is session


@pytest.mark.asyncio
async def test_medical_record_rejects_missing_appointment():
    service = MedicalRecordService(MagicMock(), AsyncMock())
    service.appt_repo.get = AsyncMock(return_value=None)
    with pytest.raises(HTTPException) as exc:
        await service.create(
            MedicalRecordCreate(appointment_id="missing", chief_complaint="valid complaint"),
            "doctor-1",
        )
    assert exc.value.status_code == 404


@pytest.mark.asyncio
async def test_medical_record_isolation_rejects_other_doctor():
    service = MedicalRecordService(MagicMock(), AsyncMock())
    service.appt_repo.get = AsyncMock(return_value=SimpleNamespace(doctor_id="doctor-owner", patient_id="patient-1"))
    with pytest.raises(HTTPException) as exc:
        await service.create(
            MedicalRecordCreate(appointment_id="appointment-1", chief_complaint="valid complaint"),
            "other-doctor",
        )
    assert exc.value.status_code == 403


@pytest.mark.asyncio
async def test_appointment_cancel_updates_status_and_publishes_event():
    appointment = SimpleNamespace(
        id="appointment-1", status="SCHEDULED", scheduled_at=datetime.now(timezone.utc) + timedelta(days=2),
        cancellation_reason=None, cancelled_by=None, cancelled_at=None,
    )
    session = MagicMock()
    publisher = AsyncMock()
    service = AppointmentService(session, publisher)
    service.repo.get = AsyncMock(return_value=appointment)
    service.repo.update = AsyncMock(side_effect=lambda value: value)
    with patch("app.domain.services.clinical_service.log_operation", new=AsyncMock()):
        result = await service.cancel("appointment-1", AppointmentCancelRequest(reason="patient request"), "patient-1", "PATIENT")
    assert result.status == "CANCELLED"
    publisher.publish.assert_awaited_once()


@pytest.mark.asyncio
async def test_user_deactivation_is_idempotent_and_cancels_future_appointments():
    patient = SimpleNamespace(id="patient-1", user_id="user-1", is_active=True)
    appointment = SimpleNamespace(
        id="appointment-1", status="SCHEDULED", scheduled_at=datetime.now(timezone.utc) + timedelta(days=1),
        cancellation_reason=None, cancelled_at=None,
    )
    session = MagicMock()
    session.scalar = AsyncMock(return_value=patient)
    session.flush = AsyncMock()
    session.commit = AsyncMock()
    session.execute = AsyncMock(return_value=SimpleNamespace(scalars=lambda: SimpleNamespace(all=lambda: [appointment])))

    class Context:
        async def __aenter__(self):
            return session
        async def __aexit__(self, *args):
            return False

    with patch("app.infrastructure.events.consumers.log_operation", new=AsyncMock()):
        handler = _make_user_deactivated_handler(lambda: Context(), AsyncMock())
        body = json.dumps({"event_id": "event-1", "user_id": "user-1"}).encode()
        await handler(body)
        await handler(body)

    assert patient.is_active is False
    assert appointment.status == "CANCELLED"
    session.commit.assert_awaited_once()


def test_migration_rollback_is_non_destructive():
    migration_dir = ROOT / "patient-clinical-service" / "migrations"
    migrations = list(migration_dir.glob("*.sql"))
    assert migrations
    text = "\n".join(path.read_text(encoding="utf-8").upper() for path in migrations)
    assert "DROP TABLE" not in text
    assert "ALTER TABLE" in text