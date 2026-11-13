from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    SERVICE_NAME: str = "patient-clinical-service"
    LOG_LEVEL: str = "INFO"
    DEBUG: bool = False

    # Banco único e compartilhado pelos bounded contexts Patient e Clinical.
    # Ambos os domínios leem a MESMA variável, garantindo consistência de dados.
    # (A migração para o banco dedicado é tratada nas fases de dados.)
    DATABASE_URL: str = "postgresql+asyncpg://clinical:clinical_pass@localhost:5432/clinical_db"
    RABBITMQ_URL: str = "amqp://promptuario:promptuario_pass@localhost:5672/"

    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"

    # MinIO / S3-compatible storage.
    # As credenciais seguem a infraestrutura MinIO (promptuario/promptuario_pass);
    # o patient-service legado usava o default de desenvolvimento (minioadmin),
    # mantido aqui apenas como nota para evitar override incompatível.
    S3_ENDPOINT: str = "http://localhost:9000"
    S3_PUBLIC_ENDPOINT: str = "http://localhost:9000"
    S3_ACCESS_KEY: str = "promptuario"
    S3_SECRET_KEY: str = "promptuario_pass"
    S3_REGION: str = "us-east-1"
    S3_PRESIGNED_URL_EXPIRY: int = 300  # seconds

    # S3 Buckets — mantidos separados de propósito:
    S3_BUCKET_PATIENT_DOCUMENTS: str = "patient-documents"  # documentos de pacientes
    S3_BUCKET_PRESCRIPTIONS: str = "prescriptions"          # PDFs de prescrições/atestados

    # Document upload limits
    MAX_DOCUMENT_SIZE_MB: int = 50
    ALLOWED_DOCUMENT_MIME_TYPES: list[str] = [
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/tiff",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "text/csv",
    ]

    # PDF generation
    PDF_GENERATION_ENABLED: bool = True
    PDF_TEMPLATE_LOGO_URL: str = ""

    # Business rules
    APPOINTMENT_CANCEL_HOURS_MIN: int = 24

    # Signature
    SIGNATURE_HASH_ALGORITHM: str = "sha256"


settings = Settings()
