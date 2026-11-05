# Plano de Migração do Reporting Service: Python (FastAPI) para Java (Spring Boot)

Este documento detalha o passo a passo completo e o mapeamento técnico necessário para migrar o **Reporting Service** (serviço de relatórios, exportações e estatísticas analíticas) da sua stack atual em Python (FastAPI / SQLAlchemy / Celery / MinIO) para a stack corporativa exigida no Projeto Integrador: **Java 21 (LTS) e Spring Boot 3.x**.

---

## 1. Mapeamento de Stack Tecnológica

Equivalência 1-para-1 de cada tecnologia que compõe o serviço atualmente:

| Componente | Atual (Python) | Destino (Java 21 / Spring Boot 3.x) |
| :--- | :--- | :--- |
| **Linguagem Base** | Python 3.12 | Java 21 (LTS) |
| **Framework Web** | FastAPI + Uvicorn | Spring Boot 3.3+ (Spring Web / MVC) |
| **Gerenciador de Dependências** | pip / requirements.txt | Maven (`pom.xml`) |
| **ORM / Banco de Dados** | SQLAlchemy 2.0 (Async) | Spring Data JPA / Hibernate 6.x |
| **Banco de Dados Relacional** | PostgreSQL 15 (`reporting_db`) | PostgreSQL 15 (`reporting_db`) |
| **Migrações de Banco** | Alembic | Flyway (`V1__init_reporting_schema.sql`) |
| **Serialização e Validação** | Pydantic v2 | Java Records + Jakarta Validation (`@Valid`, `@NotNull`) |
| **Storage S3 (MinIO)** | boto3 / botocore | AWS SDK for Java 2.x (`S3Client` e `S3Presigner`) |
| **Bucket de Armazenamento** | `reports` | `reports` (gerenciado via `@ConfigurationProperties`) |
| **Processamento Assíncrono / Worker** | Celery 5 + Redis (DB 1) | Spring AMQP Worker (`@RabbitListener`) ou `@Async` com ThreadPool dedicada |
| **Agendamento Periódico (Cron)** | Celery Beat | Spring Task Scheduling (`@Scheduled`) com ThreadPoolTaskScheduler |
| **Geração de Arquivos CSV** | Módulo `csv` nativo | OpenCSV ou Apache Commons CSV |
| **Geração de Arquivos Excel (XLSX)** | openpyxl | Apache POI (`poi-ooxml`) |
| **Geração de Arquivos PDF** | WeasyPrint / ReportLab | OpenPDF ou Flying Saucer com Thymeleaf |
| **Mensageria (RabbitMQ)** | aio-pika / pika | Spring AMQP (`@RabbitListener` para consumo de eventos) |
| **Webhooks de Notificação** | httpx async | Spring `RestClient` com retry e timeout configurados |
| **Autenticação (JWT)** | python-jose / FastAPI Depends | Spring Security (OAuth2 Resource Server / JWT) |
| **Observabilidade e Métricas** | Prometheus FastApi Instrumentator | Spring Boot Actuator + Micrometer Prometheus |

---

## 2. Estrutura de Diretórios Equivalente (Clean Architecture)

A organização respeita os princípios de separação de responsabilidades e convenções do Spring Boot:

### Origem (Python / FastAPI):
```text
backend/reporting-service/app/
├── api/
│   └── routers.py              # Endpoints de exportação, estatísticas, webhooks
├── config.py                   # Configurações do serviço, MinIO, Redis, RabbitMQ
├── domain/
│   └── models/
│       └── report.py           # ReportJob, ReportSchedule, WebhookConfig, DailyStats, etc.
├── infrastructure/
│   ├── database.py             # Sessão SQLAlchemy
│   ├── events/
│   │   └── consumers.py        # Consumo de eventos para atualizar DailyStats
│   └── s3.py                   # Cliente MinIO S3
├── schemas/
│   └── report_schemas.py       # Schemas Pydantic
├── workers/
│   ├── celery_tasks.py         # Tarefas Celery de geração de relatórios
│   └── exporters/              # Lógica de montagem de CSV, XLSX e PDF
└── main.py                     # Inicialização da API e registro de rotas
```

### Destino (Java 21 / Spring Boot 3.x):
```text
backend-java/reporting-service/
├── pom.xml
└── src/main/java/br/edu/ifrn/promptuario/reporting/
    ├── ReportingServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java         # Validação de token JWT do IAM
    │   ├── S3Config.java               # Beans S3Client e S3Presigner (MinIO)
    │   ├── RabbitMQConfig.java         # Exchanges, Filas e Jackson2MessageConverter
    │   └── AsyncConfig.java            # ThreadPoolTaskExecutor e ThreadPoolTaskScheduler
    ├── controller/
    │   ├── ReportExportController.java # /api/v1/reports/export (solicitação e download)
    │   ├── DashboardStatsController.java# /api/v1/reports/consultations, summary, etc.
    │   ├── ScheduleController.java     # /api/v1/reports/schedules
    │   └── WebhookController.java      # /api/v1/reports/webhooks
    ├── model/
    │   ├── ReportJob.java              # Entidade JPA (tabela `report_jobs`)
    │   ├── ReportSchedule.java         # Entidade JPA (tabela `report_schedules`)
    │   ├── DailyStats.java             # Entidade JPA (tabela `daily_stats`)
    │   ├── WebhookConfig.java          # Entidade JPA (tabela `webhook_configs`)
    │   ├── WebhookDeliveryLog.java     # Entidade JPA (tabela `webhook_delivery_logs`)
    │   ├── ReportAuditLog.java         # Entidade JPA (tabela `report_audit_logs`)
    │   ├── ReportType.java             # Enum (CONSULTATIONS, PATIENTS, DOCTORS, etc.)
    │   ├── OutputFormat.java           # Enum (JSON, CSV, PDF, XLSX)
    │   └── ReportStatus.java           # Enum (PENDING, RUNNING, COMPLETED, FAILED)
    ├── dto/
    │   ├── ExportRequestDTO.java       # Record com parâmetros do relatório solicitado
    │   ├── JobStatusResponseDTO.java   # Record com status, progresso e link de download
    │   ├── DashboardSummaryDTO.java    # Record com totais e contagens do dashboard
    │   └── ScheduleCreateDTO.java      # Record para agendamento recorrente de relatório
    ├── repository/
    │   ├── ReportJobRepository.java    # JpaRepository<ReportJob, String/UUID>
    │   ├── DailyStatsRepository.java   # Consultas agregadas por data e tipo
    │   ├── ReportScheduleRepository.java
    │   ├── WebhookConfigRepository.java
    │   └── ReportAuditLogRepository.java
    ├── service/
    │   ├── ReportJobService.java       # Orquestração do ciclo de vida dos jobs
    │   ├── DashboardStatsService.java  # Leitura e agregações de métricas diárias
    │   ├── WebhookDeliveryService.java # Envio assíncrono de notificações HTTP
    │   └── ScheduleManagementService.java
    ├── worker/
    │   ├── ReportExecutionWorker.java  # Worker que processa o job (@Async ou @RabbitListener)
    │   └── exporters/
    │       ├── CsvReportExporter.java  # Montagem de CSV com OpenCSV
    │       ├── ExcelReportExporter.java# Montagem de planilhas XLSX com Apache POI
    │       └── PdfReportExporter.java  # Montagem de PDF com OpenPDF/Thymeleaf
    ├── messaging/
    │   └── DomainEventConsumer.java    # @RabbitListener atualizando DailyStats
    └── storage/
        └── S3ReportStorageService.java # Upload no bucket `reports` e pre-signed URLs
```

---

## 3. Mapeamento de Modelos de Dados (PostgreSQL `reporting_db`)

### 1. `ReportJob` (`report_jobs`)
- Identificador primário UUID (`id`), tipo de relatório (`report_type`), usuário solicitante (`requested_by`).
- Parâmetros estruturados em JSON (`parameters` - mapeado com `@JdbcTypeCode(SqlTypes.JSON)`).
- Status de execução (`status`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`).
- Formato de saída (`output_format`: `JSON`, `CSV`, `PDF`, `XLSX`).
- Localização no MinIO (`s3_key`), mensagem de erro caso falhe (`error_message`), total de registros gerados (`row_count`).
- Metadados de auditoria (`created_at`, `completed_at`).

### 2. `ReportSchedule` (`report_schedules`)
- Permite configurar exportações periódicas com expressões Cron (`cron_expression`).
- Parâmetros, formato de saída e lista de destinatários em JSON (`recipients`).
- Rastreamento de execução (`last_run_at`, `next_run_at`, `active`).

### 3. `DailyStats` (`daily_stats`)
- Tabela de estatísticas consolidadas por data (`stat_date` formato `YYYY-MM-DD`).
- Métrica analisada (`stat_type` - ex: `consultations_count`, `new_patients_count`, `cancellations_count`).
- Valor numérico acumulado (`value`) e metadados contextuais em JSON (`meta`).

### 4. `WebhookConfig` e `WebhookDeliveryLog`
- Permite que sistemas externos se inscrevam para receber notificação quando um job de relatório for concluído.
- Registro detalhado de tentativas, status HTTP de retorno e tempo de entrega.

### 5. `ReportAuditLog` (`report_audit_logs`)
- Trilha de auditoria imutável registrando quem solicitou ou baixou relatórios, alterações em agendamentos e entregas de webhooks.

---

## 4. Endpoints REST Preservados (Contratos de API)

Todas as rotas expostas pelo FastAPI permanecem **estritamente inalteradas**, garantindo total compatibilidade com o API Gateway e Frontend:

| Método | Endpoint | Papel Mínimo | Descrição |
| :--- | :--- | :--- | :--- |
| `GET` | `/healthz` | Público | Health check simples para o Gateway |
| `POST` | `/api/v1/reports/export` | `ADMIN`, `DOCTOR` | Cria um job assíncrono de exportação de relatório |
| `GET` | `/api/v1/reports/export/{job_id}` | Autenticado | Retorna o status atual do job (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`) |
| `GET` | `/api/v1/reports/export/{job_id}/download` | Autenticado | Redireciona para URL pré-assinada de download no MinIO ou entrega link |
| `GET` | `/api/v1/reports/consultations` | `ADMIN`, `DOCTOR` | Estatísticas diárias de consultas agregadas |
| `GET` | `/api/v1/reports/patients` | `ADMIN` | Estatísticas diárias de novos pacientes |
| `GET` | `/api/v1/reports/doctors` | `ADMIN` | Produtividade e consultas realizadas por médico |
| `GET` | `/api/v1/reports/summary` | `ADMIN` | Resumo analítico rápido para cards do Dashboard |
| `GET` | `/api/v1/reports/schedules` | `ADMIN` | Lista agendamentos recorrentes de relatórios |
| `POST` | `/api/v1/reports/schedules` | `ADMIN` | Cria novo agendamento recorrente com expressão Cron |
| `GET` | `/api/v1/reports/webhooks` | `ADMIN` | Lista webhooks de notificação cadastrados |
| `POST` | `/api/v1/reports/webhooks` | `ADMIN` | Registra novo endpoint webhook |

---

## 5. Processamento Assíncrono e Workers (Substituição do Celery)

### 1. Despacho e Execução dos Jobs:
- Ao receber `POST /api/v1/reports/export`, a API cria o `ReportJob` no banco com status `PENDING` e publica mensagem na fila RabbitMQ `reporting.job.queue`.
- O worker Spring AMQP (`@RabbitListener(queues = "reporting.job.queue")`) recebe o `job_id`:
  1. Atualiza status para `RUNNING`.
  2. Consulta os dados necessários nas bases analíticas ou réplicas de leitura.
  3. Aciona o exportador correspondente (`CsvReportExporter`, `ExcelReportExporter` ou `PdfReportExporter`).
  4. Realiza o upload do stream resultante no bucket `reports` do MinIO via AWS SDK 2.x `S3Client`.
  5. Atualiza o job para `COMPLETED`, preenchendo a `s3_key`, quantidade de linhas e data de término.
  6. Se houver webhooks registrados para o evento, aciona o `WebhookDeliveryService`.

### 2. Agendamentos Recorrentes (Substituição do Celery Beat):
- Implementação de um agendador gerenciado por `@Scheduled(fixedRate = 60000)` que lê `report_schedules`, avalia as expressões Cron ativas que coincidem com o minuto atual e dispara a criação do job correspondente.

---

## 6. Consumo de Eventos de Domínio (RabbitMQ)

O serviço consome eventos emitidos por outros microsserviços para alimentar os dados em tempo real de `DailyStats`:

- **Eventos Consumidos:**
  - `patient.created` -> Incrementa `new_patients_count` para a data do dia.
  - `appointment.created` -> Incrementa `appointments_scheduled` para a data da consulta.
  - `appointment.cancelled` -> Incrementa `appointments_cancelled`.
  - `medical_record.created` -> Incrementa `consultations_completed` e atualiza produtividade do médico.
- **Idempotência:** Toda atualização de `DailyStats` utiliza operação atômica de incremento condicional ou controle por identificador de evento processado.

---

## 7. Passo a Passo da Refatoração

### Passo 1: Inicialização do Projeto
- Criar projeto base Maven com Spring Boot 3.3+, dependências: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `spring-boot-starter-amqp`, `spring-boot-starter-validation`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-actuator`, `software.amazon.awssdk:s3`, `org.apache.poi:poi-ooxml`, `com.opencsv:opencsv`, `com.github.librepdf:openpdf`.

### Passo 2: Script Flyway do Banco `reporting_db`
- Criar `V1__init_reporting_schema.sql` com as tabelas `report_jobs`, `report_schedules`, `daily_stats`, `webhook_configs`, `webhook_delivery_logs` e `report_audit_logs`.

### Passo 3: Entidades JPA e Repositórios
- Implementar as entidades JPA em `model/` com mapeamento adequado de JSON e Enums.
- Criar repositórios com queries personalizadas de consolidação estatística por período.

### Passo 4: Serviços de Exportação (CSV, Excel e PDF)
- Implementar a geração de planilhas Excel estruturadas e estilizadas com Apache POI.
- Implementar a geração de arquivos CSV de alta performance com OpenCSV.
- Implementar a geração de relatórios tabulares em PDF com OpenPDF.

### Passo 5: Integração com MinIO (AWS SDK 2.x)
- Implementar `S3ReportStorageService` para persistência dos relatórios gerados e geração de URLs pré-assinadas com validade controlada (ex: 1 hora) via `S3Presigner`.

### Passo 6: Mensageria e Workers
- Configurar `@RabbitListener` para o processamento assíncrono dos jobs e para o consumo dos eventos de domínio do ecossistema.

### Passo 7: Controladores REST e Tratamento de Exceções
- Mapear os endpoints com anotações de validação e tratamento global de exceções (`@RestControllerAdvice`).

### Passo 8: Dockerização e Integração com Docker Compose
- Criar `Dockerfile` multi-stage com Java 21 JRE Alpine.
- Substituir o serviço `reporting-service` no `docker-compose.yml` expondo a porta interna `8000` (porta `8005` no host).

### Passo 9: Testes Automatizados
- Testes unitários com JUnit 5 cobrindo a geração de cada formato de arquivo.
- Testes de integração com Testcontainers (PostgreSQL, RabbitMQ e MinIO) validando o ciclo completo: solicitação -> processamento do worker -> upload S3 -> link de download.
