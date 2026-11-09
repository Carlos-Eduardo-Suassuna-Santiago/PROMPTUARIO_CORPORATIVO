from __future__ import annotations

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.clinical_routers import appointments_router, records_router, schedules_router
from app.api.patient_routers import router as patients_router
from app.config import settings

# Import every domain model so that ``Base.metadata`` is fully populated with
# both the Patient and the Clinical tables before the engine runs.
from app.domain.models.patient import (  # noqa: F401
    Allergy,
    ContinuousMedication,
    MedicationHistory,
    Patient,
    PatientDocument,
    Vaccine,
)
from app.domain.models.clinical import (  # noqa: F401
    Appointment,
    DoctorSchedule,
    ExamRequest,
    ExamRequestHistory,
    MedicalCertificate,
    MedicalCertificateHistory,
    MedicalRecord,
    MedicalRecordHistory,

    Prescription,
    PrescriptionHistory,
    TimeSlot,
)
from app.domain.services.clinical_service import _ensure_s3_bucket as _ensure_prescriptions_bucket
from app.domain.services.patient_service import _ensure_s3_bucket as _ensure_patient_documents_bucket
from app.infrastructure.events.consumers import setup_consumers
from shared.events.broker import EventConsumer, EventPublisher
from shared.models.database import Base, build_engine, build_session_factory
from shared.observability import setup_observability

logger = logging.getLogger(__name__)

app = FastAPI(
    title="PROMPTUARIO — Patient-Clinical Service",
    description="Cadastro de pacientes, agendamentos, prontuários, prescrições e documentos",
    version="1.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

setup_observability(app, settings.SERVICE_NAME, settings.LOG_LEVEL)

# Public API surface is preserved exactly as it was in the legacy services.
#   /api/v1/patients/*      -> patient_routers
#   /api/v1/appointments/*  -> clinical_routers
#   /api/v1/records/*       -> clinical_routers
#   /api/v1/schedules/*     -> clinical_routers
app.include_router(patients_router, prefix="/api/v1")
app.include_router(appointments_router, prefix="/api/v1")
app.include_router(records_router, prefix="/api/v1")
app.include_router(schedules_router, prefix="/api/v1")


@app.on_event("startup")
async def startup():
    engine = build_engine(settings.DATABASE_URL)
    # NOTE: ``create_all`` mirrors the legacy services and is only meant to
    # bootstrap a brand-new database. Real data migration (preserving IDs) is
    # handled by the dedicated migration phase and must NOT rely on this call.
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    app.state.session_factory = build_session_factory(engine)

    publisher = EventPublisher(settings.RABBITMQ_URL)
    await publisher.connect()
    app.state.publisher = publisher

    consumer = EventConsumer(settings.RABBITMQ_URL, settings.SERVICE_NAME)
    await consumer.connect()
    setup_consumers(consumer, app.state.session_factory, publisher)
    await consumer.start()
    app.state.consumer = consumer

    # Keep the two storage buckets separate: patient documents vs prescriptions.
    _ensure_patient_documents_bucket()
    _ensure_prescriptions_bucket()

    logger.info("Patient-Clinical Service started ✅")


@app.on_event("shutdown")
async def shutdown():
    await app.state.publisher.close()
    await app.state.consumer.close()


@app.get("/healthz", tags=["Health"])
async def health():
    return {"status": "ok", "service": settings.SERVICE_NAME}
