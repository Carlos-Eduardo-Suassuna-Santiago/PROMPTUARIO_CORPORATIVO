from __future__ import annotations

import json
import sys
from pathlib import Path
from types import SimpleNamespace


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
BACKEND_ROOT = REPOSITORY_ROOT / "backend"
sys.path.insert(0, str(BACKEND_ROOT))

from shared.events import MedicalRecordCreatedEvent, PrescriptionGeneratedEvent  # noqa: E402
from shared.events.broker import _extract_event_id  # noqa: E402


CONTRACTS = (
    REPOSITORY_ROOT
    / "backend-java"
    / "patient-clinical-service"
    / "src"
    / "test"
    / "resources"
    / "contracts"
)


def _fixture(name: str) -> dict:
    return json.loads((CONTRACTS / name).read_text(encoding="utf-8"))


def test_java_medical_record_json_is_accepted_by_python_model():
    payload = _fixture("medical-record-created.json")

    event = MedicalRecordCreatedEvent.model_validate(payload)

    assert event.event_id == payload["eventId"]
    assert event.event_type == payload["eventType"]
    assert event.correlation_id == payload["correlationId"]
    assert event.record_id == payload["recordId"]
    assert event.patient_id == payload["patientId"]


def test_java_prescription_json_is_accepted_by_python_model():
    payload = _fixture("prescription-generated.json")

    event = PrescriptionGeneratedEvent.model_validate(payload)

    assert event.event_id == payload["eventId"]
    assert event.prescription_id == payload["prescriptionId"]
    assert event.record_id == payload["recordId"]
    assert event.patient_id == payload["patientId"]
    assert event.correlation_id == payload["correlationId"]


def test_models_continue_to_accept_legacy_snake_case():
    payload = _fixture("medical-record-created.json")
    legacy_payload = {
        "event_id": payload["eventId"],
        "timestamp": payload["timestamp"],
        "event_type": payload["eventType"],
        "correlation_id": payload["correlationId"],
        "record_id": payload["recordId"],
        "appointment_id": payload["appointmentId"],
        "patient_id": payload["patientId"],
        "doctor_id": payload["doctorId"],
        "chief_complaint": payload["chiefComplaint"],
        "diagnosis_codes": payload["diagnosisCodes"],
        "prescriptions_count": payload["prescriptionsCount"],
        "exam_requests_count": payload["examRequestsCount"],
    }

    event = MedicalRecordCreatedEvent.model_validate(legacy_payload)

    assert event.event_id == payload["eventId"]
    assert event.record_id == payload["recordId"]
    assert event.patient_id == payload["patientId"]


def test_consumer_idempotency_uses_event_id_header_or_body():
    from_header = SimpleNamespace(
        headers={"eventId": "event-from-header"},
        body=b"{}",
        message_id="delivery-id",
    )
    from_body = SimpleNamespace(
        headers={},
        body=b'{"event_id":"event-from-body"}',
        message_id="delivery-id",
    )

    assert _extract_event_id(from_header) == "event-from-header"
    assert _extract_event_id(from_body) == "event-from-body"
