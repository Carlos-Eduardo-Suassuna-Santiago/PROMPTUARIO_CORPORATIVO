from app.infrastructure.repositories.patient_repository import (
    AllergyRepository,
    DocumentRepository,
    MedicationHistoryRepository,
    MedicationRepository,
    PatientRepository,
    VaccineRepository,
)
from app.infrastructure.repositories.clinical_repository import (
    AppointmentRepository,
    ExamRequestRepository,
    MedicalCertificateRepository,
    MedicalRecordRepository,
    PrescriptionRepository,
)

__all__ = [
    "PatientRepository",
    "AllergyRepository",
    "VaccineRepository",
    "MedicationRepository",
    "MedicationHistoryRepository",
    "DocumentRepository",
    "AppointmentRepository",
    "MedicalRecordRepository",
    "PrescriptionRepository",
    "ExamRequestRepository",
    "MedicalCertificateRepository",
]
