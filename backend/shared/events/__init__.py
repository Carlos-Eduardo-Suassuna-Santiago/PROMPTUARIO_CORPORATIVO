"""
Shared domain events for PROMPTUARIO.
All microservices import from this module to ensure event contract consistency.
"""
from __future__ import annotations

import json
import uuid
from datetime import datetime, timezone
from typing import Any, ClassVar

from pydantic import AliasChoices, BaseModel, Field


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _new_id() -> str:
    return f"evt_{uuid.uuid4().hex[:20]}"


def _new_correlation_id() -> str:
    return str(uuid.uuid4())


# ─── Base ─────────────────────────────────────────────────────────────────────

class DomainEvent(BaseModel):
    event_id: str = Field(
        default_factory=_new_id,
        validation_alias=AliasChoices("event_id", "eventId"),
    )
    event_type: str = Field(validation_alias=AliasChoices("event_type", "eventType"))
    timestamp: datetime = Field(default_factory=_now)
    correlation_id: str = Field(
        default_factory=_new_correlation_id,
        validation_alias=AliasChoices("correlation_id", "correlationId"),
    )
    version: str = "1.0"

    def to_json(self) -> bytes:
        return self.model_dump_json().encode()

    @classmethod
    def from_json(cls, data: bytes) -> "DomainEvent":
        return cls.model_validate_json(data)


# ─── IAM Events ───────────────────────────────────────────────────────────────

class UserCreatedEvent(DomainEvent):
    event_type: str = "UserCreated"
    user_id: str
    email: str
    role: str
    full_name: str
    created_at: datetime = Field(default_factory=_now)
    cpf: str | None = None
    date_of_birth: str | None = None
    gender: str | None = None
    phone: str | None = None

    ROUTING_KEY: ClassVar[str] = "iam.user.created"
    EXCHANGE: ClassVar[str] = "promptuario.iam"


class UserDeactivatedEvent(DomainEvent):
    event_type: str = "UserDeactivated"
    user_id: str
    reason: str
    deactivated_by: str

    ROUTING_KEY: ClassVar[str] = "iam.user.deactivated"
    EXCHANGE: ClassVar[str] = "promptuario.iam"


class UserReactivatedEvent(DomainEvent):
    event_type: str = "UserReactivated"
    user_id: str
    reactivated_by: str

    ROUTING_KEY: ClassVar[str] = "iam.user.reactivated"
    EXCHANGE: ClassVar[str] = "promptuario.iam"


class UserUpdatedEvent(DomainEvent):
    event_type: str = "UserUpdated"
    user_id: str
    changed_fields: list[str]
    full_name: str | None = None
    email: str | None = None

    ROUTING_KEY: ClassVar[str] = "iam.user.updated"
    EXCHANGE: ClassVar[str] = "promptuario.iam"


# ─── Patient Events ───────────────────────────────────────────────────────────

class PatientCreatedEvent(DomainEvent):
    event_type: str = "PatientCreated"
    patient_id: str
    user_id: str
    full_name: str
    date_of_birth: str | None = None
    blood_type: str | None = None

    ROUTING_KEY: ClassVar[str] = "patient.created"
    EXCHANGE: ClassVar[str] = "promptuario.patient"


class PatientUpdatedEvent(DomainEvent):
    event_type: str = "PatientUpdated"
    patient_id: str
    changed_fields: list[str]
    phone: str | None = None
    full_name: str | None = None

    ROUTING_KEY: ClassVar[str] = "patient.updated"
    EXCHANGE: ClassVar[str] = "promptuario.patient"


class AllergyAddedEvent(DomainEvent):
    event_type: str = "AllergyAdded"
    patient_id: str
    allergy_id: str
    substance: str
    severity: str

    ROUTING_KEY: ClassVar[str] = "patient.allergy.added"
    EXCHANGE: ClassVar[str] = "promptuario.patient"


# ─── Clinical Events ──────────────────────────────────────────────────────────

class AppointmentCreatedEvent(DomainEvent):
    event_type: str = "AppointmentCreated"
    appointment_id: str
    patient_id: str
    doctor_id: str
    scheduled_at: datetime
    appointment_type: str
    specialty: str | None = None

    ROUTING_KEY: ClassVar[str] = "clinical.appointment.created"
    EXCHANGE: ClassVar[str] = "promptuario.clinical"


class AppointmentCancelledEvent(DomainEvent):
    event_type: str = "AppointmentCancelled"
    appointment_id: str
    cancelled_by: str
    cancellation_reason: str
    hours_before: float
    policy_violated: bool

    ROUTING_KEY: ClassVar[str] = "clinical.appointment.cancelled"
    EXCHANGE: ClassVar[str] = "promptuario.clinical"


class MedicalRecordCreatedEvent(DomainEvent):
    event_type: str = Field(
        default="MedicalRecordCreated",
        validation_alias=AliasChoices("event_type", "eventType"),
    )
    record_id: str = Field(validation_alias=AliasChoices("record_id", "recordId"))
    appointment_id: str = Field(validation_alias=AliasChoices("appointment_id", "appointmentId"))
    patient_id: str = Field(validation_alias=AliasChoices("patient_id", "patientId"))
    doctor_id: str = Field(validation_alias=AliasChoices("doctor_id", "doctorId"))
    chief_complaint: str = Field(validation_alias=AliasChoices("chief_complaint", "chiefComplaint"))
    diagnosis_codes: list[str] = Field(
        default_factory=list,
        validation_alias=AliasChoices("diagnosis_codes", "diagnosisCodes"),
    )
    prescriptions_count: int = Field(
        default=0,
        validation_alias=AliasChoices("prescriptions_count", "prescriptionsCount"),
    )
    exam_requests_count: int = Field(
        default=0,
        validation_alias=AliasChoices("exam_requests_count", "examRequestsCount"),
    )

    ROUTING_KEY: ClassVar[str] = "clinical.medical_record.created"
    EXCHANGE: ClassVar[str] = "promptuario.clinical"


class PrescriptionGeneratedEvent(DomainEvent):
    event_type: str = Field(
        default="PrescriptionGenerated",
        validation_alias=AliasChoices("event_type", "eventType"),
    )
    prescription_id: str = Field(validation_alias=AliasChoices("prescription_id", "prescriptionId"))
    record_id: str = Field(validation_alias=AliasChoices("record_id", "recordId"))
    patient_id: str = Field(validation_alias=AliasChoices("patient_id", "patientId"))
    doctor_id: str = Field(validation_alias=AliasChoices("doctor_id", "doctorId"))
    medications: list[dict[str, Any]] = Field(default_factory=list)
    pdf_s3_key: str | None = Field(
        default=None,
        validation_alias=AliasChoices("pdf_s3_key", "pdfS3Key"),
    )

    ROUTING_KEY: ClassVar[str] = "clinical.prescription.generated"
    EXCHANGE: ClassVar[str] = "promptuario.clinical"


# ─── AI Events ────────────────────────────────────────────────────────────────

class AnalysisCompletedEvent(DomainEvent):
    event_type: str = "AnalysisCompleted"
    job_id: str
    record_id: str | None = None
    patient_id: str
    analysis_type: str
    risk_level: str
    result: dict[str, Any] = {}
    model_version: str = "v1.0"

    ROUTING_KEY: ClassVar[str] = "ai.analysis.completed"
    EXCHANGE: ClassVar[str] = "promptuario.ai"


# ─── Exchange names ───────────────────────────────────────────────────────────

EXCHANGES = {
    "domain.events": "topic",
    "promptuario.iam": "topic",
    "promptuario.patient": "topic",
    "promptuario.clinical": "topic",
    "promptuario.ai": "topic",
    "promptuario.dlx": "direct",
}
