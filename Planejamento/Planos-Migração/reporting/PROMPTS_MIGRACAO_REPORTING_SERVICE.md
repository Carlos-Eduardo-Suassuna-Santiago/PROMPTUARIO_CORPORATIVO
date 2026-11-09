# Guia de Prompts de Implementação: Migração do Reporting Service (FastAPI -> Spring Boot 3.x / Java 21)

Este documento contém o conjunto completo de **prompts técnicos estruturados e detalhados**, organizados em **9 fases sequenciais**, para guiar a implementação assistida por IA da migração do **Reporting Service** (serviço analítico, consolidação de métricas, exportações em múltiplos formatos, webhooks e agendamento) de Python (FastAPI / SQLAlchemy / Celery / Redis / WeasyPrint / openpyxl) para a stack corporativa padronizada: **Java 21 (LTS), Spring Boot 3.3+, Spring Data JPA / Hibernate 6, Apache POI, OpenCSV, OpenPDF, Spring AMQP (RabbitMQ) e AWS SDK for Java 2.x (MinIO)**.

O conteúdo foi desenhado com base no documento [PLANO_MIGRACAO_REPORTING_SERVICE.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/Planos-Migra%C3%A7%C3%A3o/reporting/PLANO_MIGRACAO_REPORTING_SERVICE.md) e nos contratos analíticos do PROMPTUÁRIO.

---

## Índice das Fases de Implementação

1. [Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base](#fase-1-bootstrap-do-projeto-dependências-maven-e-configurações-base)
2. [Fase 2: Migração Flyway e Schema do Banco de Dados (`reporting_db`)](#fase-2-migração-flyway-e-schema-do-banco-de-dados-reporting_db)
3. [Fase 3: Entidades JPA, Enums e Repositórios Spring Data (Jobs, Stats e Webhooks)](#fase-3-entidades-jpa-enums-e-repositórios-spring-data-jobs-stats-e-webhooks)
4. [Fase 4: Java Records (DTOs) e Validações de Contrato Jakarta](#fase-4-java-records-dtos-e-validações-de-contrato-jakarta)
5. [Fase 5: Motores de Exportação Multi-Formato (CSV, Excel XLSX e PDF)](#fase-5-motores-de-exportação-multi-formato-csv-excel-xlsx-e-pdf)
6. [Fase 6: Integração com Storage S3 (MinIO) e Geração de URLs Pré-Assinadas](#fase-6-integração-com-storage-s3-minio-e-geração-de-urls-pré-assinadas)
7. [Fase 7: Worker Assíncrono de Relatórios, Agendamento Cron e Notificações Webhook](#fase-7-worker-assíncrono-de-relatórios-agendamento-cron-e-notificações-webhook)
8. [Fase 8: Mensageria RabbitMQ e Atualização Contínua de Métricas Diárias (DailyStats)](#fase-8-mensageria-rabbitmq-e-atualização-contínua-de-métricas-diárias-dailystats)
9. [Fase 9: Controladores REST, Segurança OAuth2 JWT, Docker Multi-Stage e Testes](#fase-9-controladores-rest-segurança-oauth2-jwt-docker-multi-stage-e-testes)

---

## Diretrizes Gerais para o Ciclo de Execução

- **Substituição Definitiva do Celery / Redis:** O processamento assíncrono de relatórios é transferido para filas dedicadas do RabbitMQ (`reporting.job.queue`) consumidas por `@RabbitListener` ou executores `@Async("reportTaskExecutor")`, eliminando a dependência do Redis DB 1 e do Celery Beat.
- **Armazenamento de Relatórios no MinIO:** Os artefatos gerados (CSV, XLSX, PDF, JSON) são persistidos no bucket `reports` sob a chave `reports/{report_type}/{job_id}.{ext}`. O download é disponibilizado via URL pré-assinada (`S3Presigner`) com expiração de 1 hora.
- **Agregação em Tempo Real via Eventos:** A tabela `daily_stats` consolida estatísticas em tempo real consumindo eventos do ecossistema (`patient.created`, `appointment.created`, `appointment.cancelled`, `medical_record.created`), evitando consultas pesadas de contagem nos bancos transacionais em horário de pico.
- **Porta Canônica:** O Reporting Service opera internamente na **porta 8000** e é mapeado no host na **porta 8005** (`8005:8000`).

---

## Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base

### Objetivo
Criar a base do novo microsserviço `backend-java/reporting-service` com Java 21 e Spring Boot 3.3+, configurando o `pom.xml` com as bibliotecas especializadas de geração de documentos (Apache POI, OpenCSV, OpenPDF), cliente AWS SDK v2 para S3, RabbitMQ, PostgreSQL e Actuator.

### Arquivos Alvo
- `backend-java/reporting-service/pom.xml`
- `backend-java/reporting-service/src/main/resources/application.yml`
- `backend-java/reporting-service/src/main/java/br/edu/ifrn/promptuario/reporting/ReportingServiceApplication.java`
- `backend-java/reporting-service/src/main/java/br/edu/ifrn/promptuario/reporting/config/AsyncConfig.java`

### Prompt da Fase 1

```text
Atue como Engenheiro de Software Sênior especialista em Java 21 e Spring Boot 3.3+.
Vamos criar a base estrutural do microsserviço corporativo "reporting-service" em "backend-java/reporting-service/".

Gere os arquivos estruturais iniciais do projeto:

1. "pom.xml":
   - Java 21, Spring Boot versão 3.3.4 (ou superior compatível).
   - GroupId: "br.edu.ifrn.promptuario"
   - ArtifactId: "reporting-service"
   - Dependências obrigatórias:
     * spring-boot-starter-web
     * spring-boot-starter-data-jpa
     * org.postgresql:postgresql (runtime)
     * org.flywaydb:flyway-core e org.flywaydb:flyway-database-postgresql
     * spring-boot-starter-validation
     * spring-boot-starter-security
     * spring-boot-starter-oauth2-resource-server
     * spring-boot-starter-amqp (RabbitMQ)
     * spring-boot-starter-actuator
     * io.micrometer:micrometer-registry-prometheus
     * software.amazon.awssdk:s3:2.25.0+ (AWS SDK v2 para MinIO/S3)
     * org.apache.poi:poi-ooxml:5.2.5 (geração de planilhas Excel XLSX)
     * com.opencsv:opencsv:5.9 (geração de CSV de alta performance)
     * com.github.librepdf:openpdf:1.3.39 (geração de relatórios PDF)
     * org.springframework.boot:spring-boot-starter-thymeleaf (templates de PDF)
     * org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0
     * org.projectlombok:lombok (scope provided)
     * spring-boot-starter-test, testcontainers (postgresql, rabbitmq, localstack) em escopo test.

2. "src/main/resources/application.yml":
   - Porta interna HTTP: 8000.
   - Datasource PostgreSQL para o banco "reporting_db":
     * url: "jdbc:postgresql://${DB_REPORTING_HOST:localhost}:5432/reporting_db"
     * username: "${DB_REPORTING_USER:reporting}"
     * password: "${DB_REPORTING_PASS:reporting_pass}"
   - JPA / Hibernate com dialect PostgreSQL, "ddl-auto: validate", "open-in-view: false".
   - Flyway ativado em "classpath:db/migration".
   - RabbitMQ:
     * host: "${RABBITMQ_HOST:localhost}"
     * port: 5672
     * queue: "reporting.job.queue"
     * exchange: "domain.events"
   - Storage S3 MinIO sob "app.storage.s3":
     * endpoint: "${S3_ENDPOINT:http://minio:9000}"
     * access-key: "${S3_ACCESS_KEY:promptuario}"
     * secret-key: "${S3_SECRET_KEY:promptuario_pass}"
     * bucket-reports: "${S3_BUCKET_REPORTS:reports}"
     * region: "us-east-1"
     * presigned-url-expiration-minutes: 60
   - Segurança JWT: "app.security.jwt.secret: ${JWT_SECRET_KEY:change-me-in-production-super-secret-key-min-32-chars}" com algoritmo HMAC-SHA256.
   - Actuator: expondo "health,info,prometheus" sob "/actuator".

3. "ReportingServiceApplication.java":
   - Classe principal com anotações @SpringBootApplication, @EnableAsync e @EnableScheduling.

4. "AsyncConfig.java" em "br.edu.ifrn.promptuario.reporting.config":
   - Configura ThreadPoolTaskExecutor com pool dedicado para processamento paralelo de exportações ("reportTaskExecutor") com corePoolSize=4, maxPoolSize=8, queueCapacity=50.
   - Configura ThreadPoolTaskScheduler para avaliação periódica dos agendamentos Cron.

Valide se a compilação "mvn clean compile" ocorre com sucesso.
```

### Critérios de Aceite
- Compilação executada com sucesso via `mvn clean compile`.
- Todas as dependências de manipulação de documentos (POI, OpenCSV, OpenPDF) integradas sem conflito de classpath.

---

## Fase 2: Migração Flyway e Schema do Banco de Dados (`reporting_db`)

### Objetivo
Criar o script DDL canônico `V1__init_reporting_schema.sql` para o PostgreSQL `reporting_db`, contendo as tabelas `report_jobs`, `report_schedules`, `daily_stats`, `webhook_configs`, `webhook_delivery_logs` e `report_audit_logs`, com suporte a tipos JSONB e índices analíticos por data e métrica.

### Arquivos Alvo
- `backend-java/reporting-service/src/main/resources/db/migration/V1__init_reporting_schema.sql`

### Prompt da Fase 2

```text
Atue como DBA Especialista em PostgreSQL 15 e Flyway.
Precisamos criar o script DDL inicial para o microsserviço "reporting-service" em "backend-java/reporting-service/src/main/resources/db/migration/V1__init_reporting_schema.sql".

O script deve conter:

1. Enums nativos ou constraints:
   - "report_type": ('CONSULTATIONS', 'PATIENTS', 'DOCTORS', 'PRESCRIPTIONS', 'CUSTOM', 'FULL_SYSTEM')
   - "output_format": ('JSON', 'CSV', 'PDF', 'XLSX')
   - "report_status": ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')

2. Tabela "report_jobs":
   - id: VARCHAR(36) PRIMARY KEY.
   - report_type: VARCHAR(30) NOT NULL.
   - requested_by: VARCHAR(36) NOT NULL.
   - parameters: JSONB NOT NULL DEFAULT '{}'::jsonb.
   - status: VARCHAR(20) NOT NULL DEFAULT 'PENDING'.
   - output_format: VARCHAR(10) NOT NULL DEFAULT 'JSON'.
   - result_data: JSONB NULL.
   - s3_key: VARCHAR(500) NULL.
   - error_message: TEXT NULL.
   - row_count: INT NOT NULL DEFAULT 0.
   - schedule_id: VARCHAR(36) NULL.
   - webhook_triggered: BOOLEAN NOT NULL DEFAULT FALSE.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.
   - completed_at: TIMESTAMPTZ NULL.

3. Tabela "report_schedules":
   - id: VARCHAR(36) PRIMARY KEY.
   - name: VARCHAR(120) NOT NULL.
   - report_type: VARCHAR(30) NOT NULL.
   - output_format: VARCHAR(10) NOT NULL DEFAULT 'XLSX'.
   - cron_expression: VARCHAR(100) NOT NULL.
   - parameters: JSONB NOT NULL DEFAULT '{}'::jsonb.
   - recipients: JSONB NOT NULL DEFAULT '[]'::jsonb.
   - active: BOOLEAN NOT NULL DEFAULT TRUE.
   - last_run_at: TIMESTAMPTZ NULL.
   - next_run_at: TIMESTAMPTZ NULL.
   - created_by: VARCHAR(36) NOT NULL.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.
   - updated_at: TIMESTAMPTZ NULL.

4. Tabela "daily_stats":
   - id: VARCHAR(36) PRIMARY KEY.
   - stat_date: VARCHAR(10) NOT NULL. -- formato YYYY-MM-DD
   - stat_type: VARCHAR(50) NOT NULL. -- CONSULTATIONS, NEW_PATIENTS, CANCELLATIONS, etc.
   - entity_id: VARCHAR(36) NULL.    -- ex: doctor_id
   - value: INT NOT NULL DEFAULT 0.
   - metadata: JSONB NOT NULL DEFAULT '{}'::jsonb.
   - updated_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.

5. Tabela "webhook_configs":
   - id: VARCHAR(36) PRIMARY KEY.
   - url: VARCHAR(500) NOT NULL.
   - secret: VARCHAR(128) NOT NULL.
   - description: VARCHAR(255) DEFAULT ''.
   - active: BOOLEAN NOT NULL DEFAULT TRUE.
   - max_retries: INT NOT NULL DEFAULT 3.
   - retry_interval_seconds: INT NOT NULL DEFAULT 60.
   - events: JSONB NOT NULL DEFAULT '[]'::jsonb.
   - last_delivery_at: TIMESTAMPTZ NULL.
   - last_success_at: TIMESTAMPTZ NULL.
   - consecutive_failures: INT NOT NULL DEFAULT 0.
   - created_by: VARCHAR(36) NOT NULL.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.
   - updated_at: TIMESTAMPTZ NULL.

6. Tabela "webhook_delivery_logs":
   - id: VARCHAR(36) PRIMARY KEY.
   - webhook_config_id: VARCHAR(36) NOT NULL REFERENCES webhook_configs(id) ON DELETE CASCADE.
   - job_id: VARCHAR(36) NOT NULL.
   - event_type: VARCHAR(50) NOT NULL.
   - url: VARCHAR(500) NOT NULL.
   - status_code: INT NULL.
   - response_body: TEXT NULL.
   - success: BOOLEAN NOT NULL DEFAULT FALSE.
   - attempt: INT NOT NULL DEFAULT 1.
   - error_message: TEXT NULL.
   - delivered_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.

7. Tabela "report_audit_logs":
   - id: VARCHAR(36) PRIMARY KEY.
   - event_type: VARCHAR(40) NOT NULL.
   - entity_type: VARCHAR(30) NOT NULL.
   - entity_id: VARCHAR(36) NULL.
   - description: VARCHAR(500) DEFAULT ''.
   - performed_by: VARCHAR(36) NOT NULL.
   - event_metadata: JSONB NOT NULL DEFAULT '{}'::jsonb.
   - ip_address: VARCHAR(45) NULL.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.

8. Índices Analíticos:
   - Índices em "daily_stats(stat_date, stat_type)", "daily_stats(entity_id)".
   - Índices em "report_jobs(status)", "report_jobs(created_at)", "report_jobs(requested_by)".
   - Índices em "webhook_delivery_logs(job_id)", "report_audit_logs(created_at)".

Gere o script SQL completo e sintaticamente válido para execução no Flyway.
```

### Critérios de Aceite
- Execução limpa do script pelo Flyway sem avisos de sintaxe.
- Mapeamento adequado de tipos JSONB e chaves primárias UUID/VARCHAR(36).

---

## Fase 3: Entidades JPA, Enums e Repositórios Spring Data (Jobs, Stats e Webhooks)

### Objetivo
Mapear as entidades de persistência no pacote `model/` utilizando Hibernate 6 e Spring Data JPA, aplicando `@JdbcTypeCode(SqlTypes.JSON)` para colunas JSONB e criando as interfaces de repositório com queries agregadas (`COUNT`, `SUM`, agrupamentos temporais).

### Arquivos Alvo
- `model/ReportJob.java`
- `model/ReportSchedule.java`
- `model/DailyStats.java`
- `model/WebhookConfig.java`
- `model/WebhookDeliveryLog.java`
- `model/ReportAuditLog.java`
- `model/enums/`: `ReportType.java`, `OutputFormat.java`, `ReportStatus.java`.
- `repository/ReportJobRepository.java`
- `repository/DailyStatsRepository.java`
- `repository/ReportScheduleRepository.java`
- `repository/WebhookConfigRepository.java`
- `repository/ReportAuditLogRepository.java`

### Prompt da Fase 3

```text
Atue como Desenvolvedor Java Especialista em Spring Data JPA e Hibernate 6.
Vamos mapear as entidades e repositórios para o "reporting-service" em "br.edu.ifrn.promptuario.reporting".

1. Enums no pacote "model.enums":
   - "ReportType": CONSULTATIONS, PATIENTS, DOCTORS, PRESCRIPTIONS, CUSTOM, FULL_SYSTEM.
   - "OutputFormat": JSON, CSV, PDF, XLSX.
   - "ReportStatus": PENDING, RUNNING, COMPLETED, FAILED.

2. Entidades JPA no pacote "model":
   - "ReportJob":
     * id: String (UUID gerado com UUID.randomUUID().toString()).
     * reportType (ReportType @Enumerated(EnumType.STRING)), requestedBy: String.
     * parameters: Map<String, Object> via @JdbcTypeCode(SqlTypes.JSON).
     * status (ReportStatus), outputFormat (OutputFormat).
     * resultData: Map<String, Object> via @JdbcTypeCode(SqlTypes.JSON).
     * s3Key: String, errorMessage: String, rowCount: int.
     * scheduleId: String, webhookTriggered: boolean.
     * createdAt: Instant (@CreationTimestamp), completedAt: Instant.
   - "ReportSchedule":
     * id, name: String, reportType, outputFormat, cronExpression: String.
     * parameters: Map<String, Object> via @JdbcTypeCode(SqlTypes.JSON).
     * recipients: List<String> via @JdbcTypeCode(SqlTypes.JSON).
     * active: boolean, lastRunAt, nextRunAt, createdBy, createdAt, updatedAt.
   - "DailyStats":
     * id: String, statDate: String ("YYYY-MM-DD"), statType: String, entityId: String, value: int.
     * metadata: Map<String, Object> via @JdbcTypeCode(SqlTypes.JSON).
     * updatedAt: Instant.
   - "WebhookConfig", "WebhookDeliveryLog" e "ReportAuditLog".

3. Repositórios Spring Data JPA no pacote "repository":
   - "ReportJobRepository":
     * Page<ReportJob> findByRequestedByOrderByCreatedAtDesc(String requestedBy, Pageable pageable);
     * Page<ReportJob> findByStatus(ReportStatus status, Pageable pageable);
   - "DailyStatsRepository":
     * Optional<DailyStats> findByStatDateAndStatTypeAndEntityId(String statDate, String statType, String entityId);
     * Optional<DailyStats> findByStatDateAndStatTypeAndEntityIdIsNull(String statDate, String statType);
     * @Query para buscar métricas agregadas por período ("statDate BETWEEN :from AND :to") ordenado por statDate DESC.
     * @Query para consolidar produtividade de médicos agrupada por doctorId.
   - "ReportScheduleRepository":
     * List<ReportSchedule> findByActiveTrue();
   - "WebhookConfigRepository" e "ReportAuditLogRepository".

Forneça o código completo com anotações Jakarta, métodos otimizados e construtores.
```

### Critérios de Aceite
- Mapeamento JSONB em Hibernate 6 validado com `@JdbcTypeCode(SqlTypes.JSON)`.
- Consultas agregadas de estatísticas diárias com bom plano de execução.

---

## Fase 4: Java Records (DTOs) e Validações de Contrato Jakarta

### Objetivo
Implementar os DTOs como **Java Records imutáveis**, substituindo os schemas Pydantic de relatórios, parâmetros de exportação, responses de status de jobs e resumos de dashboards.

### Arquivos Alvo
- `dto/ReportExportRequestDTO.java`
- `dto/ReportJobResponseDTO.java`
- `dto/DashboardSummaryDTO.java`
- `dto/DailyStatItemDTO.java`
- `dto/DoctorProductivityDTO.java`
- `dto/ScheduleCreateDTO.java`
- `dto/ScheduleResponseDTO.java`
- `dto/WebhookCreateDTO.java`
- `dto/WebhookResponseDTO.java`

### Prompt da Fase 4

```text
Atue como Engenheiro de Software especialista em API Design e Java 21.
Vamos criar os Data Transfer Objects (DTOs) como Java Records imutáveis para o "reporting-service" em "br.edu.ifrn.promptuario.reporting.dto".

Mapeie os seguintes Records com anotações de validação Jakarta:

1. Solicitação e Status de Exportação:
   - "ReportExportRequestDTO": (
       @NotNull(message = "Tipo de relatório é obrigatório") ReportType reportType,
       @NotNull(message = "Formato de saída é obrigatório") OutputFormat outputFormat,
       Map<String, Object> parameters // ex: from_date, to_date, doctor_id
     )
   - "ReportJobResponseDTO": (
       String id,
       ReportType reportType,
       OutputFormat outputFormat,
       ReportStatus status,
       int rowCount,
       String s3Key,
       String downloadUrl, // URL pré-assinada ou relativa
       String errorMessage,
       Instant createdAt,
       Instant completedAt
     ) onde o método utilitário estático "fromEntity(ReportJob job, String presignedUrl)" faz a montagem.

2. Métricas do Dashboard:
   - "DashboardSummaryDTO": (
       long totalConsultationsToday,
       long totalNewPatientsToday,
       long totalCancellationsToday,
       long totalPrescriptionsToday,
       Map<String, Object> metricsWindow
     )
   - "DailyStatItemDTO": (String date, long value, Map<String, Object> metadata)
   - "DoctorProductivityDTO": (String doctorId, String doctorName, long totalConsultations, double averagePerDay)

3. Agendamentos e Webhooks:
   - "ScheduleCreateDTO": (
       @NotBlank String name,
       @NotNull ReportType reportType,
       @NotNull OutputFormat outputFormat,
       @NotBlank String cronExpression,
       Map<String, Object> parameters,
       List<String> recipients
     )
   - "ScheduleResponseDTO": (String id, String name, ReportType reportType, OutputFormat outputFormat, String cronExpression, boolean active, Instant lastRunAt, Instant nextRunAt)
   - "WebhookCreateDTO": (@NotBlank String url, @NotBlank String secret, String description, List<String> events)
   - "WebhookResponseDTO": (String id, String url, String description, boolean active, int consecutiveFailures, Instant lastSuccessAt)

Garanta paridade total de nomenclatura e serialização JSON (snake_case quando apropriado) com os contratos consumidos pelo Frontend.
```

### Critérios de Aceite
- Records compiláveis e imutáveis com validações Jakarta completas.
- Compatibilidade contratual com os modelos do Frontend React.

---

## Fase 5: Motores de Exportação Multi-Formato (CSV, Excel XLSX e PDF)

### Objetivo
Implementar os exportadores dedicados de alta performance para converter listas de registros em arquivos binários nos formatos CSV (OpenCSV), planilhas Excel estilizadas com abas (Apache POI) e relatórios em PDF (OpenPDF com cabeçalho corporativo e tabelas formatadas).

### Arquivos Alvo
- `worker/exporters/ReportExporter.java` (interface)
- `worker/exporters/CsvReportExporter.java`
- `worker/exporters/ExcelReportExporter.java`
- `worker/exporters/PdfReportExporter.java`

### Prompt da Fase 5

```text
Atue como Engenheiro de Software Especialista em Geração e Processamento de Documentos em Java.
Vamos implementar os motores de exportação do "reporting-service" no pacote "br.edu.ifrn.promptuario.reporting.worker.exporters".

1. Interface "ReportExporter.java":
   - "byte[] export(List<Map<String, Object>> data, String reportTitle, Map<String, Object> metadata);"
   - "String getContentType();"
   - "String getFileExtension();"

2. "CsvReportExporter.java" (OpenCSV):
   - Extrai as colunas a partir das chaves do primeiro registro.
   - Utiliza "CSVWriter" com separador vírgula e codificação UTF-8 com BOM (para abertura correta no Excel sem corromper acentos).
   - Retorna array de bytes ("byte[]").

3. "ExcelReportExporter.java" (Apache POI - SXSSFWorkbook / XSSFWorkbook):
   - Suporte a geração de planilhas XLSX estilizadas:
     * Cabeçalho corporativo com fundo azul escuro/petróleo (#1A6E6E), texto branco em negrito e bordas finas.
     * Linhas de dados com formatação alternada (zebra striping leve).
     * Auto-ajuste de largura das colunas (autoSizeColumn).
     * Formatação automática de células de data e valores numéricos.
     * Suporte a relatórios com múltiplas abas quando o relatório for "FULL_SYSTEM" (ex: aba "Consultas", aba "Pacientes", aba "Prescrições").

4. "PdfReportExporter.java" (OpenPDF):
   - Utiliza OpenPDF (com.github.librepdf:openpdf) para gerar documento estruturado:
     * Página A4 (paisagem ou retrato dependendo do número de colunas).
     * Cabeçalho com título do relatório, data/hora de emissão e usuário solicitante.
     * Tabela com "PdfPTable", células estilizadas e repetição automática do cabeçalho em quebra de página.
     * Rodapé com numeração de páginas ("Página X de Y").

Forneça o código completo, com proteção contra consumo excessivo de memória em relatórios com milhares de registros.
```

### Critérios de Aceite
- Arquivos CSV abrem sem problemas de acentuação (UTF-8 com BOM).
- Planilhas XLSX geradas com formatação profissional legível pelo Microsoft Excel e LibreOffice.
- PDFs paginados corretamente com cabeçalhos e rodapés institucionais.

---

## Fase 6: Integração com Storage S3 (MinIO) e Geração de URLs Pré-Assinadas

### Objetivo
Configurar o bean `S3Client` e `S3Presigner` do AWS SDK for Java 2.x e implementar o serviço `S3ReportStorageService` para persistência dos relatórios gerados no bucket `reports` e emissão de URLs pré-assinadas com validade controlada (ex: 60 minutos).

### Arquivos Alvo
- `config/S3Config.java`
- `storage/S3ReportStorageService.java`

### Prompt da Fase 6

```text
Atue como Engenheiro de Cloud e Armazenamento S3 especialista em AWS SDK for Java 2.x e MinIO.
Vamos implementar o armazenamento e disponibilização segura de relatórios no "reporting-service".

1. "S3Config.java" em "br.edu.ifrn.promptuario.reporting.config":
   - Configurar Beans para MinIO:
     * "S3Client": endpoint configurado para MinIO, credenciais estáticas, "forcePathStyle(true)".
     * "S3Presigner": configurado com o mesmo endpoint e credenciais para pré-assinatura de URLs de download temporárias.
   - EventListener na inicialização para criar o bucket "reports" caso não exista.

2. "S3ReportStorageService.java" em "br.edu.ifrn.promptuario.reporting.storage":
   - "uploadReport(String reportType, String jobId, String extension, byte[] content, String contentType)":
     * Constrói a chave estruturada: "reports/{reportType.toLowerCase()}/{jobId}.{extension.toLowerCase()}".
     * Executa PutObjectRequest com metadados adicionais (tamanho, jobId, data de geração).
     * Retorna a chave S3 gravada.
   - "generatePresignedDownloadUrl(String s3Key, Duration expiration)":
     * Utiliza S3Presigner para gerar URL assinada temporária (padrão 60 minutos).
     * Trata substituição de endpoint interno (ex: "http://minio:9000") pelo endpoint externo público configurado em "S3_PUBLIC_ENDPOINT" se aplicável.
   - "deleteReport(String s3Key)".

Gere código com logs estruturados e tratamento resiliente de S3Exception.
```

### Critérios de Aceite
- Upload de binários de relatórios no bucket `reports` do MinIO.
- URLs pré-assinadas geradas permitindo download direto sem expor credenciais do MinIO.

---

## Fase 7: Worker Assíncrono de Relatórios, Agendamento Cron e Notificações Webhook

### Objetivo
Implementar o worker assíncrono de processamento de relatórios (`ReportExecutionWorker` via `@RabbitListener` ou `@Async`), o agendador de tarefas periódicas (`ReportSchedulerService` substituindo o Celery Beat via `@Scheduled`) e o despachante de webhooks HTTP assíncrono com assinatura HMAC (`WebhookDeliveryService`).

### Arquivos Alvo
- `worker/ReportExecutionWorker.java`
- `service/ReportSchedulerService.java`
- `service/WebhookDeliveryService.java`
- `service/ReportJobService.java`

### Prompt da Fase 7

```text
Atue como Engenheiro de Backend especialista em Processamento Assíncrono, Workers e Webhooks em Spring Boot.
Vamos construir o orquestrador de execução de relatórios do "reporting-service".

1. "ReportJobService.java":
   - "createExportJob(ReportExportRequestDTO request, String userId)":
     * Persiste ReportJob com status PENDING no banco "reporting_db".
     * Envia mensagem para a fila RabbitMQ "reporting.job.queue" contendo o "jobId" (ou despacha para @Async).
     * Registra auditoria em "report_audit_logs".
     * Retorna "ReportJobResponseDTO".

2. "ReportExecutionWorker.java" (pacote "worker"):
   - Listener @RabbitListener(queues = "reporting.job.queue"):
     1. Localiza o ReportJob pelo ID e altera status para RUNNING.
     2. Extrai os dados agregados da tabela "daily_stats" conforme o "reportType" e parâmetros de data.
     3. Seleciona o exportador correspondente (CSV, XLSX, PDF ou JSON).
     4. Compila o arquivo em memória/stream.
     5. Realiza o upload no MinIO via S3ReportStorageService no bucket "reports".
     6. Atualiza o ReportJob: status = COMPLETED, s3Key, rowCount = total de linhas, completedAt = Instant.now().
     7. Se houver falha, captura a exceção: status = FAILED, errorMessage = exc.getMessage().
     8. Dispara notificação de webhook se configurado.

3. "ReportSchedulerService.java" (Substituição do Celery Beat):
   - Executa a cada 60 segundos via @Scheduled(fixedRate = 60000):
     * Busca todos os "ReportSchedule" ativos no banco.
     * Avalia se a "cronExpression" do agendamento coincide com o minuto atual (usando CronExpression do Spring).
     * Se coincidir: dispara a criação do job de relatório automaticamente.
     * Atualiza "lastRunAt" e calcula "nextRunAt".

4. "WebhookDeliveryService.java":
   - Para jobs concluídos, localiza webhooks ativos cadastrados para o evento "report.completed".
   - Monta payload JSON com os dados do job e URL de download.
   - Calcula assinatura HMAC-SHA256 no header "X-Signature" usando o segredo do webhook.
   - Envia requisição POST via Spring RestClient com timeout de 5 segundos.
   - Registra tentativa na tabela "webhook_delivery_logs" com status HTTP e tempo de resposta.

Forneça código resiliente com retentativas controladas e sem bloqueio de threads principais.
```

### Critérios de Aceite
- Transição completa do ciclo de vida do job (`PENDING` -> `RUNNING` -> `COMPLETED`/`FAILED`).
- Desativação completa do Celery Beat com agendamento nativo do Spring TaskScheduler.
- Assinatura criptográfica válida nos envios de webhook.

---

## Fase 8: Mensageria RabbitMQ e Atualização Contínua de Métricas Diárias (DailyStats)

### Objetivo
Configurar os listeners do Spring AMQP (`DomainEventConsumer`) para escutar os eventos de domínio emitidos pelos outros microsserviços do sistema (`patient.created`, `appointment.created`, `appointment.cancelled`, `medical_record.created`) e atualizar atomicamente as métricas analíticas diárias na tabela `daily_stats`.

### Arquivos Alvo
- `config/RabbitMQConfig.java`
- `messaging/DomainEventConsumer.java`
- `service/DashboardStatsService.java`

### Prompt da Fase 8

```text
Atue como Especialista em Arquitetura Orientada a Eventos (EDA) com Spring AMQP e RabbitMQ.
Vamos configurar o consumo de eventos para consolidação de métricas no "reporting-service".

1. "RabbitMQConfig.java" em "br.edu.ifrn.promptuario.reporting.config":
   - Declarar Queue "reporting.events.queue" (durable).
   - Ligar a fila na TopicExchange "domain.events" com as seguintes routing keys:
     * "patient.created"
     * "appointment.created"
     * "appointment.cancelled"
     * "medical_record.created"
     * "prescription.generated"
   - Configurar Jackson2JsonMessageConverter para desserialização automática.

2. "DomainEventConsumer.java" em "br.edu.ifrn.promptuario.reporting.messaging":
   - @RabbitListener(queues = "reporting.events.queue"):
     * Evento "appointment.created": incrementa a métrica "CONSULTATIONS" para a data atual (ou data agendada) em "daily_stats".
     * Evento "appointment.cancelled": incrementa a métrica "CANCELLATIONS" em "daily_stats".
     * Evento "patient.created": incrementa a métrica "NEW_PATIENTS" em "daily_stats".
     * Evento "medical_record.created": incrementa a métrica "DOCTOR_CONSULTATIONS" vinculando o "doctorId" em "daily_stats(entity_id)".
     * Evento "prescription.generated": incrementa a métrica "PRESCRIPTIONS".
   - Idempotência:
     * Evita contagem duplicada caso o RabbitMQ reenvie a mesma mensagem.

3. "DashboardStatsService.java":
   - "getDailyConsultations(String fromDate, String toDate)": busca estatísticas consolidadas no período.
   - "getNewPatientsStats(String fromDate, String toDate)".
   - "getDoctorProductivityStats(String fromDate, String toDate, String doctorId)".
   - "getSummaryMetrics()": consolida os totais do dia para os cards do dashboard administrativo.

Garanta operações de incremento atômicas com controle transacional adequado no PostgreSQL.
```

### Critérios de Aceite
- Eventos recebidos atualizam imediatamente os registros de `daily_stats`.
- Dashboards administrativos consomem dados pré-agregados com tempo de resposta < 50ms.

---

## Fase 9: Controladores REST, Segurança OAuth2 JWT, Docker Multi-Stage e Testes

### Objetivo
Implementar os controladores REST (`ReportExportController`, `DashboardStatsController`, `ScheduleController`, `WebhookController`, `ReportAuditController`), configurar o Spring Security OAuth2 Resource Server com autorizações RBAC, criar o `Dockerfile` multi-stage otimizado e elaborar testes de integração com Testcontainers.

### Arquivos Alvo
- `controller/ReportExportController.java`
- `controller/DashboardStatsController.java`
- `controller/ScheduleController.java`
- `controller/WebhookController.java`
- `config/SecurityConfig.java`
- `backend-java/reporting-service/Dockerfile`
- `backend/docker-compose.yml` (atualização)
- `backend-java/reporting-service/src/test/java/br/edu/ifrn/promptuario/reporting/ReportingIntegrationTest.java`

### Prompt da Fase 9

```text
Atue como Desenvolvedor Sênior e Especialista em DevOps/QA.
Vamos finalizar a implementação do "reporting-service" com controladores REST, Dockerfile e testes de integração.

1. Controladores REST no pacote "controller":
   - "ReportExportController.java" (@RequestMapping("/api/v1/reports/export")):
     * POST "" -> Solicita exportação (retorna HTTP 202 Accepted com JobStatusResponseDTO).
     * GET "/{job_id}" -> Consulta status e progresso do job.
     * GET "/{job_id}/download" -> Retorna link pré-assinado temporário do MinIO ou faz redirect.
   - "DashboardStatsController.java" (@RequestMapping("/api/v1/reports")):
     * GET "/consultations" -> Estatísticas de consultas.
     * GET "/patients" -> Estatísticas de novos pacientes.
     * GET "/doctors" -> Produtividade médica.
     * GET "/summary" -> Resumo rápido de KPIs para cards do dashboard.
   - "ScheduleController.java" (@RequestMapping("/api/v1/reports/schedules")):
     * GET "", POST "" -> Gestão de agendamentos recorrentes.
   - "WebhookController.java" (@RequestMapping("/api/v1/reports/webhooks")):
     * GET "", POST "" -> Gestão de webhooks.
   - Health check: GET "/healthz" -> { "status": "ok", "service": "reporting-service" }.

2. "SecurityConfig.java":
   - Configurar Spring Security com OAuth2 Resource Server decodificando JWT simétrico.
   - Endpoints públicos: "/healthz", "/actuator/**".
   - Demais endpoints protegidos por roles (@PreAuthorize("hasRole('ADMIN')") ou DOCTOR).

3. "backend-java/reporting-service/Dockerfile":
   - Multi-stage build com Java 21:
     * Stage 1: "maven:3.9-eclipse-temurin-21-alpine", compila JAR executável.
     * Stage 2: "eclipse-temurin:21-jre-alpine", copia app.jar, expõe porta 8000, usuário não-root.
     * Flags JVM: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0".

4. Atualização no "backend/docker-compose.yml":
   - Atualizar a entrada do "reporting-service" apontando para a nova imagem Java.
   - Portas: "8005:8000".
   - Desativar ou remover o container "reporting-worker" (Celery), pois o processamento assíncrono agora ocorre internamente via Spring AMQP Worker.

5. "ReportingIntegrationTest.java":
   - Teste ponta a ponta com Testcontainers (PostgreSQL, RabbitMQ e MinIO):
     * Solicita exportação de relatório em CSV via POST /api/v1/reports/export.
     * Aguarda o processamento pelo worker.
     * Valida que o job atinge status COMPLETED e que o arquivo CSV foi gravado no MinIO.
     * Valida o download através da URL pré-assinada.

Forneça os arquivos completos e comandos prontos para homologação.
```

### Critérios de Aceite
- Build da imagem Docker final com tamanho inferior a 280MB.
- Passagem com sucesso de 100% dos testes de integração no Testcontainers.
- Eliminação definitiva dos containers Celery (`reporting-worker`) e redução do consumo de memória.

---

## Matriz de Rastreabilidade e Riscos Técnicos

| Risco Identificado | Severidade | Impacto | Medida Mitigatória nos Prompts |
| :--- | :--- | :--- | :--- |
| **Esgotamento de Memória (OOM) no Excel** | Alta | Queda do container em relatórios extensos | A Fase 5 utiliza `SXSSFWorkbook` (streaming workbook) para descartar linhas da memória para o disco. |
| **Acentuação corrompida em CSV** | Média | Nomes e diagnósticos ilegíveis no Excel | A Fase 5 injeta obrigatoriamente o preâmbulo UTF-8 com BOM (`\uFEFF`) na exportação CSV. |
| **Orfandade de jobs agendados** | Alta | Relatórios automáticos deixam de ser disparados | A Fase 7 substitui o Celery Beat por `@Scheduled` com avaliação de expressões Cron gerenciada pelo Spring. |
| **Falha de Webhooks travando workers** | Média | Fila de relatórios represada por endpoints lentos | A Fase 7 configura timeout curto de 5s e retentativas não-bloqueantes com registro em log. |

---

## Comandos Rápidos de Validação

```bash
# 1. Compilar e rodar testes do Reporting Service:
cd backend-java/reporting-service
mvn clean test

# 2. Subir o serviço no Docker Compose:
cd ../../backend
docker compose up -d --build reporting-service

# 3. Validar health check:
curl -i http://localhost:8005/healthz

# 4. Consultar resumo de métricas do dashboard:
curl -s http://localhost:8005/api/v1/reports/summary \
  -H "Authorization: Bearer <TOKEN>" | jq .
```
