"""Unified Event consumers for Patient-Clinical Service.

Handles IAM user events. Patient synchronization (projection) events are no
longer consumed internally â€” the unified service reads Patient directly from
the ``patients`` table. External publication of those events is untouched.
"""
from __future__ import annotations

import json
import logging
from datetime import datetime, timezone
from sqlalchemy import select

from app.config import settings
from shared.audit import log_operation
from shared.events import (
    UserCreatedEvent,
    UserDeactivatedEvent,
)
from shared.events.broker import EventConsumer

logger = logging.getLogger(__name__)


def setup_consumers(consumer: EventConsumer, session_factory, publisher) -> None:
    """Register all event handlers for Patient-Clinical Service."""

    # IAM User events
    consumer.register(
        exchange=UserCreatedEvent.EXCHANGE,
        routing_key=UserCreatedEvent.ROUTING_KEY,
        handler=_make_user_created_handler(session_factory, publisher),
    )
    consumer.register(
        exchange=UserDeactivatedEvent.EXCHANGE,
        routing_key=UserDeactivatedEvent.ROUTING_KEY,
        handler=_make_user_deactivated_handler(session_factory, publisher),
    )


def _make_user_created_handler(session_factory, publisher):
    processed_event_ids: set[str] = set()

    async def handle(body: bytes) -> None:
        try:
            data = json.loads(body)
            event_id = data.get("event_id")
            if event_id and event_id in processed_event_ids:
                return
            role = data.get("role")
            if role != "PATIENT":
                return

            user_id = data.get("user_id")
            if not user_id:
                return

            from app.domain.schemas.patient_schemas import PatientCreate
            from app.domain.services.patient_service import PatientService

            async with session_factory() as session:
                svc = PatientService(session, publisher)

                try:
                    await svc.get_by_user(user_id)
                    logger.info("Patient already exists for user %s", user_id)
                    return
                except Exception:
                    pass

                patient_create = PatientCreate(
                    user_id=user_id,
                    full_name=data.get("full_name"),
                    cpf=data.get("cpf"),
                    date_of_birth=data.get("date_of_birth"),
                    gender=data.get("gender"),
                    phone=data.get("phone"),
                    email=data.get("email"),
                )
                await svc.create(patient_create)
                await session.commit()
                if event_id:
                    processed_event_ids.add(event_id)
                logger.info("Patient profile created for user %s", user_id)

        except Exception as e:
            logger.error("Error handling UserCreated: %s", e)
            raise

    return handle


def _make_user_deactivated_handler(session_factory, publisher):
    processed_event_ids: set[str] = set()

    async def handle(body: bytes) -> None:
        """Desativa o paciente e cancela consultas futuras, de forma transacional."""
        try:
            data = json.loads(body)
            event_id = data.get("event_id")
            if event_id and event_id in processed_event_ids:
                return
            user_id = data.get("user_id")
            if not user_id:
                return

            from app.domain.models.patient import Patient
            from app.domain.models.clinical import Appointment

            async with session_factory() as session:
                # 1. Localizar o paciente diretamente (tabela patients)
                patient = await session.scalar(
                    select(Patient).where(Patient.user_id == user_id)
                )

                # 2. Desativar o paciente + auditoria (sem commit: tudo numa transaÃ§Ã£o)
                if patient and patient.is_active:
                    patient.is_active = False
                    await session.flush()
                    await log_operation(
                        session,
                        service=settings.SERVICE_NAME,
                        table="patients",
                        operation="DELETE",
                        record_id=patient.id,
                        user_id=user_id,
                        new_values={"is_active": False},
                    )
                    logger.info("Patient deactivated for user %s", user_id)

                # 3. Cancelar consultas futuras + auditoria
                patient_id = patient.id if patient else None
                if patient_id:
                    now = datetime.now(timezone.utc)
                    future_appts = (
                        await session.execute(
                            select(Appointment).where(
                                Appointment.patient_id == patient_id,
                                Appointment.status.in_(["SCHEDULED", "CONFIRMED"]),
                                Appointment.scheduled_at > now,
                            )
                        )
                    ).scalars().all()

                    for appt in future_appts:
                        appt.status = "CANCELLED"
                        appt.cancellation_reason = "UsuÃ¡rio desativado"
                        appt.cancelled_at = now
                        await log_operation(
                            session,
                            service=settings.SERVICE_NAME,
                            table="appointments",
                            operation="DELETE",
                            record_id=appt.id,
                            user_id=user_id,
                            new_values={
                                "status": "CANCELLED",
                                "cancellation_reason": "UsuÃ¡rio desativado",
                            },
                        )
                    logger.info(
                        "Cancelled %d future appointments for user %s",
                        len(future_appts), user_id,
                    )

                # 4. Commit Ãºnico (transacional)
                await session.commit()
                if event_id:
                    processed_event_ids.add(event_id)

        except Exception as e:
            logger.error("Error handling UserDeactivated: %s", e)
            raise

    return handle
