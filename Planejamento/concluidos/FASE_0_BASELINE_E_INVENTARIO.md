# Relatório de Baseline e Inventário — Fase 0
## Unificação: `patient-service` + `clinical-service` $\rightarrow$ `patient-clinical-service`

**Data de Execução:** 01/10/2026  
**Responsável Técnico:** Engenheiro Senior de Integração / Arquitetura  
**Status da Fase 0:** CONCLUÍDO COM SUCESSO (Auditoria e Inventário realizados sem alterações de código-fonte)

---

## 1. Visão Geral e Objetivos

O objetivo deste documento é consolidar o inventário e o estado de baseline dos microserviços **`backend/patient-service`** e **`backend/clinical-service`** antes do início da unificação no novo serviço **`backend/patient-clinical-service`**.

### Regras Obrigatórias de Governança
1. **Preservação dos serviços legados:** Não apagar nem desativar `patient-service` ou `clinical-service` durante as fases intermediárias.
2. **Imutabilidade de Contratos Públicos:** Não alterar URLs, rotas e prefixos de API consumidos pelo Frontend ou Gateway.
3. **Imutabilidade de Identificadores:** Não alterar UUIDs/chaves de pacientes, consultas, prontuários, prescrições, atestados ou documentos.
4. **Preservação do Barramento de Eventos:** Manter todos os eventos RabbitMQ consumidos por `ai-service`, `reporting-service` ou outros serviços.
5. **Critério de Parada:** Não avançar de fase caso a fase atual contenha erros impeditivos.
6. **Integridade do Git:** Preservar modificações existentes e versionar incrementalmente.
7. **Proibição de `Base.metadata.create_all()` em Produção:** Garantir migrações formais e seguras de dados.

---

## 2. Estado do Versionamento (Git Status)

- **Branch Atual:** `develop` (alinhado com `origin/develop`).
- **Modificações preexistentes não commitadas na Workspace:**
  - `DOCUMENTATION/ARQUITETURA_DE_SOFTWARE.md`
  - `DOCUMENTATION/DIAGRAMA_DE_CASOS_DE_USO.md`
  - `DOCUMENTATION/PLANO_DE_TESTES.md`
  - `DOCUMENTATION/REQUISITOS_HISTORIAS_DE_USUARIOS.md`
  - `DOCUMENTATION/TECNOLOGIAS_UTILIZADAS.md`
- **Código-fonte dos Serviços (`backend/patient-service` e `backend/clinical-service`):** Limpo, sem alterações pendentes.

---

## 3. Inventário de Arquitetura e Estrutura dos Serviços

### 3.1. Estrutura de Arquivos

```
backend/
├── patient-service/
│   ├── app/
│   │   ├── api/routers.py                    # Endpoints REST de pacientes, documentos, vacinas, medicamentos
│   │   ├── config.py                         # Configurações Pydantic Settings
│   │   ├── main.py                           # App FastAPI, Lifespan, Event Broker & Metrics
│   │   ├── domain/
│   │   │   ├── models/patient.py & schemas.py # Modelos ORM SQLAlchemy e DTOs Pydantic
│   │   │   └── services/patient_service.py   # Lógica de negócio (Patient, Allergy, Vaccine, Medication, Document)
│   │   └── infrastructure/
│   │       ├── events/consumers.py           # Consumidores AMQP (UserCreatedEvent, UserDeactivatedEvent)
│   │       └── repositories/patient_repository.py # Repositórios assíncronos SQLAlchemy
│   ├── tests/
│   │   ├── test_patient.py
│   │   ├── test_documents.py
│   │   └── test_medication_history.py
│   ├── Dockerfile
│   └── requirements.txt
│
├── clinical-service/
│   ├── app/
│   │   ├── api/routers.py                    # Endpoints REST de consultas, agendas, prontuários, exames
│   │   ├── config.py                         # Configurações Pydantic Settings
│   │   ├── main.py                           # App FastAPI, Lifespan & Event Broker
│   │   ├── domain/
│   │   │   ├── models/clinical.py & schemas.py # Modelos ORM SQLAlchemy e DTOs Pydantic
│   │   │   └── services/clinical_service.py  # Lógica de negócio (Appointment, Record, Rx, Exam, Cert)
│   │   ├── infrastructure/
│   │   │   ├── events/consumers.py           # Consumidores AMQP (PatientCreated, PatientUpdated, UserDeactivated)
│   │   │   └── repositories/clinical_repository.py # Repositórios assíncronos SQLAlchemy
│   │   └── workers/
│   │       ├── celery_app.py                 # Configuração da instância Celery
│   │       └── prescription_tasks.py         # Tarefas assíncronas para geração de PDF de prescrição
│   ├── tests/
│   │   ├── test_clinical.py
│   │   ├── test_prescription_pdf.py
│   │   └── test_rich_notes_and_signature.py
│   ├── Dockerfile
│   ├── Dockerfile.worker
│   └── requirements.txt
```

---

## 4. Variáveis de Ambiente e Configurações

### Patient Service
| Variável | Valor Padrão / Exemplo | Finalidade |
| :--- | :--- | :--- |
| `SERVICE_NAME` | `patient-service` | Identificador de métricas e rastreabilidade |
| `DATABASE_URL` | `postgresql+asyncpg://patient:patient_pass@db-patient:5432/patient_db` | Banco relacional do Patient |
| `RABBITMQ_URL` | `amqp://promptuario:promptuario_pass@rabbitmq:5672/` | Mensageria RabbitMQ |
| `JWT_SECRET_KEY` | `super-secret-change-in-production-32chars` | Validação de tokens JWT |
| `JWT_ALGORITHM` | `HS256` | Algoritmo criptográfico de autenticação |
| `S3_ENDPOINT` | `http://minio:9000` | Endpoint interno MinIO / S3 |
| `S3_PUBLIC_ENDPOINT` | `http://localhost:9000` | Endpoint público para presigned URLs |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | `promptuario` / `promptuario_pass` | Credenciais S3 |
| `S3_BUCKET_PATIENT_DOCUMENTS` | `patient-documents` | Bucket para exames e documentos anexos |
| `S3_PRESIGNED_URL_EXPIRY` | `300` | Validade dos links temporários de download (segundos) |
| `MAX_DOCUMENT_SIZE_MB` | `50` | Limite de upload por documento |

### Clinical Service
| Variável | Valor Padrão / Exemplo | Finalidade |
| :--- | :--- | :--- |
| `SERVICE_NAME` | `clinical-service` | Identificador de métricas e rastreabilidade |
| `DATABASE_URL` | `postgresql+asyncpg://clinical:clinical_pass@db-clinical:5432/clinical_db` | Banco relacional do Clinical |
| `RABBITMQ_URL` | `amqp://promptuario:promptuario_pass@rabbitmq:5672/` | Mensageria RabbitMQ |
| `JWT_SECRET_KEY` | `super-secret-change-in-production-32chars` | Validação de tokens JWT |
| `JWT_ALGORITHM` | `HS256` | Algoritmo de assinatura |
| `S3_ENDPOINT` | `http://minio:9000` | Endpoint interno MinIO / S3 |
| `S3_PUBLIC_ENDPOINT` | `http://localhost:9000` | Endpoint público para presigned URLs |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | `promptuario` / `promptuario_pass` | Credenciais S3 |
| `S3_BUCKET_PRESCRIPTIONS` | `prescriptions` | Bucket para PDFs de prescrições e atestados |
| `CELERY_BROKER_URL` | `redis://redis:6379/3` | Broker Celery para workers assíncronos |
| `CELERY_RESULT_BACKEND` | `redis://redis:6379/3` | Backend de resultados de tarefas Celery |
| `PDF_GENERATION_ENABLED` | `True` | Habilitação de geração de PDFs de prescrições |
| `APPOINTMENT_CANCEL_HOURS_MIN` | `24` | Política de antecedência mínima para cancelamento por paciente |

---

## 5. Mapeamento de Tabelas de Banco de Dados (Total: 18 Domínio + Audit)

### Tabelas em `patient_db` (6 tabelas)
1. `patients` — Dados cadastrais, identificação civil (CPF), contatos, endereço e flags LGPD (`is_active`, `anonymized`).
2. `allergies` — Cadastro de alergias com severidade (`MILD`, `MODERATE`, `SEVERE`) e tipo de reação.
3. `vaccines` — Registro de vacinação, dose, lote e agendamento de reforço.
4. `continuous_medications` — Medicamentos em uso contínuo, posologia, médico prescritor e status ativo.
5. `medication_history` — Trilha de auditoria imutável registrando versões e motivos de alterações/desativações.
6. `patient_documents` — Metadados de arquivos armazenados no S3 (tipo, tamanho, hash SHA-256, chave S3, soft delete).
7. `audit_logs` — Trilha de auditoria de operações.

### Tabelas em `clinical_db` (12 tabelas)
1. `patient_projections` — Cópia local desnormalizada de pacientes criada para permitir consultas sem chamadas síncronas HTTP.
2. `doctor_schedules` — Configuração semanal de agenda por médico.
3. `time_slots` — Horários discretos de atendimento para marcação de consultas.
4. `appointments` — Agendamentos de consultas com status (`SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`).
5. `medical_records` — Prontuário médico com anamnese, exame físico, diagnóstico CID, rich notes sanitizadas e assinatura digital.
6. `medical_record_history` — Histórico imutável de alterações do prontuário médico.
7. `prescriptions` — Prescrições vinculadas ao prontuário, lista de medicamentos, instruções e chave do PDF gerado.
8. `prescription_history` — Histórico imutável de prescrições.
9. `exam_requests` — Solicitações de exames laboratoriais/imagem e laudos de resultados.
10. `exam_request_history` — Histórico de solicitações de exames.
11. `medical_certificates` — Atestados médicos com dias de repouso, motivo, chave do PDF e hash de assinatura.
12. `medical_certificate_history` — Histórico de atestados médicos emitidos.
13. `audit_logs` — Trilha de auditoria de operações.

---

## 6. Mapeamento de Eventos e Mensageria (RabbitMQ)

### Eventos Publicados
| Evento | Exchange | Routing Key | Origem Atual | Consumidores Externos |
| :--- | :--- | :--- | :--- | :--- |
| `PatientCreatedEvent` | `patient.events` | `patient.created` | `patient-service` | `clinical-service` (projeção) |
| `PatientUpdatedEvent` | `patient.events` | `patient.updated` | `patient-service` | `clinical-service` (projeção) |
| `AllergyAddedEvent` | `patient.events` | `allergy.added` | `patient-service` | Auditoria / Alertas |
| `AppointmentCreatedEvent` | `clinical.events` | `appointment.created` | `clinical-service` | `reporting-service` |
| `AppointmentCancelledEvent` | `clinical.events` | `appointment.cancelled` | `clinical-service` | `reporting-service` |
| `MedicalRecordCreatedEvent` | `clinical.events` | `medical_record.created` | `clinical-service` | `ai-service`, `reporting-service` |
| `PrescriptionGeneratedEvent` | `clinical.events` | `prescription.generated` | `clinical-service` | `ai-service`, `reporting-service` |

### Eventos Consumidos
| Evento | Exchange | Routing Key | Consumidor Atual | Ação Executada |
| :--- | :--- | :--- | :--- | :--- |
| `UserCreatedEvent` | `iam.events` | `user.created` | `patient-service` | Cria perfil automático quando `role == "PATIENT"` |
| `UserDeactivatedEvent` | `iam.events` | `user.deactivated` | `patient-service` | Desativa o registro do paciente |
| `UserDeactivatedEvent` | `iam.events` | `user.deactivated` | `clinical-service` | Cancela consultas futuras do usuário desativado |
| `PatientCreatedEvent` | `patient.events` | `patient.created` | `clinical-service` | Realiza upsert em `patient_projections` |
| `PatientUpdatedEvent` | `patient.events` | `patient.updated` | `clinical-service` | Atualiza telefone/nome em `patient_projections` |

---

## 7. Mapeamento de Rotas e Gateway (Preservação Contratual)

Todas as rotas mapeadas no Gateway ([backend/gateway/app/main.py](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/backend/gateway/app/main.py)) devem permanecer idênticas durante e após a unificação:

### Rotas de Pacientes (`/api/v1/patients`)
- `GET /api/v1/patients` (Listagem paginada de pacientes com filtro por nome/CPF)
- `POST /api/v1/patients` (Criação de perfil de paciente)
- `GET /api/v1/patients/me` (Dados do próprio paciente autenticado)
- `GET /api/v1/patients/{patient_id}` (Detalhes do paciente)
- `GET /api/v1/patients/{patient_id}/summary` (Sumário clínico consolidado)
- `PUT /api/v1/patients/{patient_id}` (Atualização cadastral)
- `DELETE /api/v1/patients/{patient_id}` (Desativação lógica)
- `POST /api/v1/patients/{patient_id}/anonymize` (Anonimização LGPD)
- `GET|POST /api/v1/patients/{patient_id}/allergies` (Alergias)
- `DELETE /api/v1/patients/{patient_id}/allergies/{allergy_id}`
- `GET|POST /api/v1/patients/{patient_id}/vaccines` (Vacinas)
- `DELETE /api/v1/patients/{patient_id}/vaccines/{vaccine_id}`
- `GET|POST /api/v1/patients/{patient_id}/medications` (Medicamentos contínuos)
- `PUT /api/v1/patients/{patient_id}/medications/{med_id}`
- `POST /api/v1/patients/{patient_id}/medications/{med_id}/deactivate`
- `POST /api/v1/patients/{patient_id}/medications/{med_id}/reactivate`
- `DELETE /api/v1/patients/{patient_id}/medications/{med_id}`
- `GET /api/v1/patients/{patient_id}/medications/history` (Auditoria de medicamentos)
- `POST /api/v1/patients/{patient_id}/documents/upload` (Upload S3)
- `GET /api/v1/patients/{patient_id}/documents` (Listagem de documentos)
- `GET /api/v1/patients/{patient_id}/documents/{document_id}`
- `GET /api/v1/patients/{patient_id}/documents/{document_id}/download` (Presigned URL)
- `DELETE /api/v1/patients/{patient_id}/documents/{document_id}` (Soft delete)

### Rotas Clínicas (`/api/v1/appointments`, `/api/v1/schedules`, `/api/v1/records`)
- `GET|POST /api/v1/appointments` (Agendamentos de consultas)
- `GET /api/v1/appointments/{appointment_id}`
- `PUT /api/v1/appointments/{appointment_id}/cancel` (Cancelamento)
- `PUT /api/v1/appointments/{appointment_id}/confirm` (Confirmação)
- `PUT /api/v1/appointments/{appointment_id}/complete` (Conclusão)
- `POST /api/v1/records` (Criação de prontuário)
- `GET|PUT /api/v1/records/{record_id}` (Leitura e atualização de prontuário)
- `POST /api/v1/records/{record_id}/sign` (Assinatura digital com hash SHA-256)
- `GET /api/v1/records/{record_id}/signature/verify` (Verificação de integridade)
- `GET /api/v1/records/{record_id}/history` (Auditoria de prontuário)
- `GET /api/v1/records` e `GET /api/v1/records/patient/{patient_id}`
- `POST /api/v1/records/{record_id}/prescriptions` (Criação de prescrição)
- `GET /api/v1/records/{record_id}/prescriptions/{prescription_id}/pdf/download`
- `GET /api/v1/records/{record_id}/prescriptions/{prescription_id}/history`
- `GET /api/v1/records/{record_id}/prescriptions/{prescription_id}/pdf`
- `POST /api/v1/records/{record_id}/certificates` (Emissão de atestado médico)
- `GET /api/v1/records/{record_id}/certificates/{certificate_id}/pdf`
- `POST /api/v1/records/{record_id}/exams` (Solicitação de exames)
- `PUT /api/v1/records/{record_id}/exams/{exam_id}/result` (Registro de resultado)
- `GET /api/v1/records/{record_id}/exams/{exam_id}/history`

---

## 8. Infraestrutura de Containers e Portas

| Serviço / Container | Imagem / Build Context | Portas (Host : Container) | Finalidade |
| :--- | :--- | :--- | :--- |
| `gateway` | `gateway/Dockerfile` | `8000:8000` | Ponto de entrada unificado, auth JWT e proxy |
| `patient-service` | `patient-service/Dockerfile` | Interna `8000` | API legada de pacientes |
| `clinical-service` | `clinical-service/Dockerfile` | Interna `8000` | API legada clínica |
| `clinical-worker` | `clinical-service/Dockerfile.worker` | Sem portas externas | Worker Celery para geração de PDFs |
| `db-patient` | `postgres:15-alpine` | Interna `5432` | Banco PostgreSQL `patient_db` |
| `db-clinical` | `postgres:15-alpine` | Interna `5432` | Banco PostgreSQL `clinical_db` |
| `redis` | `redis:7-alpine` | `6379:6379` | Cache do Gateway e broker Celery |
| `rabbitmq` | `rabbitmq:3.13-management-alpine` | `5672:5672`, `15672:15672` | Broker AMQP e console web |
| `minio` | `minio/minio:latest` | `9000:9000`, `9001:9001` | Object Storage S3 compatível |

---

## 9. Baseline e Diagnóstico da Execução de Testes

Execução dos testes em ambiente isolado antes de qualquer alteração:

### Resumo Quantitativo
- **Total de Testes Aprovados no Estado Inicial:** **40 testes**
- **Total de Falhas e Erros Pré-existentes Identificados:** **18 itens**

### Detalhamento por Suíte de Teste

| Arquivo de Teste | Aprovados | Falhas | Causa Raiz da Falha Pré-existente |
| :--- | :---: | :---: | :--- |
| `patient-service/tests/test_patient.py` | 0 | 1 | **Erro de Sintaxe / Arquivo Corrompido:** A linha 65 contém caracteres nulos (*null bytes*) e formatação espaçada gerando erro de coleção no pytest. |
| `patient-service/tests/test_documents.py` | 0 | 8 | **Erro de Fixture:** A fixture `mock_patient` é invocada como função direta `mock_patient()` em vez de ser injetada como parâmetro nas demais fixtures. |
| `patient-service/tests/test_medication_history.py` | 5 | 4 | **Falha de Mock em Asserções:** Em testes de update/deactivate, asserções como `assert result.dosage == "100mg"` comparam o retorno com o objeto mock original. |
| `clinical-service/tests/test_clinical.py` | **9** | **0** | **100% de aprovação** nos testes unitários e de integração de consultas. |
| `clinical-service/tests/test_rich_notes_and_signature.py` | **18** | 1 | **Bug na Sanitização HTML:** `_sanitize_rich_notes` faz `.replace('<', '<')` sem escapar para `&lt;`, falhando no teste `test_sanitize_escapes_html_tags`. |
| `clinical-service/tests/test_prescription_pdf.py` | **8** | 4 | **Dependências Nativas e Mock:** 3 testes falham pela ausência de bibliotecas de sistema C (*GObject/Pango* do WeasyPrint) no host Windows (presentes no Dockerfile); 1 teste falha por `TypeError: 'UUID' object is not subscriptable` no mock da linha 156 (`uuid.uuid4()[:2]`). |

---

## 10. Conclusão da Fase 0 e Próximos Passos

A **Fase 0 (Baseline e Inventário)** foi concluída satisfatoriamente. O mapeamento assegura:
1. Conhecimento integral de todas as 18 tabelas de domínio, 2 buckets S3, 7 eventos de mensageria e mais de 40 rotas HTTP.
2. Identificação clara de todos os testes preexistentes aprovados e dos que continham falhas conhecidas antes da migração.
3. Garantia de que nenhuma linha de código em `patient-service` ou `clinical-service` foi modificada nesta fase.

Estamos prontos para prosseguir para a **Fase 1 — Criação da estrutura base de `backend/patient-clinical-service`**.
