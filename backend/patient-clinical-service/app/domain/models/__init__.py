from app.domain.models.patient import (
    Allergy,
    ContinuousMedication,
    MedicationHistory,
    Patient,
    PatientDocument,
    Vaccine,
)
from app.domain.models.clinical import (
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
from shared.models.database import Base

__all__ = [
    "Patient",
    "Allergy",
    "Vaccine",
    "ContinuousMedication",
    "MedicationHistory",
    "PatientDocument",

    "DoctorSchedule",
    "TimeSlot",
    "Appointment",
    "MedicalRecord",
    "MedicalRecordHistory",
    "Prescription",
    "PrescriptionHistory",
    "ExamRequest",
    "ExamRequestHistory",
    "MedicalCertificate",
    "MedicalCertificateHistory",
    "Base",
    "list_tables",
]


def list_tables() -> list[str]:
    """Lista todas as tabelas carregadas no metadata unificado da ``Base``.

    Útil para validação (Fase 3): confirma que Patient e Clinical estão na mesma
    ``Base`` sem colisão de nomes de tabela.
    """
    return sorted(Base.metadata.tables.keys())
