-- Add the shared audit correlation field required by the unified service.
-- Non-destructive and idempotent; legacy databases are not modified.
BEGIN;
ALTER TABLE public.audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS ix_audit_logs_correlation_id ON public.audit_logs (correlation_id);
ALTER TABLE public.audit_logs_patient_staging ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(36);
COMMIT;
