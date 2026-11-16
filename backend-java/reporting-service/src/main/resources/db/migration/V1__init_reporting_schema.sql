-- Initial schema for the Reporting Service (PostgreSQL 15).
-- Enum values are stored as VARCHAR to match Hibernate @Enumerated(EnumType.STRING).

CREATE TABLE report_jobs (
    id                  VARCHAR(36)  PRIMARY KEY,
    report_type         VARCHAR(30) NOT NULL,
    requested_by        VARCHAR(36) NOT NULL,
    parameters          JSONB       NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    output_format       VARCHAR(10) NOT NULL DEFAULT 'JSON',
    result_data         JSONB,
    s3_key              VARCHAR(500),
    error_message       TEXT,
    row_count           INT         NOT NULL DEFAULT 0,
    schedule_id         VARCHAR(36),
    webhook_triggered   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMPTZ,
    CONSTRAINT ck_report_jobs_report_type
        CHECK (report_type IN ('CONSULTATIONS', 'PATIENTS', 'DOCTORS', 'PRESCRIPTIONS', 'CUSTOM', 'FULL_SYSTEM')),
    CONSTRAINT ck_report_jobs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_report_jobs_output_format
        CHECK (output_format IN ('JSON', 'CSV', 'PDF', 'XLSX')),
    CONSTRAINT ck_report_jobs_row_count
        CHECK (row_count >= 0)
);

CREATE TABLE report_schedules (
    id                  VARCHAR(36)  PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    report_type         VARCHAR(30)  NOT NULL,
    output_format       VARCHAR(10)  NOT NULL DEFAULT 'XLSX',
    cron_expression     VARCHAR(100) NOT NULL,
    parameters          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    recipients          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run_at         TIMESTAMPTZ,
    next_run_at         TIMESTAMPTZ,
    created_by          VARCHAR(36)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ,
    CONSTRAINT ck_report_schedules_report_type
        CHECK (report_type IN ('CONSULTATIONS', 'PATIENTS', 'DOCTORS', 'PRESCRIPTIONS', 'CUSTOM', 'FULL_SYSTEM')),
    CONSTRAINT ck_report_schedules_output_format
        CHECK (output_format IN ('JSON', 'CSV', 'PDF', 'XLSX'))
);

CREATE TABLE daily_stats (
    id                  VARCHAR(36) PRIMARY KEY,
    stat_date           VARCHAR(10) NOT NULL,
    stat_type           VARCHAR(50) NOT NULL,
    entity_id           VARCHAR(36),
    value               INT         NOT NULL DEFAULT 0,
    metadata            JSONB       NOT NULL DEFAULT '{}'::jsonb,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_daily_stats_stat_date_format
        CHECK (stat_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$')
);

CREATE TABLE webhook_configs (
    id                      VARCHAR(36)  PRIMARY KEY,
    url                     VARCHAR(500) NOT NULL,
    secret                  VARCHAR(128) NOT NULL,
    description             VARCHAR(255) DEFAULT '',
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    max_retries             INT          NOT NULL DEFAULT 3,
    retry_interval_seconds  INT          NOT NULL DEFAULT 60,
    events                  JSONB        NOT NULL DEFAULT '[]'::jsonb,
    last_delivery_at        TIMESTAMPTZ,
    last_success_at         TIMESTAMPTZ,
    consecutive_failures    INT          NOT NULL DEFAULT 0,
    created_by              VARCHAR(36)  NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ,
    CONSTRAINT ck_webhook_configs_max_retries
        CHECK (max_retries >= 0),
    CONSTRAINT ck_webhook_configs_retry_interval
        CHECK (retry_interval_seconds >= 0),
    CONSTRAINT ck_webhook_configs_consecutive_failures
        CHECK (consecutive_failures >= 0)
);

CREATE TABLE webhook_delivery_logs (
    id                  VARCHAR(36)  PRIMARY KEY,
    webhook_config_id   VARCHAR(36)  NOT NULL,
    job_id              VARCHAR(36)  NOT NULL,
    event_type          VARCHAR(50)  NOT NULL,
    url                 VARCHAR(500) NOT NULL,
    status_code         INT,
    response_body       TEXT,
    success             BOOLEAN      NOT NULL DEFAULT FALSE,
    attempt             INT          NOT NULL DEFAULT 1,
    error_message       TEXT,
    delivered_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_delivery_logs_config
        FOREIGN KEY (webhook_config_id) REFERENCES webhook_configs (id) ON DELETE CASCADE,
    CONSTRAINT ck_webhook_delivery_logs_attempt
        CHECK (attempt > 0)
);

CREATE TABLE report_audit_logs (
    id                  VARCHAR(36)  PRIMARY KEY,
    event_type          VARCHAR(40)  NOT NULL,
    entity_type         VARCHAR(30)  NOT NULL,
    entity_id           VARCHAR(36),
    description         VARCHAR(500) DEFAULT '',
    performed_by        VARCHAR(36)  NOT NULL,
    event_metadata      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    ip_address          VARCHAR(45),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_daily_stats_stat_date_stat_type
    ON daily_stats (stat_date, stat_type);
CREATE INDEX ix_daily_stats_entity_id
    ON daily_stats (entity_id);

CREATE INDEX ix_report_jobs_status
    ON report_jobs (status);
CREATE INDEX ix_report_jobs_created_at
    ON report_jobs (created_at);
CREATE INDEX ix_report_jobs_requested_by
    ON report_jobs (requested_by);

CREATE INDEX ix_webhook_delivery_logs_job_id
    ON webhook_delivery_logs (job_id);
CREATE INDEX ix_report_audit_logs_created_at
    ON report_audit_logs (created_at);
