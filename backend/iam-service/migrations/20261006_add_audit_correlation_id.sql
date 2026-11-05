-- Keep the IAM audit table aligned with shared.audit.AuditLog.
ALTER TABLE public.audit_logs
ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS ix_audit_logs_correlation_id
ON public.audit_logs (correlation_id);
