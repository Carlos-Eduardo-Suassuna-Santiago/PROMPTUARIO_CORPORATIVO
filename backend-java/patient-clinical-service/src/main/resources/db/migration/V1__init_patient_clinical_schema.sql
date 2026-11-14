CREATE TABLE patients (
    id                  VARCHAR(36)  PRIMARY KEY,
    user_id             VARCHAR(36)  NOT NULL UNIQUE,
    full_name           VARCHAR(255) NOT NULL,
    cpf                 VARCHAR(14)  UNIQUE,
    date_of_birth       DATE,
    gender              VARCHAR(10),
    blood_type          VARCHAR(5),
    phone               VARCHAR(20),
    email               VARCHAR(255),
    street              VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(2),
    zip_code            VARCHAR(9),
    emergency_name      VARCHAR(255),
    emergency_phone     VARCHAR(20),
    emergency_relation  VARCHAR(50),
    notes               TEXT,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    anonymized          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_patients_gender
        CHECK (gender IS NULL OR gender IN ('M', 'F', 'OTHER'))
);

CREATE INDEX ix_patients_full_name_lower ON patients (LOWER(full_name));

CREATE TABLE allergies (
    id             VARCHAR(36)  PRIMARY KEY,
    patient_id     VARCHAR(36)  NOT NULL,
    substance      VARCHAR(255) NOT NULL,
    severity       VARCHAR(20)  NOT NULL,
    reaction_type  VARCHAR(255),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_allergies_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT ck_allergies_severity
        CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE'))
);

CREATE INDEX ix_allergies_patient_id ON allergies (patient_id);

CREATE TABLE vaccines (
    id             VARCHAR(36)  PRIMARY KEY,
    patient_id     VARCHAR(36)  NOT NULL,
    name           VARCHAR(255) NOT NULL,
    dose           VARCHAR(50),
    manufacturer   VARCHAR(100),
    lot_number     VARCHAR(100),
    applied_at     DATE,
    next_dose_at   DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_vaccines_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX ix_vaccines_patient_id ON vaccines (patient_id);

CREATE TABLE continuous_medications (
    id                   VARCHAR(36)  PRIMARY KEY,
    patient_id           VARCHAR(36)  NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    dosage               VARCHAR(100) NOT NULL,
    frequency            VARCHAR(100) NOT NULL,
    route                VARCHAR(100),
    prescribing_doctor   VARCHAR(255),
    started_at           DATE,
    ended_at             DATE,
    end_reason           VARCHAR(255),
    notes                TEXT,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    version              INTEGER      NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_continuous_medications_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX ix_continuous_medications_patient_active
    ON continuous_medications (patient_id, active);

CREATE TABLE medication_history (
    id                   VARCHAR(36)  PRIMARY KEY,
    patient_id           VARCHAR(36)  NOT NULL,
    medication_id        VARCHAR(36),
    name                 VARCHAR(255) NOT NULL,
    dosage               VARCHAR(100) NOT NULL,
    frequency            VARCHAR(100) NOT NULL,
    prescribing_doctor   VARCHAR(255),
    started_at           DATE,
    ended_at             DATE,
    end_reason           VARCHAR(255),
    notes                TEXT,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    version              INTEGER      NOT NULL,
    change_type          VARCHAR(20)  NOT NULL,
    changed_by           VARCHAR(255),
    snapshot             JSONB,
    created_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_medication_history_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_medication_history_medication
        FOREIGN KEY (medication_id) REFERENCES continuous_medications (id) ON DELETE SET NULL,
    CONSTRAINT ck_medication_history_change_type
        CHECK (change_type IN ('CREATED', 'UPDATED', 'DEACTIVATED', 'REACTIVATED'))
);

CREATE INDEX ix_medication_history_patient_created_at
    ON medication_history (patient_id, created_at DESC);
CREATE INDEX ix_medication_history_medication_id
    ON medication_history (medication_id);

CREATE TABLE patient_documents (
    id             VARCHAR(36)  PRIMARY KEY,
    patient_id     VARCHAR(36)  NOT NULL,
    document_type  VARCHAR(30)  NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    s3_key          VARCHAR(512) NOT NULL,
    file_size       BIGINT       NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    file_hash       VARCHAR(64),
    description     TEXT,
    uploaded_by     VARCHAR(36),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_patient_documents_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT ck_patient_documents_document_type
        CHECK (document_type IN ('EXAM', 'PRESCRIPTION', 'REPORT', 'IMAGE', 'LAB_RESULT', 'CERTIFICATE', 'OTHER'))
);

CREATE INDEX ix_patient_documents_patient_active_created_at
    ON patient_documents (patient_id, is_active, created_at DESC);

CREATE TABLE doctor_schedules (
    id          VARCHAR(36)  PRIMARY KEY,
    doctor_id   VARCHAR(36)  NOT NULL,
    specialty   VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_doctor_schedules_doctor_active
    ON doctor_schedules (doctor_id, is_active);

CREATE TABLE time_slots (
    id            VARCHAR(36) PRIMARY KEY,
    schedule_id   VARCHAR(36) NOT NULL,
    slot_date     DATE        NOT NULL,
    start_time    VARCHAR(5)  NOT NULL,
    end_time      VARCHAR(5)  NOT NULL,
    is_available  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_time_slots_schedule
        FOREIGN KEY (schedule_id) REFERENCES doctor_schedules (id) ON DELETE CASCADE
);

CREATE INDEX ix_time_slots_schedule_id ON time_slots (schedule_id);
CREATE INDEX ix_time_slots_date_available
    ON time_slots (slot_date, is_available, schedule_id);

CREATE TABLE appointments (
    id                   VARCHAR(36) PRIMARY KEY,
    patient_id           VARCHAR(36) NOT NULL,
    doctor_id            VARCHAR(36) NOT NULL,
    slot_id              VARCHAR(36),
    scheduled_at         TIMESTAMPTZ NOT NULL,
    appointment_type     VARCHAR(30) NOT NULL DEFAULT 'CONSULTATION',
    specialty            VARCHAR(100),
    status               VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    cancellation_reason  VARCHAR(255),
    cancelled_by         VARCHAR(36),
    cancelled_at         TIMESTAMPTZ,
    notes                TEXT,
    created_by           VARCHAR(36) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_time_slot
        FOREIGN KEY (slot_id) REFERENCES time_slots (id),
    CONSTRAINT ck_appointments_type
        CHECK (appointment_type IN ('CONSULTATION', 'RETURN', 'EXAM', 'URGENT')),
    CONSTRAINT ck_appointments_status
        CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);

CREATE INDEX ix_appointments_patient_scheduled_status
    ON appointments (patient_id, scheduled_at, status);
CREATE INDEX ix_appointments_doctor_scheduled_status
    ON appointments (doctor_id, scheduled_at, status);
CREATE INDEX ix_appointments_status_scheduled_at
    ON appointments (status, scheduled_at);
CREATE INDEX ix_appointments_slot_id ON appointments (slot_id);

CREATE TABLE medical_records (
    id                VARCHAR(36) PRIMARY KEY,
    appointment_id    VARCHAR(36) NOT NULL UNIQUE,
    patient_id        VARCHAR(36) NOT NULL,
    doctor_id         VARCHAR(36) NOT NULL,
    chief_complaint   TEXT        NOT NULL,
    anamnesis         TEXT,
    physical_exam     TEXT,
    diagnosis         TEXT,
    diagnosis_codes   JSONB,
    treatment_plan    TEXT,
    observations      TEXT,
    rich_notes         JSONB,
    signature_hash    VARCHAR(64),
    signed_by         VARCHAR(36),
    signed_at         TIMESTAMPTZ,
    ai_analysis_id    VARCHAR(36),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_medical_records_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE CASCADE
);

CREATE INDEX ix_medical_records_patient_created_at
    ON medical_records (patient_id, created_at DESC);

CREATE TABLE medical_record_history (
    id           VARCHAR(36) PRIMARY KEY,
    record_id    VARCHAR(36) NOT NULL,
    changed_by   VARCHAR(36) NOT NULL,
    change_type  VARCHAR(50) NOT NULL,
    snapshot     JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_medical_record_history_record
        FOREIGN KEY (record_id) REFERENCES medical_records (id) ON DELETE CASCADE
);

CREATE INDEX ix_medical_record_history_record_created_at
    ON medical_record_history (record_id, created_at DESC);

CREATE TABLE prescriptions (
    id                VARCHAR(36) PRIMARY KEY,
    record_id         VARCHAR(36) NOT NULL,
    patient_id        VARCHAR(36) NOT NULL,
    doctor_id         VARCHAR(36) NOT NULL,
    medications       JSONB       NOT NULL,
    instructions      TEXT,
    valid_days         INTEGER     NOT NULL DEFAULT 30,
    pdf_s3_key         VARCHAR(500),
    pdf_generated_at  TIMESTAMPTZ,
    signature_hash    VARCHAR(64),
    signed_by         VARCHAR(36),
    signed_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_prescriptions_record
        FOREIGN KEY (record_id) REFERENCES medical_records (id) ON DELETE CASCADE
);

CREATE INDEX ix_prescriptions_record_created_at
    ON prescriptions (record_id, created_at DESC);
CREATE INDEX ix_prescriptions_patient_created_at
    ON prescriptions (patient_id, created_at DESC);

CREATE TABLE prescription_history (
    id               VARCHAR(36) PRIMARY KEY,
    prescription_id  VARCHAR(36) NOT NULL,
    record_id        VARCHAR(36) NOT NULL,
    changed_by       VARCHAR(36) NOT NULL,
    change_type      VARCHAR(50) NOT NULL,
    snapshot         JSONB       NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_prescription_history_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions (id) ON DELETE CASCADE
);

CREATE INDEX ix_prescription_history_prescription_created_at
    ON prescription_history (prescription_id, created_at DESC);
CREATE INDEX ix_prescription_history_record_id
    ON prescription_history (record_id);

CREATE TABLE exam_requests (
    id            VARCHAR(36)  PRIMARY KEY,
    record_id     VARCHAR(36)  NOT NULL,
    patient_id    VARCHAR(36)  NOT NULL,
    doctor_id     VARCHAR(36)  NOT NULL,
    exam_type     VARCHAR(255) NOT NULL,
    urgency       VARCHAR(20)  NOT NULL DEFAULT 'ROUTINE',
    instructions  TEXT,
    result         TEXT,
    result_date    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_exam_requests_record
        FOREIGN KEY (record_id) REFERENCES medical_records (id) ON DELETE CASCADE,
    CONSTRAINT ck_exam_requests_urgency
        CHECK (urgency IN ('ROUTINE', 'URGENT', 'EMERGENCY'))
);

CREATE INDEX ix_exam_requests_record_created_at
    ON exam_requests (record_id, created_at DESC);
CREATE INDEX ix_exam_requests_patient_created_at
    ON exam_requests (patient_id, created_at DESC);

CREATE TABLE exam_request_history (
    id           VARCHAR(36) PRIMARY KEY,
    exam_id      VARCHAR(36) NOT NULL,
    record_id    VARCHAR(36) NOT NULL,
    changed_by   VARCHAR(36) NOT NULL,
    change_type  VARCHAR(50) NOT NULL,
    snapshot     JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_exam_request_history_exam
        FOREIGN KEY (exam_id) REFERENCES exam_requests (id) ON DELETE CASCADE
);

CREATE INDEX ix_exam_request_history_exam_created_at
    ON exam_request_history (exam_id, created_at DESC);
CREATE INDEX ix_exam_request_history_record_id
    ON exam_request_history (record_id);

CREATE TABLE medical_certificates (
    id                VARCHAR(36) PRIMARY KEY,
    record_id         VARCHAR(36) NOT NULL,
    patient_id        VARCHAR(36) NOT NULL,
    doctor_id         VARCHAR(36) NOT NULL,
    reason            TEXT        NOT NULL,
    days_off          INTEGER     NOT NULL,
    start_date        DATE        NOT NULL,
    notes             TEXT,
    pdf_s3_key         VARCHAR(255),
    pdf_generated_at  TIMESTAMPTZ,
    signature_hash    VARCHAR(64),
    signed_by         VARCHAR(36),
    signed_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_medical_certificates_record
        FOREIGN KEY (record_id) REFERENCES medical_records (id) ON DELETE CASCADE
);

CREATE INDEX ix_medical_certificates_record_created_at
    ON medical_certificates (record_id, created_at DESC);

CREATE TABLE medical_certificate_history (
    id              VARCHAR(36) PRIMARY KEY,
    certificate_id  VARCHAR(36) NOT NULL,
    record_id       VARCHAR(36) NOT NULL,
    changed_by      VARCHAR(36) NOT NULL,
    change_type     VARCHAR(50) NOT NULL,
    snapshot        JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_medical_certificate_history_certificate
        FOREIGN KEY (certificate_id) REFERENCES medical_certificates (id) ON DELETE CASCADE
);

CREATE INDEX ix_medical_certificate_history_certificate_created_at
    ON medical_certificate_history (certificate_id, created_at DESC);
CREATE INDEX ix_medical_certificate_history_record_id
    ON medical_certificate_history (record_id);

CREATE TABLE audit_logs (
    id              VARCHAR(36)  PRIMARY KEY,
    service_name    VARCHAR(50)  NOT NULL,
    table_name      VARCHAR(100) NOT NULL,
    operation       VARCHAR(30)  NOT NULL,
    record_id       VARCHAR(36),
    user_id         VARCHAR(36),
    user_role       VARCHAR(20),
    user_email      VARCHAR(255),
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(45),
    request_id      VARCHAR(36),
    correlation_id  VARCHAR(36),
    timestamp       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_audit_logs_service_timestamp
    ON audit_logs (service_name, timestamp);
CREATE INDEX ix_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX ix_audit_logs_operation ON audit_logs (operation);
CREATE INDEX ix_audit_logs_correlation_id ON audit_logs (correlation_id);
