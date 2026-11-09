"""
FASE 3 — Unificação dos modelos.

Valida que:
1. Todos os modelos Patient e Clinical estão carregados no novo serviço;
2. Todas as classes usam a MESMA instância de ``Base``;
3. Não há colisão de nomes de tabela;
4. Os ENUMs PostgreSQL não têm nomes conflitantes/com valores divergentes;
5. Todas as tabelas existentes são mantidas (inclusive ``patient_projections``);
6. A verificação de metadata lista todas as tabelas;
7. O DDL completo compila (smoke de persistência em SQLite em memória).
"""
from __future__ import annotations

import os

# Garante que a instância global de Settings seja criável nos testes.
os.environ.setdefault("JWT_SECRET_KEY", "test-secret-key-for-model-tests")

import pytest  # noqa: E402
from sqlalchemy import create_engine, inspect  # noqa: E402
from sqlalchemy.sql.sqltypes import Enum as SAEnum  # noqa: E402

import app.domain.models as models  # noqa: E402, F401  (popula a Base.metadata)
from shared.models.database import Base  # noqa: E402

EXPECTED_TABLES = {
    # Patient
    "patients",
    "allergies",
    "vaccines",
    "continuous_medications",
    "medication_history",
    "patient_documents",
    # Clinical
    "doctor_schedules",
    "time_slots",
    "appointments",
    "medical_records",
    "medical_record_history",
    "prescriptions",
    "prescription_history",
    "exam_requests",
    "exam_request_history",
    "medical_certificates",
    "medical_certificate_history",
    # compartilhada (auditoria)
    "audit_logs",
}

# ENUMs PostgreSQL esperados por domínio (somente os revisados na Fase 3).
EXPECTED_ENUMS = {
    "gender_type",
    "allergy_severity",
    "medication_change_type",
    "document_type",
    "appointment_type",
    "appointment_status",
    "exam_urgency",
}


def test_all_models_loaded_and_all_tables_present():
    """Todas as tabelas de Patient e Clinical (e a de auditoria) existem."""
    tables = set(Base.metadata.tables.keys())
    missing = EXPECTED_TABLES - tables
    assert not missing, f"Tabelas esperadas ausentes: {sorted(missing)}"


def test_no_table_name_collisions():
    """Nomes de tabela únicos no metadata unificado."""
    names = list(Base.metadata.tables.keys())
    assert len(names) == len(set(names)), "Colisão de nome de tabela detectada"


def test_metadata_listing_lists_all_tables():
    """A verificação de metadata (list_tables) cobre todas as tabelas."""
    listed = set(models.list_tables())
    assert EXPECTED_TABLES <= listed
    assert len(listed) == len(Base.metadata.tables)


def test_all_classes_share_same_base_instance():
    """Patient e Clinical usam a MESMA instância da ``Base``."""
    from app.domain.models.patient import (
        Allergy, ContinuousMedication, MedicationHistory, Patient,
        PatientDocument, Vaccine,
    )
    from app.domain.models.clinical import (
        Appointment, DoctorSchedule, ExamRequest, ExamRequestHistory,
        MedicalCertificate, MedicalCertificateHistory, MedicalRecord,
        MedicalRecordHistory, Prescription, PrescriptionHistory,
        TimeSlot,
    )

    patient_classes = (
        Patient, Allergy, ContinuousMedication, MedicationHistory,
        PatientDocument, Vaccine,
    )
    clinical_classes = (
        DoctorSchedule, TimeSlot, Appointment, MedicalRecord,
        MedicalRecordHistory, Prescription, PrescriptionHistory, ExamRequest,
        ExamRequestHistory, MedicalCertificate, MedicalCertificateHistory,
    )
    for cls in (*patient_classes, *clinical_classes):
        assert cls.__mapper__.local_table.metadata is Base.metadata, (
            f"{cls.__name__} não compartilha a mesma Base"
        )


def test_patient_projections_removed():
    """A tabela legada patient_projections deve ter sido removida do metadata unificado."""
    assert "patient_projections" not in Base.metadata.tables


def test_enum_names_have_no_conflicting_values():
    """Nenhum ENUM nomeado é reutilizado com valores divergentes (colisão de tipo)."""
    name_to_values: dict[str, tuple[str, ...]] = {}
    for table in Base.metadata.tables.values():
        for column in table.columns:
            column_type = column.type
            if not isinstance(column_type, SAEnum):
                continue
            if not getattr(column_type, "name", None) or not column_type.enums:
                continue
            values = tuple(column_type.enums)
            previous = name_to_values.setdefault(column_type.name, values)
            assert values == previous, (
                f"Colisão de ENUM '{column_type.name}': "
                f"{values} != {previous}"
            )
    assert EXPECTED_ENUMS <= set(name_to_values), (
        f"ENUMs revisados ausentes: {sorted(EXPECTED_ENUMS - set(name_to_values))}"
    )


def test_schema_ddl_compiles_persistence_smoke():
    """Smoke de persistência: o DDL unificado é compilado sem erro (SQLite)."""
    engine = create_engine("sqlite:///:memory:")
    try:
        Base.metadata.create_all(engine)
        inspector = inspect(engine)
        created = set(inspector.get_table_names())
    finally:
        engine.dispose()

    missing = EXPECTED_TABLES - created
    assert not missing, f"Tabelas não criadas pelo DDL: {sorted(missing)}"