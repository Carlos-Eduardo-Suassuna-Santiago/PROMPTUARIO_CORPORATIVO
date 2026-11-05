# PROMPTUARIO — Patient-Clinical Service

Serviço unificado que reúne o **Patient Service** e o **Clinical Service** em um
único microserviço, preservando os contratos externos (URLs, campos, códigos
HTTP, eventos e buckets) dos serviços legados.

> **Fase 1 — em construção.** Este serviço foi criado **sem remover** os
> serviços antigos (`patient-service` e `clinical-service`), que continuam
> ativos e são o caminho de rollback durante toda a migração.

## Contexto no ecossistema Promptuário

```
Gateway (:8000) → Patient-Clinical Service (:8000 interno) → PostgreSQL / RabbitMQ / Redis / MinIO
```

Porta padrão do serviço: `8000` (atrás do API Gateway, que expõe `:8000`).

## Estrutura

Os módulos Patient e Clinical ficam **separados internamente**, o que reduz o
risco de regressão e facilita o rollback:

```
app/
├── api/
│   ├── patient_routers.py     # /api/v1/patients/*
│   └── clinical_routers.py    # /api/v1/appointments|records|schedules/*
├── domain/
│   ├── models/                # patient.py + clinical.py (mesma Base)
│   ├── schemas/               # patient_schemas.py + clinical_schemas.py
│   └── services/              # patient_service.py + clinical_service.py
├── infrastructure/
│   ├── events/                # consumidores IAM + sincronização Patient
│   ├── repositories/          # patient_repository.py + clinical_repository.py
│   └── s3/                    # cliente S3/MinIO
└── workers/                   # Celery (PDF de prescrições)
```

## Endpoints preservados

| Método | Endpoint                                  | Origem  |
|--------|-------------------------------------------|---------|
| GET    | `/api/v1/patients`                        | Patient |
| POST   | `/api/v1/patients`                        | Patient |
| GET    | `/api/v1/patients/me`                     | Patient |
| GET    | `/api/v1/patients/{id}`                   | Patient |
| POST   | `/api/v1/patients/{id}/allergies`         | Patient |
| POST   | `/api/v1/patients/{id}/documents/upload`  | Patient |
| GET    | `/api/v1/appointments`                    | Clinical|
| POST   | `/api/v1/appointments`                    | Clinical|
| POST   | `/api/v1/records`                         | Clinical|
| POST   | `/api/v1/schedules`                       | Clinical|
| GET    | `/healthz`                                | —       |

As URLs públicas, os nomes de campos, os códigos HTTP e as regras de
autorização permanecem **idênticos** aos serviços originais.

## Buckets MinIO (mantidos separados)

- `patient-documents` — documentos de pacientes
- `prescriptions` — PDFs de prescrições e atestados

## Eventos de domínio

Publica (contratos inalterados, consumidos por AI e Reporting):

- `PatientCreated`, `PatientUpdated`, `AllergyAdded`
- `AppointmentCreated`, `AppointmentCancelled`
- `MedicalRecordCreated`, `PrescriptionGenerated`

Consome:

- `UserCreated`, `UserDeactivated` (IAM)
- `PatientCreated`, `PatientUpdated` (sincronização da projeção durante a migração)

## Pré-requisitos

- PostgreSQL, RabbitMQ, Redis e MinIO disponíveis
- Python 3.12+

## Início rápido

### Desenvolvimento local (sem Docker)

```bash
cd backend/patient-clinical-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### Com Docker Compose

> A entrada no `docker-compose.yml` (serviço + worker + banco) é adicionada em
> uma fase posterior da migração.

```bash
cd backend
docker compose build patient-clinical-service
```

### Worker Celery (geração de PDF de prescrições)

```bash
cd backend/patient-clinical-service
celery -A app.workers.celery_app.celery_app worker --loglevel=info -Q celery
```

## Testes

```bash
cd backend/patient-clinical-service
python -m pytest tests/ -v --tb=short
```

Os testes são os mesmos dos serviços legados (pacientes, documentos, histórico
de medicamentos, clínicos, PDFs e assinaturas/rich notes), garantindo paridade
de comportamento.

## Variáveis de ambiente importantes

| Variável                        | Descrição                                  | Default        |
|---------------------------------|--------------------------------------------|----------------|
| `DATABASE_URL`                  | URL de conexão com PostgreSQL              | —              |
| `RABBITMQ_URL`                  | URL de conexão com RabbitMQ                | —              |
| `JWT_SECRET_KEY`                | Chave secreta JWT (obrigatória)            | *obrigatório*  |
| `JWT_ALGORITHM`                 | Algoritmo JWT                              | `HS256`        |
| `S3_ENDPOINT`                   | Endpoint MinIO/S3                          | —              |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | Credenciais MinIO                        | —              |
| `S3_BUCKET_PATIENT_DOCUMENTS`   | Bucket de documentos de pacientes          | `patient-documents` |
| `S3_BUCKET_PRESCRIPTIONS`       | Bucket de prescrições                      | `prescriptions` |
| `CELERY_BROKER_URL`             | Broker do Celery (Redis)                   | `redis://localhost:6379/3` |
| `APPOINTMENT_CANCEL_HOURS_MIN`  | Horas mínimas para cancelamento            | `24`           |

## Rollback

Enquanto a migração não é concluída, o Gateway continua podendo apontar para
`patient-service` e `clinical-service`. Este serviço é aditivo e não altera os
serviços antigos, permitindo reverter o corte sem perda de dados.
