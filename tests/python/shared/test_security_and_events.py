import json
from datetime import datetime, timezone

import pytest

from shared.events import MedicalRecordCreatedEvent
from shared.utils.security import (
    create_access_token,
    decode_token,
    hash_password,
    verify_password,
)


SECRET = "test-only-secret-key-with-at-least-32-characters"


@pytest.mark.unit
def test_password_hash_and_verify_support_long_input():
    password = "senha-" + ("x" * 200)
    hashed = hash_password(password)

    assert hashed != password
    assert verify_password(password, hashed)
    assert not verify_password("outra-senha", hashed)


@pytest.mark.unit
def test_access_token_accepts_bearer_and_quoted_format():
    token = create_access_token("user-1", "DOCTOR", "doctor@example.com", SECRET, "HS256")

    payload = decode_token(f'Bearer "{token}"', SECRET, "HS256")

    assert payload.sub == "user-1"
    assert payload.role == "DOCTOR"
    assert payload.type == "access"


@pytest.mark.unit
def test_invalid_token_is_normalized_to_value_error():
    with pytest.raises(ValueError, match="Invalid token"):
        decode_token("Bearer invalid", SECRET, "HS256")


@pytest.mark.unit
def test_event_contract_round_trips_and_accepts_legacy_aliases():
    event = MedicalRecordCreatedEvent(
        eventId="evt-1",
        correlationId="corr-1",
        recordId="record-1",
        appointmentId="appointment-1",
        patientId="patient-1",
        doctorId="doctor-1",
        chiefComplaint="Headache",
        diagnosisCodes=["R51.9"],
    )

    restored = MedicalRecordCreatedEvent.from_json(event.to_json())

    assert restored.event_id == "evt-1"
    assert restored.record_id == "record-1"
    assert restored.diagnosis_codes == ["R51.9"]
    assert json.loads(event.to_json())["event_id"] == "evt-1"
