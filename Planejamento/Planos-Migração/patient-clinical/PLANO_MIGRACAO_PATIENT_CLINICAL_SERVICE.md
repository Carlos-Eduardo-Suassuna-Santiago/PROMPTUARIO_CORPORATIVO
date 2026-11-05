# Plano de Migração do Patient-Clinical Service: Python (FastAPI) para Java (Spring Boot)

Este documento detalha o passo a passo completo e o mapeamento técnico necessário para migrar o **Patient-Clinical Service** da sua stack em Python (FastAPI / SQLAlchemy / Celery) para a stack corporativa exigida no Projeto Integrador: **Java 21 (LTS) e Spring Boot 3.x**.

> **Atualização Arquitetural (Pós-Unificação Fase 12):**  
> Os microsserviços legados `patient-service` e `clinical-service` foram fundidos em um único microsserviço canônico: **`patient-clinical-service`**.  
> O modelo de dados agora compartilha o banco relacional unificado `patient_clinical_db`, eliminando a tabela intermediária `patient_projections` e chamadas HTTP internas. A migração para Java contempla toda essa arquitetura reformulada.

---

## 1. Mapeamento de Stack Tecnológica

Equivalência 1-para-1 de cada tecnologia que compõe o serviço unificado atualmente:

| Componente | Atual (Python) | Destino (Java 21 / Spring Boot 3.x) |
| :--- | :--- | :--- |
| **Linguagem Base** | Python 3.12 | Java 21 (LTS) |
| **Framework Web** | FastAPI + Uvicorn | Spring Boot 3.3+ (Spring Web / MVC) |
| **Gerenciador de Dependências** | pip / requirements.txt | Maven (`pom.xml`) |
| **ORM / Banco de Dados** | SQLAlchemy 2.0 (Async/Sync) | Spring Data JPA / Hibernate 6.x |
| **Migrações de Banco** | Alembic | Flyway (`V1__init_unified_schema.sql`) |
| **Serialização e Validação** | Pydantic v2 | Java Records + Jakarta Validation (`@Valid`, `@NotNull`, etc.) |
| **Storage S3 (MinIO)** | boto3 / botocore | AWS SDK for Java 2.x (`S3Client`) |
| **Buckets de Armazenamento** | `patient-documents` e `prescriptions` | `patient-documents` e `prescriptions` (gerenciados via `@ConfigurationProperties`) |
| **Mensageria (RabbitMQ)** | aio-pika / pika | Spring AMQP (`RabbitTemplate` e `@RabbitListener`) |
| **Processamento Assíncrono / Worker** | Celery + Redis (DB 3) | Spring AMQP Worker (`@RabbitListener`) ou `@Async` com ThreadPool dedicada |
| **Geração de PDF (Prescrições/Atestados)** | WeasyPrint / Jinja2 | OpenPDF / Flying Saucer / Thymeleaf |
| **Autenticação e RBAC** | python-jose / FastAPI Depends | Spring Security (OAuth2 Resource Server / JWT) |
| **Configuração e Variáveis de Ambiente** | pydantic-settings (`Settings`) | `application.yml` + `@ConfigurationProperties` |
| **Observabilidade e Métricas** | Prometheus FastApi Instrumentator | Spring Boot Actuator + Micrometer Prometheus |

---

## 2. Estrutura de Diretórios Equivalente (Modular Monolith / Clean Architecture)

O serviço unificado organiza os domínios de **Patient** e **Clinical** em contextos delimitados (*bounded contexts*), mantendo alta coesão e baixo acoplamento:

### Origem (Python / FastAPI):
```text
backend/patient-clinical-service/app/
├── api/
│   ├── patient_routers.py       # Endpoints /api/v1/patients/*
│   └── clinical_routers.py      # Endpoints /api/v1/appointments, records, schedules
├── config.py                    # Configurações unificadas (DB, S3, RabbitMQ, JWT, Celery)
├── domain/
│   ├── models/
│   │   ├── patient.py           # Patient, Allergy, Vaccine, ContinuousMedication, etc.
│   │   └── clinical.py          # Appointment, MedicalRecord, Prescription, ExamRequest, etc.
│   ├── schemas/
│   │   ├── patient_schemas.py   # Schemas Pydantic de entrada/saída de pacientes
│   │   └── clinical_schemas.py  # Schemas Pydantic clínicos, SOAP, prescrições
│   └── services/
│       ├── patient_service.py   # Regras de negócio de pacientes, upload S3
│       └── clinical_service.py  # Regras de agendamento, prontuários, emissão de receitas
├── infrastructure/
│   ├── database.py              # Engine e sessão SQLAlchemy
│   ├── events/
│   │   ├── consumers.py         # Consumidor de eventos do IAM (UserDeactivated, etc.)
│   │   └── publishers.py        # Publicador de eventos de domínio RabbitMQ
│   ├── repositories/
│   │   ├── patient_repository.py
│   │   └── clinical_repository.py
│   └── s3/                      # Cliente MinIO/S3
└── workers/
    ├── celery_app.py            # App Celery integrado
    └── tasks.py                 # Geração assíncrona de PDF de prescrições
```

### Destino (Java 21 / Spring Boot 3.x):
```text
backend-java/patient-clinical-service/
├── pom.xml
└── src/main/java/br/edu/ifrn/promptuario/
    ├── PatientClinicalApplication.java
    ├── config/
    │   ├── SecurityConfig.java         # Spring Security OAuth2 JWT Resource Server
    │   ├── S3Config.java               # Configuração do Bean S3Client (MinIO)
    │   ├── RabbitMQConfig.java         # Exchanges, Filas e Jackson2MessageConverter
    │   └── AsyncWorkerConfig.java      # Configuração de ThreadPool e workers assíncronos
    ├── modules/
    │   ├── patient/
    │   │   ├── controller/             # PatientController (/api/v1/patients)
    │   │   ├── model/                  # Patient, Allergy, Vaccine, Medication, PatientDocument
    │   │   ├── dto/                    # Java Records de request/response
    │   │   ├── repository/             # Spring Data JPA Repositories
    │   │   └── service/                # PatientService, PatientDocumentService
    │   └── clinical/
    │       ├── controller/             # AppointmentController, MedicalRecordController, ScheduleController
    │       ├── model/                  # MedicalRecord, Prescription, ExamRequest, Appointment, etc.
    │       ├── dto/                    # Java Records (SOAP, PrescriptionRequest, etc.)
    │       ├── repository/             # Spring Data JPA Repositories
    │       └── service/                # ClinicalService, AppointmentService, PrescriptionPdfService
    └── shared/
        ├── event/                      # Records de Eventos (PatientCreated, MedicalRecordCreated, etc.)
        ├── exception/                  # GlobalExceptionHandler (@RestControllerAdvice)
        ├── messaging/                  # EventPublisher e EventConsumers (IAM listener)
        └── storage/                    # S3StorageService genérico
```

---

## 3. Mapeamento de Modelos de Dados (PostgreSQL `patient_clinical_db`)

Com a unificação arquitetural, todas as tabelas residem no mesmo esquema relacional. O Spring Data JPA mapeará as seguintes entidades:

### Contexto de Pacientes (`patient`):
1. **`Patient` (`patients`):** ID (UUID), `user_id` (UUID), `cpf` (único), `name`, `birth_date`, `gender`, `phone`, `blood_type`, status de auditoria e flags de ativação.
2. **`Allergy` (`allergies`):** `@ManyToOne` com `Patient`, alergeno, severidade (`AllergySeverity` Enum), reações.
3. **`Vaccine` (`vaccines`):** `@ManyToOne` com `Patient`, nome da vacina, data de aplicação, lote, dose.
4. **`ContinuousMedication` (`continuous_medications`):** Medicamento de uso contínuo, dosagem, posologia.
5. **`MedicationHistory` (`medication_history`):** Histórico de alterações de medicação (`MedicationChangeType`).
6. **`PatientDocument` (`patient_documents`):** Metadados e S3 key no bucket `patient-documents`.

### Contexto Clínico (`clinical`):
1. **`Appointment` (`appointments`):**
   - `@ManyToOne` direto com `Patient` (eliminando projeções legadas).
   - Relacionamento com `doctor_id`, data/hora, tipo de consulta (`AppointmentType`), status (`AppointmentStatus`).
2. **`DoctorSchedule` (`doctor_schedules`) & `TimeSlot` (`time_slots`):**
   - Grade de horários e slots de atendimento disponíveis por médico.
3. **`MedicalRecord` (`medical_records`):**
   - Prontuário clínico estruturado em SOAP (Subjetivo, Objetivo, Avaliação, Plano).
   - `@ManyToOne` com `Patient`, `appointment_id`, `doctor_id`.
   - Campo `diagnosis_codes` mapeado como JSON (`@JdbcTypeCode(SqlTypes.JSON)`).
4. **`MedicalRecordHistory` (`medical_record_histories`):** Auditoria imutável de versões de prontuário.
5. **`Prescription` (`prescriptions`) & `PrescriptionHistory`:**
   - Itens de prescrição estruturados como JSON (`items`).
   - Campo `pdf_url` apontando para o bucket `prescriptions`.
   - Hash e status de assinatura digital.
6. **`ExamRequest` (`exam_requests`) & `ExamRequestHistory`:**
   - Solicitação de exames, justificativa clínica e urgência (`ExamUrgency`).
7. **`MedicalCertificate` (`medical_certificates`) & `MedicalCertificateHistory`:**
   - Atestados médicos com CID, período de repouso e validação de autenticidade.

---

## 4. Endpoints REST Preservados (Contrato de API)

Todas as rotas expostas pelo FastAPI permanecem **estritamente inalteradas**, garantindo que o **API Gateway** continue roteando sem quebra de contrato:

| Método | Endpoint | Descrição / Regra de Negócio |
| :--- | :--- | :--- |
| `GET` | `/healthz` | Health check consumido pelo agregador do Gateway |
| `GET` | `/api/v1/patients` | Listagem paginada de pacientes (filtros por nome, CPF) |
| `POST` | `/api/v1/patients` | Cadastro de paciente (validação de CPF e disparo de `PatientCreatedEvent`) |
| `GET` | `/api/v1/patients/me` | Dados do paciente autenticado (extraído da claim do JWT) |
| `GET` | `/api/v1/patients/{id}` | Busca de paciente por ID com validação de permissões RBAC |
| `POST` | `/api/v1/patients/{id}/allergies` | Inclusão de alergia (dispara `AllergyAddedEvent`) |
| `POST` | `/api/v1/patients/{id}/documents/upload` | Upload multipart de arquivo para MinIO (`patient-documents`) |
| `GET` | `/api/v1/appointments` | Listagem de agendamentos com filtros por médico, paciente e período |
| `POST` | `/api/v1/appointments` | Agendamento de consulta com validação de conflito de agenda |
| `PATCH` | `/api/v1/appointments/{id}/cancel` | Cancelamento de consulta (valida antecedência mínima configurada) |
| `POST` | `/api/v1/records` | Registro de prontuário SOAP, prescrições e exames em transação única |
| `GET` | `/api/v1/records/patient/{patient_id}` | Histórico clínico consolidado do paciente |
| `POST` | `/api/v1/schedules` | Criação de grade de disponibilidade do médico |
| `GET` | `/api/v1/schedules/available` | Consulta de horários livres para atendimento |

---

## 5. Eventos RabbitMQ (Event-Driven Architecture)

O serviço em Spring Boot assume o papel unificado de publicação e consumo de eventos do ecossistema:

### Eventos Publicados:
- `PatientCreatedEvent` (`routing_key: patient.created`)
- `PatientUpdatedEvent` (`routing_key: patient.updated`)
- `AllergyAddedEvent` (`routing_key: patient.allergy.added`)
- `AppointmentCreatedEvent` (`routing_key: appointment.created`)
- `AppointmentCancelledEvent` (`routing_key: appointment.cancelled`)
- `MedicalRecordCreatedEvent` (`routing_key: medical_record.created`)
- `PrescriptionGeneratedEvent` (`routing_key: prescription.generated`)

> Esses eventos são consumidos pelo `ai-service` (para análises assíncronas) e pelo `reporting-service` (para consolidação analítica).

### Eventos Consumidos:
- `UserDeactivatedEvent` (`exchange: iam.events`):
  - Ao receber o evento, localiza o paciente associado de forma transacional.
  - Inativa o paciente, cancela consultas futuras agendadas e registra o log de auditoria correspondente (`@Transactional`).

---

## 6. Processamento Assíncrono e Geração de PDFs (Substituição do Celery)

No serviço Python, a tarefa `clinical.generate_prescription_pdf` era despachada via Celery usando o Redis DB 3.  
Na implementação em Spring Boot:
1. **Fila RabbitMQ Dedicada:** Disparo de mensagens na fila `prescription.pdf.generation`.
2. **Worker Spring AMQP (`@RabbitListener`):**
   - Consome a mensagem com o ID da prescrição.
   - Renderiza o layout HTML/CSS da receita usando Thymeleaf.
   - Converte o template para PDF com **OpenPDF** ou **Flying Saucer**.
   - Faz o upload do binário para o bucket `prescriptions` usando o `S3Client`.
   - Gera uma pré-assinada URL temporária e atualiza o registro no banco via Spring Data JPA.
3. **Alternativa `@Async`:** Se desejado manter o processamento estritamente local dentro do container da API, utiliza-se `@Async("prescriptionTaskExecutor")` com persistência assíncrona.

---

## 7. Passo a Passo da Refatoração (Roteiro de Implementação)

### Passo 1: Inicialização do Projeto Spring Boot
- Criar o projeto base em `backend-java/patient-clinical-service` com Spring Boot 3.3+ e Java 21.
- Dependências: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `spring-boot-starter-validation`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-amqp`, `spring-boot-starter-actuator`, `software.amazon.awssdk:s3`, `com.github.librepdf:openpdf`.

### Passo 2: Banco de Dados e Migração Flyway
- Criar a migração `V1__init_patient_clinical_schema.sql` refletindo fielmente o DDL do banco `patient_clinical_db` gerado na unificação (tabelas `patients`, `appointments`, `medical_records`, `prescriptions`, etc.).

### Passo 3: Entidades JPA e Repositórios
- Implementar as entidades JPA em pacotes modulares (`patient/model` e `clinical/model`).
- Configurar conversores para colunas JSON (`@JdbcTypeCode(SqlTypes.JSON)`) e Enums de domínio.
- Criar interfaces de repositório estendendo `JpaRepository<T, UUID>` com métodos de consulta otimizados (`@Query`).

### Passo 4: DTOs e Validação de Contrato
- Traduzir os schemas Pydantic para Java Records imutáveis.
- Adicionar anotações de validação (`@NotBlank`, `@NotNull`, `@Pattern`, `@Size`) e mensagens de erro padronizadas.

### Passo 5: Camada de Serviços e Transacionalidade
- Implementar `PatientService` e `ClinicalService` anotados com `@Service`.
- Aplicar `@Transactional` em fluxos críticos de negócio:
  - Criação de prontuário + histórico + publicação de evento.
  - Cancelamento em lote de agendamentos em caso de desativação de usuário.

### Passo 6: Integrações de Infraestrutura (S3 e RabbitMQ)
- Criar `S3StorageService` gerenciando os buckets `patient-documents` e `prescriptions`.
- Implementar publishers e listeners com `RabbitTemplate` e `@RabbitListener`, garantindo idempotência no consumo.

### Passo 7: Controladores REST e Tratamento de Exceções
- Mapear os endpoints em `PatientController`, `AppointmentController`, `MedicalRecordController`, `DoctorScheduleController`.
- Criar classe `@RestControllerAdvice` para formatar erros 400, 404, 409 e 500 no formato JSON esperado pelo Gateway e pelo Frontend React.

### Passo 8: Segurança (Spring Security OAuth2 Resource Server)
- Configurar o `SecurityFilterChain` para decodificar e validar o token JWT (assinatura e claims) emitido pelo IAM Service.
- Aplicar autorizações refinadas via anotações `@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")`.

### Passo 9: Dockerização e Integração no Docker Compose
- Criar `Dockerfile` multi-stage com `eclipse-temurin:21-jdk` (build) e `eclipse-temurin:21-jre-alpine` (runtime).
- Atualizar a definição do serviço no `docker-compose.yml`:
  - Substituir o container Python pela imagem Java na porta interna `8000` (exposta na porta `8002` do host).
  - Configurar as variáveis de ambiente compatíveis (`SPRING_DATASOURCE_URL`, `SPRING_RABBITMQ_HOST`, etc.).

### Passo 10: Testes Automatizados e Homologação
- Implementar testes de integração com **JUnit 5**, **Mockito** e **Testcontainers** (PostgreSQL, RabbitMQ e MinIO local).
- Executar os testes de integração do Gateway (`backend/gateway/tests`) para comprovar que nenhuma chamada ao novo serviço em Java quebrou contratos.

---

## 8. Ordem Recomendada de Execução

1. **Setup Inicial & Configurações:** `pom.xml`, `application.yml` e classe `@SpringBootApplication`.
2. **Schema & Flyway:** Criação do script DDL unificado `V1__init_patient_clinical_schema.sql`.
3. **Mapeamento JPA:** Entidades `Patient` e `Clinical` + Repositórios Spring Data.
4. **Infraestrutura Básica:** Beans do `S3Client` e `RabbitMQ`.
5. **Regras de Pacientes (Módulo Patient):** DTOs, `PatientService` e `PatientController`.
6. **Regras Clínicas (Módulo Clinical):** DTOs, `ClinicalService`, prontuários, agendamentos e prescrições.
7. **Worker de PDF:** Implementação da geração assíncrona de receitas médicas.
8. **Segurança & Tratamento de Erros:** `SecurityConfig` e `@RestControllerAdvice`.
9. **Testes Unitários e de Integração:** Validação da paridade de comportamento.
10. **Corte e Validação com Gateway:** Troca do container no `docker-compose.yml` e validação com o Frontend.
