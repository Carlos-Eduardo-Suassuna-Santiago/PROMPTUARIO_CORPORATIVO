# PROMPTUARIO — Patient-Clinical Service Python (legado)

Serviço unificado que reúne o **Patient Service** e o **Clinical Service** em um
único microserviço, preservando os contratos externos (URLs, campos, códigos
HTTP, eventos e buckets) dos serviços legados.

> **Implementação legada para rollback.** O serviço ativo está em
> `backend-java/patient-clinical-service` e é iniciado pelo Compose como um
> único container Java. A geração assíncrona de PDFs roda no mesmo processo,
> por meio de um `@RabbitListener` Spring AMQP.

## Contexto no ecossistema Promptuário

```
Gateway (:8000) → Patient-Clinical Java (:8000 interno) → PostgreSQL / RabbitMQ / MinIO
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

- PostgreSQL, RabbitMQ e MinIO disponíveis
- Python 3.12+

## Início rápido

### Desenvolvimento local da implementação legada (sem Docker)

```bash
cd backend/patient-clinical-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### Implementação ativa com Docker Compose

O Compose constrói somente `patient-clinical-service`. O listener Spring AMQP
de geração de PDF faz parte do mesmo JAR e não requer container separado.

```bash
cd backend
make dev-patient-clinical
```

### Worker Spring AMQP de prescrições

O worker ativo é `PrescriptionPdfWorker`, localizado em
`backend-java/patient-clinical-service`. Ele é iniciado automaticamente pelo
Spring Boot junto com a API e consome a fila durável
`prescription.pdf.generation`.

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
| `APPOINTMENT_CANCEL_HOURS_MIN`  | Horas mínimas para cancelamento            | `24`           |

## Rollback

Este diretório Python é mantido apenas como referência de rollback. O Gateway e
os dois arquivos Compose apontam para a implementação Java unificada; nenhum
worker ou configuração Celery do Patient-Clinical deve ser reintroduzido.
