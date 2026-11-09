-- Phase 6 prerequisite: align clinical_db with the unified clinical model.
-- Non-destructive and idempotent: no existing rows or tables are dropped.
BEGIN;

CREATE TABLE IF NOT EXISTS medical_certificates (
    id VARCHAR(36) PRIMARY KEY,
    record_id VARCHAR(36) NOT NULL,
    patient_id VARCHAR(36) NOT NULL,
    doctor_id VARCHAR(36) NOT NULL,
    reason TEXT NOT NULL,
    days_off INTEGER NOT NULL,
    start_date DATE NOT NULL,
    notes TEXT NULL,
    pdf_s3_key VARCHAR(255) NULL,
    pdf_generated_at TIMESTAMPTZ NULL,
    signature_hash VARCHAR(64) NULL,
    signed_by VARCHAR(36) NULL,
    signed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT medical_certificates_record_id_fkey
        FOREIGN KEY (record_id) REFERENCES medical_records(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_medical_certificates_record_id
    ON medical_certificates(record_id);

CREATE TABLE IF NOT EXISTS medical_certificate_history (
    id VARCHAR(36) PRIMARY KEY,
    certificate_id VARCHAR(36) NOT NULL,
    record_id VARCHAR(36) NOT NULL,
    changed_by VARCHAR(36) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    snapshot JSON NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT medical_certificate_history_certificate_id_fkey
        FOREIGN KEY (certificate_id) REFERENCES medical_certificates(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_medical_certificate_history_certificate_id
    ON medical_certificate_history(certificate_id);

COMMIT;
