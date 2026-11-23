-- Mantém o IAM compatível com o agregador de auditoria cross-service.
CREATE TABLE IF NOT EXISTS audit_logs (
    id              VARCHAR(36) PRIMARY KEY,
    service_name    VARCHAR(50) NOT NULL,
    table_name      VARCHAR(100) NOT NULL,
    operation       VARCHAR(30) NOT NULL,
    record_id       VARCHAR(36),
    user_id         VARCHAR(36),
    user_role       VARCHAR(20),
    user_email      VARCHAR(255),
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(45),
    request_id      VARCHAR(36),
    correlation_id  VARCHAR(36),
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_audit_logs_service_timestamp ON audit_logs (service_name, timestamp);
CREATE INDEX IF NOT EXISTS ix_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS ix_audit_logs_operation ON audit_logs (operation);
CREATE INDEX IF NOT EXISTS ix_audit_logs_correlation_id ON audit_logs (correlation_id);
