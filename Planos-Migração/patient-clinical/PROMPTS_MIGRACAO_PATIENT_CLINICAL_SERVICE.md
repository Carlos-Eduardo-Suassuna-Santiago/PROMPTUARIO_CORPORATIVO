# Guia de Prompts de Implementação: Migração do Patient-Clinical Service (FastAPI -> Spring Boot 3.x / Java 21)

Este documento contém o conjunto completo de **prompts estruturados e detalhados**, organizados em **12 fases sequenciais**, para guiar a implementação assistida por IA da migração do **Patient-Clinical Service** de Python (FastAPI/SQLAlchemy/Celery) para **Java 21 (LTS) e Spring Boot 3.3+**.

O conteúdo foi desenhado com base nas diretrizes de arquitetura unificada de [PLANO_MIGRACAO_PATIENT_CLINICAL_SERVICE.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/PLANO_MIGRACAO_PATIENT_CLINICAL_SERVICE.md) e nos contratos vigentes da aplicação.

---

## Índice das Fases de Implementação

1. [Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base](#fase-1-bootstrap-do-projeto-dependências-maven-e-configurações-base)
2. [Fase 2: Migração Flyway e Schema Unificado do Banco de Dados](#fase-2-migração-flyway-e-schema-unificado-do-banco-de-dados)
3. [Fase 3: Entidades JPA e Repositórios Spring Data (Módulos Patient e Clinical)](#fase-3-entidades-jpa-e-repositórios-spring-data-módulos-patient-e-clinical)
4. [Fase 4: Java Records (DTOs) e Validações de Contrato Jakarta](#fase-4-java-records-dtos-e-validações-de-contrato-jakarta)
5. [Fase 5: Serviços de Domínio e Transacionalidade de Pacientes (Módulo Patient)](#fase-5-serviços-de-domínio-e-transacionalidade-de-pacientes-módulo-patient)
6. [Fase 6: Serviços de Domínio e Regras Clínicas (Módulo Clinical)](#fase-6-serviços-de-domínio-e-regras-clínicas-módulo-clinical)
7. [Fase 7: Integração de Storage S3 / MinIO (Documentos e Receitas)](#fase-7-integração-de-storage-s3--minio-documentos-e-receitas)
8. [Fase 8: Mensageria RabbitMQ (Event-Driven: Publishers e IAM Consumers)](#fase-8-mensageria-rabbitmq-event-driven-publishers-e-iam-consumers)
9. [Fase 9: Worker Assíncrono de Geração de PDFs de Prescrições (Substituição Celery)](#fase-9-worker-assíncrono-de-geração-de-pdfs-de-prescrições-substituição-celery)
10. [Fase 10: Segurança OAuth2 Resource Server (JWT) e Tratamento Global de Erros](#fase-10-segurança-oauth2-resource-server-jwt-e-tratamento-global-de-erros)
11. [Fase 11: Controladores REST, Endpoints Canônicos e Health Check](#fase-11-controladores-rest-endpoints-canônicos-e-health-check)
12. [Fase 12: Dockerfile Multi-Stage, Docker Compose e Testes de Integração](#fase-12-dockerfile-multi-stage-docker-compose-e-testes-de-integração)

---

## Diretrizes Gerais para Uso dos Prompts

- **Execução Sequencial:** Cada fase constrói sobre as interfaces e artefatos gerados nas fases anteriores. Não pule fases.
- **Padrão Arquitetural:** Adotar o padrão **Modular Monolith com Clean Architecture**, agrupando o código por módulos de domínio (`modules/patient` e `modules/clinical`) e recursos transversais em `shared/` e `config/`.
- **Compatibilidade Estrita de API:** Nenhum path de endpoint, query parameter ou formato de payload JSON deve ser alterado, garantindo compatibilidade transparente com o **API Gateway** e o **Frontend React**.
- **Java Moderno:** Utilizar **Java 21**, preferindo `record` para DTOs, `var` quando couber, Pattern Matching, e evitar anotações excessivas de frameworks mágicos sem necessidade.

---

## Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base

### Objetivo
Criar a estrutura do projeto Spring Boot 3.3+ com Java 21 em `backend-java/patient-clinical-service`, configurando o `pom.xml` completo, propriedades de ambiente com suporte a profiles (`application.yml`), Actuator para métricas/health e o ponto de entrada principal `@SpringBootApplication`.

### Arquivos Alvo
- `backend-java/patient-clinical-service/pom.xml`
- `backend-java/patient-clinical-service/src/main/resources/application.yml`
- `backend-java/patient-clinical-service/src/main/resources/application-dev.yml`
- `backend-java/patient-clinical-service/src/main/resources/application-test.yml`
- `backend-java/patient-clinical-service/src/main/java/br/edu/ifrn/promptuario/PatientClinicalApplication.java`

### Prompt da Fase 1

```text
Atue como um Engenheiro de Software Sênior especialista em Java 21 e Spring Boot 3.3+.
Nosso objetivo é inicializar o novo microsserviço unificado "patient-clinical-service" em Java, localizado no diretório "backend-java/patient-clinical-service/".

Gere os arquivos estruturais iniciais do projeto:

1. "pom.xml":
   - Java 21, Spring Boot versão 3.3.4 (ou superior compatível).
   - GroupId: "br.edu.ifrn.promptuario"
   - ArtifactId: "patient-clinical-service"
   - Dependências obrigatórias:
     * spring-boot-starter-web
     * spring-boot-starter-data-jpa
     * org.postgresql:postgresql (driver runtime)
     * org.flywaydb:flyway-core e org.flywaydb:flyway-database-postgresql
     * spring-boot-starter-validation
     * spring-boot-starter-security
     * spring-boot-starter-oauth2-resource-server
     * spring-boot-starter-amqp (RabbitMQ)
     * spring-boot-starter-actuator
     * io.micrometer:micrometer-registry-prometheus
     * software.amazon.awssdk:s3:2.25.0+ (AWS SDK v2 para MinIO/S3)
     * com.github.librepdf:openpdf:1.3.39 ou superior (para geração de PDF de prescrições)
     * org.springframework.boot:spring-boot-starter-thymeleaf (templates de PDF)
     * org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0 (Swagger /docs)
     * Lombok (opcional para boilerplate de entidades, com scope provided)
     * spring-boot-starter-test, testcontainers (postgresql, rabbitmq, localstack) com escopo test.

2. "src/main/resources/application.yml":
   - Configuração de datasource para PostgreSQL conectando no banco unificado "patient_clinical_db" (com defaults para host "localhost:5432" e env vars para Docker "db-patient-clinical:5432").
   - Flyway ativado apontando para "classpath:db/migration".
   - JPA / Hibernate com dialect PostgreSQL, "ddl-auto: validate", "open-in-view: false", "show-sql: false".
   - RabbitMQ com host "localhost:5672" (env var "RABBITMQ_HOST"), virtual host "/", exchange principal "domain.events" (topic) e "iam.events" (topic).
   - Propriedades customizadas em "app.storage.s3":
     * endpoint: "${S3_ENDPOINT:http://localhost:9000}"
     * access-key: "${S3_ACCESS_KEY:minioadmin}"
     * secret-key: "${S3_SECRET_KEY:minioadmin}"
     * region: "${AWS_REGION:us-east-1}"
     * bucket-patient-documents: "${S3_BUCKET_PATIENT_DOCUMENTS:patient-documents}"
     * bucket-prescriptions: "${S3_BUCKET_PRESCRIPTIONS:prescriptions}"
   - Segurança JWT: "app.security.jwt.secret: ${JWT_SECRET_KEY:change-me-in-production-super-secret-key-min-32-chars}" e algorithm HMAC-SHA256.
   - Actuator configurado expondo "health,info,prometheus" na porta da aplicação, com "/healthz" mapeado ou verificado via "management.endpoints.web.base-path=/actuator".
   - Configuração da porta do servidor: 8000 (interna do container).

3. "PatientClinicalApplication.java":
   - Classe principal com anotação @SpringBootApplication, habilitando @EnableAsync e @EnableScheduling.

Certifique-se de que o código compile perfeitamente e siga as melhores práticas de Clean Code.
```

### Critérios de Aceite
- Execução de `mvn clean compile` sem erros.
- Arquivos de configuração estruturados com carregamento limpo de variáveis de ambiente.

---

## Fase 2: Migração Flyway e Schema Unificado do Banco de Dados

### Objetivo
Criar o script de migração Flyway `V1__init_patient_clinical_schema.sql` contendo o DDL exato de todas as tabelas, índices, enums e foreign keys do banco `patient_clinical_db`, consolidando os dados dos módulos `patient` e `clinical` e garantindo suporte a JSON e auditoria.

### Arquivos Alvo
- `backend-java/patient-clinical-service/src/main/resources/db/migration/V1__init_patient_clinical_schema.sql`

### Prompt da Fase 2

```text
Atue como um Engenheiro de Banco de Dados e DBA especializado em PostgreSQL 15 e Flyway.
Precisamos criar o script DDL canônico unificado para a migração V1 do microsserviço "patient-clinical-service".

O arquivo deve ser criado em:
"backend-java/patient-clinical-service/src/main/resources/db/migration/V1__init_patient_clinical_schema.sql"

Com base na arquitetura unificada existente do sistema (unificação dos legados patient-service e clinical-service), crie o script SQL estruturado e idempotente contendo:

1. Tipos ENUM nativos do PostgreSQL:
   - "gender_type": ('M', 'F', 'OTHER')
   - "allergy_severity": ('MILD', 'MODERATE', 'SEVERE')
   - "appointment_type": ('CONSULTATION', 'RETURN', 'EXAM', 'URGENT')
   - "appointment_status": ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
   - "exam_urgency": ('ROUTINE', 'URGENT', 'EMERGENCY')

2. Tabelas do Contexto de Paciente (Módulo Patient):
   - "patients": id (UUID/VARCHAR(36) PK), user_id (VARCHAR(36) UNIQUE NOT NULL), full_name (VARCHAR(255) NOT NULL), cpf (VARCHAR(14) UNIQUE), date_of_birth (DATE), gender (gender_type), blood_type (VARCHAR(5)), phone (VARCHAR(20)), email (VARCHAR(255)), street (VARCHAR(255)), city (VARCHAR(100)), state (VARCHAR(2)), zip_code (VARCHAR(9)), emergency_name (VARCHAR(255)), emergency_phone (VARCHAR(20)), emergency_relation (VARCHAR(50)), notes (TEXT), is_active (BOOLEAN NOT NULL DEFAULT TRUE), anonymized (BOOLEAN NOT NULL DEFAULT FALSE), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP), updated_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "allergies": id (VARCHAR(36) PK), patient_id (VARCHAR(36) REFERENCES patients(id) ON DELETE CASCADE), substance (VARCHAR(255) NOT NULL), severity (allergy_severity NOT NULL), reaction_type (VARCHAR(255)), notes (TEXT), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "vaccines": id (VARCHAR(36) PK), patient_id (VARCHAR(36) REFERENCES patients(id) ON DELETE CASCADE), name (VARCHAR(255) NOT NULL), dose (VARCHAR(50)), applied_at (DATE), next_dose_at (DATE), manufacturer (VARCHAR(100)), lot_number (VARCHAR(50)), applied_by (VARCHAR(255)), notes (TEXT), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "continuous_medications": id (VARCHAR(36) PK), patient_id (VARCHAR(36) REFERENCES patients(id) ON DELETE CASCADE), name (VARCHAR(255) NOT NULL), dosage (VARCHAR(100) NOT NULL), frequency (VARCHAR(100) NOT NULL), route (VARCHAR(50)), started_at (DATE), ended_at (DATE), is_active (BOOLEAN DEFAULT TRUE), notes (TEXT), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "medication_history": id (VARCHAR(36) PK), medication_id (VARCHAR(36) NOT NULL), patient_id (VARCHAR(36) REFERENCES patients(id) ON DELETE CASCADE), changed_by (VARCHAR(36) NOT NULL), change_type (VARCHAR(50) NOT NULL), snapshot (JSONB NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "patient_documents": id (VARCHAR(36) PK), patient_id (VARCHAR(36) REFERENCES patients(id) ON DELETE CASCADE), title (VARCHAR(255) NOT NULL), description (TEXT), category (VARCHAR(50) NOT NULL), s3_key (VARCHAR(500) NOT NULL), original_filename (VARCHAR(255) NOT NULL), mime_type (VARCHAR(100) NOT NULL), size_bytes (BIGINT NOT NULL), uploaded_by (VARCHAR(36) NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).

3. Tabelas do Contexto Clínico (Módulo Clinical):
   - "doctor_schedules": id (VARCHAR(36) PK), doctor_id (VARCHAR(36) NOT NULL), specialty (VARCHAR(100)), is_active (BOOLEAN DEFAULT TRUE), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "time_slots": id (VARCHAR(36) PK), schedule_id (VARCHAR(36) REFERENCES doctor_schedules(id) ON DELETE CASCADE), slot_date (DATE NOT NULL), start_time (VARCHAR(5) NOT NULL), end_time (VARCHAR(5) NOT NULL), is_available (BOOLEAN DEFAULT TRUE), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "appointments": id (VARCHAR(36) PK), patient_id (VARCHAR(36) REFERENCES patients(id)), doctor_id (VARCHAR(36) NOT NULL), slot_id (VARCHAR(36) REFERENCES time_slots(id)), scheduled_at (TIMESTAMPTZ NOT NULL), appointment_type (appointment_type NOT NULL DEFAULT 'CONSULTATION'), specialty (VARCHAR(100)), status (appointment_status NOT NULL DEFAULT 'SCHEDULED'), cancellation_reason (VARCHAR(255)), cancelled_by (VARCHAR(36)), cancelled_at (TIMESTAMPTZ), notes (TEXT), created_by (VARCHAR(36) NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP), updated_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "medical_records": id (VARCHAR(36) PK), appointment_id (VARCHAR(36) UNIQUE NOT NULL REFERENCES appointments(id)), patient_id (VARCHAR(36) REFERENCES patients(id)), doctor_id (VARCHAR(36) NOT NULL), chief_complaint (TEXT NOT NULL), anamnesis (TEXT), physical_exam (TEXT), diagnosis (TEXT), diagnosis_codes (JSONB DEFAULT '[]'::jsonb), treatment_plan (TEXT), observations (TEXT), rich_notes (JSONB), signature_hash (VARCHAR(64)), signed_by (VARCHAR(36)), signed_at (TIMESTAMPTZ), ai_analysis_id (VARCHAR(36)), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP), updated_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "medical_record_history": id (VARCHAR(36) PK), record_id (VARCHAR(36) REFERENCES medical_records(id) ON DELETE CASCADE), changed_by (VARCHAR(36) NOT NULL), change_type (VARCHAR(50) NOT NULL), snapshot (JSONB NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "prescriptions": id (VARCHAR(36) PK), record_id (VARCHAR(36) REFERENCES medical_records(id) ON DELETE CASCADE), patient_id (VARCHAR(36) NOT NULL), doctor_id (VARCHAR(36) NOT NULL), medications (JSONB NOT NULL DEFAULT '[]'::jsonb), instructions (TEXT), valid_days (INT DEFAULT 30), pdf_s3_key (VARCHAR(500)), pdf_generated_at (TIMESTAMPTZ), signature_hash (VARCHAR(64)), signed_by (VARCHAR(36)), signed_at (TIMESTAMPTZ), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "prescription_history": id (VARCHAR(36) PK), prescription_id (VARCHAR(36) REFERENCES prescriptions(id) ON DELETE CASCADE), record_id (VARCHAR(36) NOT NULL), changed_by (VARCHAR(36) NOT NULL), change_type (VARCHAR(50) NOT NULL), snapshot (JSONB NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "exam_requests": id (VARCHAR(36) PK), record_id (VARCHAR(36) REFERENCES medical_records(id) ON DELETE CASCADE), patient_id (VARCHAR(36) NOT NULL), doctor_id (VARCHAR(36) NOT NULL), exam_type (VARCHAR(255) NOT NULL), urgency (exam_urgency NOT NULL DEFAULT 'ROUTINE'), instructions (TEXT), result (TEXT), result_date (TIMESTAMPTZ), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "exam_request_history": id (VARCHAR(36) PK), exam_id (VARCHAR(36) REFERENCES exam_requests(id) ON DELETE CASCADE), record_id (VARCHAR(36) NOT NULL), changed_by (VARCHAR(36) NOT NULL), change_type (VARCHAR(50) NOT NULL), snapshot (JSONB NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "medical_certificates": id (VARCHAR(36) PK), record_id (VARCHAR(36) REFERENCES medical_records(id) ON DELETE CASCADE), patient_id (VARCHAR(36) NOT NULL), doctor_id (VARCHAR(36) NOT NULL), reason (TEXT NOT NULL), days_off (INT NOT NULL), start_date (DATE NOT NULL), notes (TEXT), pdf_s3_key (VARCHAR(255)), pdf_generated_at (TIMESTAMPTZ), signature_hash (VARCHAR(64)), signed_by (VARCHAR(36)), signed_at (TIMESTAMPTZ), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).
   - "medical_certificate_history": id (VARCHAR(36) PK), certificate_id (VARCHAR(36) REFERENCES medical_certificates(id) ON DELETE CASCADE), record_id (VARCHAR(36) NOT NULL), changed_by (VARCHAR(36) NOT NULL), change_type (VARCHAR(50) NOT NULL), snapshot (JSONB NOT NULL), created_at (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).

4. Tabela de Auditoria Transversal:
   - "audit_logs": id (VARCHAR(36) PK), service_name (VARCHAR(50) NOT NULL), table_name (VARCHAR(50) NOT NULL), operation (VARCHAR(10) NOT NULL), record_id (VARCHAR(36) NOT NULL), user_id (VARCHAR(36)), correlation_id (VARCHAR(64)), old_values (JSONB), new_values (JSONB), timestamp (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP).

5. Índices de Otimização:
   - Índices em `patients(user_id)`, `patients(cpf)`, `patients(full_name)`, `allergies(patient_id)`, `vaccines(patient_id)`, `continuous_medications(patient_id)`.
   - Índices em `appointments(patient_id)`, `appointments(doctor_id)`, `appointments(scheduled_at)`, `appointments(status)`.
   - Índices em `medical_records(patient_id)`, `medical_records(doctor_id)`, `medical_records(appointment_id)`.
   - Índices em `audit_logs(timestamp)`, `audit_logs(record_id)`.

Forneça o arquivo SQL completo, com comentários explicativos e padrões de chave primária e integridade referencial.
```

### Critérios de Aceite
- Script SQL sintaticamente válido para PostgreSQL 15.
- Todos os campos, enums e relacionamentos mapeados com integridade referencial e deleções em cascata onde apropriado.

---

## Fase 3: Entidades JPA e Repositórios Spring Data (Módulos Patient e Clinical)

### Objetivo
Mapear em Java as entidades de domínio com Spring Data JPA e Hibernate 6.x nos pacotes `modules/patient/model` e `modules/clinical/model`, suportando colunas JSONB com `@JdbcTypeCode(SqlTypes.JSON)`, enums como String e interfaces de repositório `JpaRepository` com queries customizadas.

### Arquivos Alvo
- `modules/patient/model/`: `Patient.java`, `Allergy.java`, `Vaccine.java`, `ContinuousMedication.java`, `MedicationHistory.java`, `PatientDocument.java` e Enums (`GenderType`, `AllergySeverity`).
- `modules/clinical/model/`: `DoctorSchedule.java`, `TimeSlot.java`, `Appointment.java`, `MedicalRecord.java`, `MedicalRecordHistory.java`, `Prescription.java`, `PrescriptionHistory.java`, `ExamRequest.java`, `ExamRequestHistory.java`, `MedicalCertificate.java`, `MedicalCertificateHistory.java` e Enums (`AppointmentType`, `AppointmentStatus`, `ExamUrgency`).
- `shared/model/AuditLog.java`
- Repositórios nos respectivos subpacotes `repository/`.

### Prompt da Fase 3

```text
Atue como Desenvolvedor Java Especialista em Spring Data JPA e Hibernate 6.
Nosso objetivo é implementar as entidades de persistência e interfaces de repositório para o microsserviço unificado "patient-clinical-service".

Package base: "br.edu.ifrn.promptuario"

1. Implemente as entidades JPA no módulo "patient" (pacote "br.edu.ifrn.promptuario.modules.patient.model"):
   - "Patient": ID como String/UUID gerado com UUID.randomUUID().toString(), relacionamentos @OneToMany (cascade ALL, orphanRemoval true) com Allergy, Vaccine, ContinuousMedication, MedicationHistory, PatientDocument. Flags "isActive", "anonymized", datas de auditoria @CreationTimestamp e @UpdateTimestamp.
   - "Allergy": com enum AllergySeverity (@Enumerated(EnumType.STRING)), relacionamento @ManyToOne com Patient.
   - "Vaccine": com campos de lote, fabricante, dose e datas.
   - "ContinuousMedication": com campos de posologia, frequência, rota e flag de ativo.
   - "MedicationHistory": campo "snapshot" mapeado como JSON usando @JdbcTypeCode(SqlTypes.JSON) do Hibernate 6.
   - "PatientDocument": metadados do arquivo e s3Key.

2. Implemente as entidades JPA no módulo "clinical" (pacote "br.edu.ifrn.promptuario.modules.clinical.model"):
   - "DoctorSchedule" e "TimeSlot": com relacionamento OneToMany.
   - "Appointment": relacionamento @ManyToOne com Patient direto (eliminando tabela intermediária legada de projeções), relacionamentos com TimeSlot e MedicalRecord (@OneToOne(mappedBy = "appointment")). Enums AppointmentType e AppointmentStatus como String.
   - "MedicalRecord": campos SOAP (chiefComplaint, anamnesis, physicalExam, diagnosis, diagnosisCodes como List<String> via @JdbcTypeCode(SqlTypes.JSON), treatmentPlan, richNotes como Map<String, Object> via @JdbcTypeCode(SqlTypes.JSON), signatureHash, signedBy, signedAt). Relacionamentos com Prescription, ExamRequest, MedicalCertificate e MedicalRecordHistory.
   - "Prescription": medications mapeado como List<Map<String, Object>> via @JdbcTypeCode(SqlTypes.JSON), validDays, pdfS3Key, pdfGeneratedAt, signatureHash.
   - "ExamRequest": examType, urgency (ExamUrgency), result, resultDate.
   - "MedicalCertificate": reason, daysOff, startDate, notes, pdfS3Key.
   - Entidades imutáveis de histórico: MedicalRecordHistory, PrescriptionHistory, ExamRequestHistory, MedicalCertificateHistory, todas com campo "snapshot" JSONB.

3. Implemente a entidade "AuditLog" em "br.edu.ifrn.promptuario.shared.model.AuditLog" para registrar auditoria de operações.

4. Crie as interfaces Spring Data JPA:
   - "PatientRepository": métodos de busca por user_id, por CPF, busca paginada filtrando por nome ignorando case ("findByFullNameContainingIgnoreCase").
   - "AppointmentRepository": busca com paginação e filtros (patientId, doctorId, status, range de datas), busca por conflito de horário ("existsByDoctorIdAndScheduledAtAndStatusNot").
   - "MedicalRecordRepository": busca por appointmentId, busca paginada do histórico clínico por patientId ordenado por createdAt DESC.
   - "PrescriptionRepository", "ExamRequestRepository", "DoctorScheduleRepository", "TimeSlotRepository", "AllergyRepository", "VaccineRepository", "ContinuousMedicationRepository", "PatientDocumentRepository".

Gere o código completo com imports Jakarta (`jakarta.persistence.*`), anotações de validação e construtores necessários.
```

### Critérios de Aceite
- Mapeamento correto de todas as colunas JSONB com `@JdbcTypeCode(SqlTypes.JSON)`.
- Chaves estrangeiras e deleções em cascata alinhadas ao script Flyway da Fase 2.

---

## Fase 4: Java Records (DTOs) e Validações de Contrato Jakarta

### Objetivo
Traduzir todos os esquemas Pydantic legados (`patient_schemas.py` e `clinical_schemas.py`) para **Java Records imutáveis**, decorados com anotações de Jakarta Validation (`@NotBlank`, `@NotNull`, `@Pattern`, `@Min`, `@Max`, `@Size`), assegurando paridade total de contrato JSON na API.

### Arquivos Alvo
- `modules/patient/dto/`: `PatientCreateDto.java`, `PatientUpdateDto.java`, `PatientResponseDto.java`, `PatientListResponseDto.java`, `PatientSummaryResponseDto.java`, `AllergyDto.java`, `VaccineDto.java`, `MedicationDto.java`, `DocumentDto.java`.
- `modules/clinical/dto/`: `AppointmentCreateDto.java`, `AppointmentCancelDto.java`, `AppointmentResponseDto.java`, `AppointmentListResponseDto.java`, `MedicalRecordCreateDto.java`, `MedicalRecordUpdateDto.java`, `MedicalRecordResponseDto.java`, `MedicalRecordHistoryResponseDto.java`, `PrescriptionCreateDto.java`, `PrescriptionResponseDto.java`, `ExamRequestCreateDto.java`, `ExamResultUpdateDto.java`, `ExamRequestResponseDto.java`, `MedicalCertificateCreateDto.java`, `MedicalCertificateResponseDto.java`, `ScheduleCreateDto.java`, `TimeSlotDto.java`.

### Prompt da Fase 4

```text
Atue como Engenheiro de Software especialista em API Design e Java 21.
Nosso objetivo é implementar todos os Data Transfer Objects (DTOs) como Java Records imutáveis para os módulos "patient" e "clinical", garantindo correspondência 1-para-1 com os contratos JSON consumidos pelo Frontend e pelo API Gateway.

Pacote de pacientes: "br.edu.ifrn.promptuario.modules.patient.dto"
Pacote clínico: "br.edu.ifrn.promptuario.modules.clinical.dto"

Implemente os seguintes Records com validações Jakarta (jakarta.validation.constraints.*):

1. DTOs de Pacientes:
   - "PatientCreateDto": (
       @NotBlank String fullName,
       @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{11}$", message = "CPF inválido") String cpf,
       LocalDate dateOfBirth,
       GenderType gender,
       String bloodType,
       String phone,
       @Email String email,
       String street, String city, String state, String zipCode,
       String emergencyName, String emergencyPhone, String emergencyRelation,
       String notes,
       String userId // opcional se enviado no body ou injetado via token
     )
   - "PatientUpdateDto": com campos opcionais para atualização cadastral parcial.
   - "PatientResponseDto": refletindo todos os dados do paciente, incluindo listas resumidas de alergias, medicações ativas e datas formatadas em ISO-8601.
   - "PatientListResponseDto": (List<PatientResponseDto> items, long total, int page, int size).
   - "PatientSummaryResponseDto": resumo consolidado (dados vitais, alergias ativas, medicações ativas, última consulta).
   - "AllergyCreateDto", "AllergyResponseDto".
   - "VaccineCreateDto", "VaccineResponseDto".
   - "MedicationCreateDto", "MedicationUpdateDto", "MedicationResponseDto".
   - "DocumentUploadResponseDto", "DocumentResponseDto".

2. DTOs Clínicos:
   - "AppointmentCreateDto": (
       @NotBlank String patientId,
       @NotBlank String doctorId,
       String slotId,
       @NotNull Instant scheduledAt,
       AppointmentType appointmentType,
       String specialty,
       String notes
     )
   - "AppointmentCancelDto": (@NotBlank String reason)
   - "AppointmentResponseDto": com dados do agendamento, nome do paciente e nome do médico.
   - "AppointmentListResponseDto": (List<AppointmentResponseDto> items, long total, int page, int size).
   - "MedicalRecordCreateDto": (
       @NotBlank String appointmentId,
       @NotBlank String patientId,
       @NotBlank String chiefComplaint,
       String anamnesis,
       String physicalExam,
       String diagnosis,
       List<String> diagnosisCodes,
       String treatmentPlan,
       String observations,
       Map<String, Object> richNotes
     )
   - "MedicalRecordResponseDto": dados completos do prontuário, histórico, prescrições e exames associados.
   - "PrescriptionCreateDto": (
       @NotEmpty List<MedicationItemDto> medications,
       String instructions,
       @Min(1) @Max(365) Integer validDays
     ) onde MedicationItemDto contém (String name, String dosage, String frequency, Integer durationDays, String instructions).
   - "ExamRequestCreateDto", "ExamResultUpdateDto", "ExamRequestResponseDto".
   - "MedicalCertificateCreateDto": (@NotBlank String reason, @NotNull @Min(1) Integer daysOff, @NotNull LocalDate startDate, String notes).
   - "ScheduleCreateDto", "TimeSlotDto".

Inclua métodos utilitários estáticos "fromEntity(...)" e "toEntity(...)" para facilitar a conversão desacoplada.
```

### Critérios de Aceite
- Compilação sem avisos de tipos não serializáveis.
- Regras de validação de CPF, e-mail e campos obrigatórios alinhadas aos schemas Pydantic de origem.

---

## Fase 5: Serviços de Domínio e Transacionalidade de Pacientes (Módulo Patient)

### Objetivo
Implementar as classes de serviço do módulo de pacientes (`PatientService`, `AllergyService`, `VaccineService`, `MedicationService`, `PatientDocumentService`), encapsulando regras de negócio, transações ACID (`@Transactional`), anonimização LGPD e registro de auditoria.

### Arquivos Alvo
- `modules/patient/service/PatientService.java`
- `modules/patient/service/AllergyService.java`
- `modules/patient/service/VaccineService.java`
- `modules/patient/service/MedicationService.java`
- `modules/patient/service/PatientDocumentService.java`
- `shared/audit/AuditService.java`

### Prompt da Fase 5

```text
Atue como Engenheiro de Backend especializado em regras de negócio e transacionalidade em Spring Boot.
Vamos construir a camada de serviços de domínio para o módulo "patient" em "br.edu.ifrn.promptuario.modules.patient.service".

1. Implemente "PatientService":
   - "createPatient(PatientCreateDto dto, String authenticatedUserId)":
     * Valida unicidade de CPF (se informado) e unicidade de user_id.
     * Persiste o paciente no PostgreSQL.
     * Registra log de auditoria via AuditService.
     * Publica o evento assíncrono "PatientCreatedEvent" (a ser consumido por AI e Reporting).
   - "getPatientById(String id)": busca por ID ou lança ResourceNotFoundException("Paciente não encontrado").
   - "getPatientByUserId(String userId)": busca direta pelo user_id do IAM.
   - "listPatients(int page, int size, String search)": retorna lista paginada com filtro por nome (case insensitive) ou CPF.
   - "updatePatient(String id, PatientUpdateDto dto, String userId)": atualiza dados cadastrais, grava auditoria e dispara "PatientUpdatedEvent".
   - "anonymizePatient(String id, String adminUserId)": executa anonimização LGPD (limpa CPF, e-mail, telefone, endereço, define anonymized=true e isActive=false).

2. Implemente "AllergyService":
   - "addAllergy(String patientId, AllergyCreateDto dto)": vincula alergia ao paciente, salva, grava auditoria e publica "AllergyAddedEvent".
   - "listAllergies(String patientId)": lista todas as alergias do paciente.
   - "deleteAllergy(String patientId, String allergyId)": remove alergia garantindo consistência.

3. Implemente "VaccineService" e "MedicationService":
   - Gerenciamento de vacinas aplicadas e doses subsequentes.
   - Registro de medicação contínua, desativação de medicação e gravação imutável em "MedicationHistory" com snapshot JSON do estado anterior.

4. Implemente "AuditService" em "br.edu.ifrn.promptuario.shared.audit.AuditService":
   - Método genérico "@Transactional(propagation = Propagation.MANDATORY)" para registrar operações (INSERT, UPDATE, DELETE) na tabela "audit_logs".

Garanta que todos os métodos que alterem o banco estejam devidamente anotados com @Transactional(rollbackFor = Exception.class).
```

### Critérios de Aceite
- Lançamento de exceções específicas de domínio (`ResourceNotFoundException`, `ConflictException`, `BusinessException`).
- Isolamento e transacionalidade estrita em operações de escrita.

---

## Fase 6: Serviços de Domínio e Regras Clínicas (Módulo Clinical)

### Objetivo
Implementar a camada de serviços clínicos (`AppointmentService`, `MedicalRecordService`, `PrescriptionService`, `ExamRequestService`, `MedicalCertificateService`, `DoctorScheduleService`), com validação de conflito de agenda, transição atômica de status de consultas, imutabilidade de prontuários com hash SHA-256 e auditoria.

### Arquivos Alvo
- `modules/clinical/service/AppointmentService.java`
- `modules/clinical/service/MedicalRecordService.java`
- `modules/clinical/service/PrescriptionService.java`
- `modules/clinical/service/ExamRequestService.java`
- `modules/clinical/service/MedicalCertificateService.java`
- `modules/clinical/service/DoctorScheduleService.java`

### Prompt da Fase 6

```text
Atue como Engenheiro de Backend especializado em Domínios Clínicos e Prontuário Eletrônico (PEP/EHR).
Vamos construir a camada de serviços de negócio do módulo "clinical" em "br.edu.ifrn.promptuario.modules.clinical.service".

1. "AppointmentService":
   - "createAppointment(AppointmentCreateDto dto, String userId)":
     * Valida existência do paciente (direto na tabela patients).
     * Valida conflito de agenda do médico no mesmo horário (não permite agendamento duplo se status != CANCELLED).
     * Se houver "slotId", valida e marca o TimeSlot como indisponível.
     * Salva o Appointment com status SCHEDULED e publica "AppointmentCreatedEvent".
   - "cancelAppointment(String appointmentId, String reason, String userId, boolean isPatient)":
     * Se for o paciente cancelando, valida a regra de antecedência mínima configurada (ex: mínimo 24h de antecedência).
     * Atualiza status para CANCELLED, preenche cancellationReason, cancelledBy e cancelledAt.
     * Libera o TimeSlot vinculado (se houver).
     * Publica "AppointmentCancelledEvent".
   - "completeAppointment(String appointmentId)": transiciona status para COMPLETED após atendimento.
   - "listAppointments(...)": listagem paginada com múltiplos filtros (médico, paciente, status, período).

2. "MedicalRecordService":
   - "createRecord(MedicalRecordCreateDto dto, String doctorId)":
     * Valida se a consulta (appointmentId) existe e se já não possui prontuário vinculado (relação 1-para-1).
     * Preenche os campos SOAP (Subjetivo, Objetivo, Avaliação, Plano).
     * Calcula o hash de integridade SHA-256 sobre os dados clínicos (chiefComplaint + diagnosis + treatmentPlan + doctorId + timestamp).
     * Preenche signatureHash, signedBy=doctorId, signedAt=Instant.now().
     * Salva o registro e cria a entrada inicial em "MedicalRecordHistory" com snapshot JSONB do registro completo.
     * Conclui a consulta associada (Appointment -> COMPLETED).
     * Publica "MedicalRecordCreatedEvent".
   - "updateRecord(String recordId, MedicalRecordUpdateDto dto, String doctorId)":
     * Apenas o médico autor ou administrador pode editar.
     * Cria novo snapshot imutável em "MedicalRecordHistory" com change_type = "UPDATE".
     * Recalcula novo hash de integridade e atualiza updated_at.
   - "getPatientHistory(String patientId)": busca todos os prontuários do paciente ordenados cronologicamente com prescrições e exames vinculados.

3. "PrescriptionService", "ExamRequestService" e "MedicalCertificateService":
   - Emissão de receitas estruturadas com cálculo de hash. Disparo assíncrono para a fila RabbitMQ "prescription.pdf.generation".
   - Solicitação de exames clínicos com urgência e registro de resultados laboratoriais.
   - Emissão de atestados médicos com CID e período de afastamento.

Todos os métodos devem ter controle transacional rigoroso com @Transactional.
```

### Critérios de Aceite
- Garantia de que consultas canceladas liberam os slots de horário correspondentes.
- Imutabilidade e rastreabilidade total do prontuário médico em `MedicalRecordHistory`.

---

## Fase 7: Integração de Storage S3 / MinIO (Documentos e Receitas)

### Objetivo
Configurar o cliente do AWS SDK for Java 2.x (`S3Client`) e implementar o serviço genérico de armazenamento `S3StorageService` para upload multipart, download streaming, geração de URLs pré-assinadas temporárias e validação/criação automática de buckets no MinIO em tempo de inicialização.

### Arquivos Alvo
- `config/S3Config.java`
- `shared/storage/S3StorageService.java`
- `modules/patient/service/PatientDocumentService.java` (atualização do upload)

### Prompt da Fase 7

```text
Atue como Engenheiro de Infraestrutura e Integração Cloud especialista em AWS SDK for Java 2.x e MinIO.
Implemente o componente de armazenamento em nuvem S3 para o microsserviço "patient-clinical-service".

1. "S3Config.java" em "br.edu.ifrn.promptuario.config":
   - Criar o Bean "S3Client" configurado para MinIO:
     * Endpoint URI customizado (lido de "app.storage.s3.endpoint").
     * Credenciais estáticas (access-key e secret-key).
     * Região padrão configurável ("us-east-1").
     * Habilitar "forcePathStyle(true)" (essencial para MinIO).
   - Criar o Bean "S3Presigner" para geração de URLs pré-assinadas de download seguro.
   - Inicializador (@EventListener(ApplicationReadyEvent.class) ou CommandLineRunner):
     * Verifica e cria automaticamente os buckets se não existirem:
       - "patient-documents"
       - "prescriptions"

2. "S3StorageService.java" em "br.edu.ifrn.promptuario.shared.storage":
   - "uploadFile(String bucket, String key, byte[] content, String contentType)": realiza putObject via S3Client.
   - "uploadInputStream(String bucket, String key, InputStream inputStream, long contentLength, String contentType)".
   - "downloadFile(String bucket, String key)": retorna ResponseInputStream<GetObjectResponse> para streaming direto.
   - "generatePresignedDownloadUrl(String bucket, String key, Duration duration)": gera URL segura temporária (ex: validade de 15 minutos).
   - "deleteFile(String bucket, String key)".

3. Integrar com "PatientDocumentService":
   - "uploadPatientDocument(String patientId, MultipartFile file, String title, String category, String uploadedBy)":
     * Valida tamanho máximo e tipos MIME permitidos (PDF, PNG, JPEG).
     * Gera chave estruturada: "patients/{patientId}/{uuid}_{filename}".
     * Faz upload no bucket "patient-documents".
     * Persiste os metadados na tabela "patient_documents".
     * Retorna "DocumentResponseDto".

Gere código resiliente com tratamento de S3Exception e logs estruturados em caso de falha.
```

### Critérios de Aceite
- Suporte pleno a `forcePathStyle(true)` para compatibilidade total com o MinIO local.
- Criação idempotente dos buckets no bootstrap da aplicação.

---

## Fase 8: Mensageria RabbitMQ (Event-Driven: Publishers e IAM Consumers)

### Objetivo
Configurar o Spring AMQP com `RabbitTemplate`, `Jackson2JsonMessageConverter`, exchanges de tópicos (`domain.events` e `iam.events`), filas dedicadas e implementar os publicadores de eventos de negócio e o consumidor idempotente de eventos do IAM (`UserCreatedEvent` e `UserDeactivatedEvent`).

### Arquivos Alvo
- `config/RabbitMQConfig.java`
- `shared/event/`: Records de eventos (`PatientCreatedEvent`, `PatientUpdatedEvent`, `AllergyAddedEvent`, `AppointmentCreatedEvent`, `AppointmentCancelledEvent`, `MedicalRecordCreatedEvent`, `PrescriptionGeneratedEvent`, `UserCreatedEvent`, `UserDeactivatedEvent`).
- `shared/messaging/EventPublisher.java`
- `shared/messaging/IamEventConsumer.java`

### Prompt da Fase 8

```text
Atue como Especialista em Arquitetura Orientada a Eventos (EDA) com Spring AMQP e RabbitMQ.
Vamos configurar a mensageria do microsserviço "patient-clinical-service" para publicar eventos de domínio e consumir eventos de usuários do IAM Service.

1. "RabbitMQConfig.java" em "br.edu.ifrn.promptuario.config":
   - Declarar Beans:
     * TopicExchange "domain.events" (durable).
     * TopicExchange "iam.events" (durable).
     * Queue "patient_clinical.iam_events" (durable).
     * Binding: ligar fila "patient_clinical.iam_events" à exchange "iam.events" com as routing keys:
       - "user.created"
       - "user.deactivated"
     * MessageConverter configurado como Jackson2JsonMessageConverter para serialização JSON transparente de Records.

2. Records de Eventos em "br.edu.ifrn.promptuario.shared.event":
   - Estrutura base padrão com: eventId (UUID), timestamp (Instant), eventType (String), correlationId (String).
   - Eventos Publicados:
     * PatientCreatedEvent (routing_key: "patient.created")
     * PatientUpdatedEvent (routing_key: "patient.updated")
     * AllergyAddedEvent (routing_key: "patient.allergy.added")
     * AppointmentCreatedEvent (routing_key: "appointment.created")
     * AppointmentCancelledEvent (routing_key: "appointment.cancelled")
     * MedicalRecordCreatedEvent (routing_key: "medical_record.created")
     * PrescriptionGeneratedEvent (routing_key: "prescription.generated")
   - Eventos Consumidos:
     * UserCreatedEvent (payload com userId, email, fullName, role, cpf, phone, etc.)
     * UserDeactivatedEvent (payload com userId, reason, timestamp)

3. "EventPublisher.java" em "br.edu.ifrn.promptuario.shared.messaging":
   - Injeta RabbitTemplate e expõe método genérico:
     "publish(String exchange, String routingKey, Object event)"
   - Adiciona headers de correlationId e timestamp na mensagem.

4. "IamEventConsumer.java" em "br.edu.ifrn.promptuario.shared.messaging":
   - Listener com @RabbitListener(queues = "patient_clinical.iam_events"):
     * Trata "UserCreatedEvent": se role == "PATIENT", cria perfil de paciente inicial de forma idempotente se ainda não existir.
     * Trata "UserDeactivatedEvent":
       1. Localiza o paciente associado pelo user_id.
       2. Inativa o cadastro (isActive = false).
       3. Busca todas as consultas futuras (scheduledAt > now e status IN ('SCHEDULED', 'CONFIRMED')) e as cancela (status = CANCELLED, cancellationReason = 'Usuário desativado no IAM').
       4. Grava auditoria correspondente.
       5. Tudo executado em transação atômica única (@Transactional).
     * Mecanismo de Idempotência: controle por "eventId" em cache/banco para ignorar mensagens duplicadas.

Garanta conformidade estrita com o formato dos eventos consumidos e emitidos pelo ecossistema do Projeto Integrador.
```

### Critérios de Aceite
- Consumo e publicação serializados em JSON padrão compatível com a biblioteca `shared.events` do ecossistema Python.
- Operação atômica completa de desativação de usuário e cancelamento de agendamentos futuros.

---

## Fase 9: Worker Assíncrono de Geração de PDFs de Prescrições (Substituição Celery)

### Objetivo
Substituir a tarefa legada Celery (`clinical.generate_prescription_pdf` via Redis DB 3) por um consumidor Spring AMQP nativo escutando na fila `prescription.pdf.generation`, renderizando o template HTML com Thymeleaf, compilando em PDF via OpenPDF / Flying Saucer, fazendo upload no bucket MinIO `prescriptions` e atualizando o registro no banco via JPA.

### Arquivos Alvo
- `config/PdfWorkerConfig.java`
- `modules/clinical/service/PrescriptionPdfWorker.java`
- `src/main/resources/templates/prescription-template.html`
- `src/main/resources/templates/certificate-template.html`

### Prompt da Fase 9

```text
Atue como Engenheiro de Backend especializado em Processamento Assíncrono e Geração de Documentos em Java.
Nosso objetivo é substituir o worker Celery legado de geração de PDF por um worker nativo em Spring Boot 3.

1. "PdfWorkerConfig.java":
   - Declarar Queue "prescription.pdf.generation" (durable).
   - Binding na exchange "domain.events" com routing key "prescription.pdf.generate" (ou fila direta).
   - Configurar ThreadPoolTaskExecutor com pool dedicado para processamento paralelo de PDFs.

2. Templates Thymeleaf ("src/main/resources/templates/"):
   - "prescription-template.html": layout limpo e profissional idêntico ao WeasyPrint original:
     * Cabeçalho corporativo com título "Prescrição Médica".
     * Grid com Nome do Paciente, CPF, Data de Nascimento, Médico Responsável e CRM.
     * Lista estilizada de medicamentos (nome, dosagem, frequência, duração em dias, instruções).
     * Instruções gerais, data de emissão e prazo de validade (validDays).
     * Rodapé com hash de assinatura digital SHA-256 e aviso de autenticidade eletrônica.
   - "certificate-template.html": layout equivalente para Atestados Médicos (dias de afastamento, CID, motivo).

3. "PrescriptionPdfWorker.java" em "br.edu.ifrn.promptuario.modules.clinical.service":
   - Método @RabbitListener(queues = "prescription.pdf.generation"):
     1. Recebe mensagem contendo "prescriptionId".
     2. Carrega a Prescrição e dados do Paciente/Médico via JPA em modo somente leitura.
     3. Popula o Context do Thymeleaf com as variáveis do relatório.
     4. Processa o HTML resultante.
     5. Converte o HTML para PDF usando "com.github.librepdf:openpdf" ou Flying Saucer (ITextRenderer).
     6. Faz o upload do PDF resultante para o MinIO via S3StorageService no bucket "prescriptions":
        - Chave: "prescriptions/{patientId}/{prescriptionId}.pdf"
     7. Atualiza na entidade Prescription:
        - "pdfS3Key" = chave S3 gerada.
        - "pdfGeneratedAt" = Instant.now().
     8. Salva a atualização no banco.
     9. Dispara o evento "PrescriptionGeneratedEvent" para notificação downstream.

Gere código com tratamento de retry em caso de falha transitória e DLQ (Dead Letter Queue) configurada.
```

### Critérios de Aceite
- Eliminação definitiva da dependência do Celery e do Redis DB 3 para o Patient-Clinical Service.
- Geração de PDFs visualmente idênticos aos gerados pelo WeasyPrint anterior.

---

## Fase 10: Segurança OAuth2 Resource Server (JWT) e Tratamento Global de Erros

### Objetivo
Configurar o Spring Security como OAuth2 Resource Server validando tokens JWT assinados com HMAC-SHA256 emitidos pelo IAM Service, converter a claim `role` em `GrantedAuthority` com prefixo `ROLE_`, aplicar controle de acesso via anotações `@PreAuthorize`, e implementar o `@RestControllerAdvice` para padronização de respostas de erro da API.

### Arquivos Alvo
- `config/SecurityConfig.java`
- `config/JwtAuthenticationConverter.java`
- `shared/exception/GlobalExceptionHandler.java`
- `shared/exception/ApiErrorResponse.java`
- Exceções customizadas (`ResourceNotFoundException`, `ConflictException`, `BusinessException`, `AccessDeniedException`).

### Prompt da Fase 10

```text
Atue como Especialista em Segurança de Microsserviços e Spring Security 6.
Vamos configurar o Spring Security OAuth2 Resource Server com validação de JWT simétrico (HMAC-SHA256) e tratamento global de erros para o "patient-clinical-service".

1. "SecurityConfig.java" e "JwtAuthenticationConverter.java":
   - Configurar o "SecurityFilterChain":
     * Desabilitar CSRF (arquitetura stateless REST).
     * Sessão STATELESS.
     * Endpoints públicos (permit all):
       - "/healthz"
       - "/actuator/health/**", "/actuator/prometheus"
       - "/swagger-ui/**", "/v3/api-docs/**", "/docs"
       - "/api/v1/schedules/*/available-slots"
     * Todos os demais endpoints requerem autenticação.
     * Configurar "oauth2ResourceServer(oauth2 -> oauth2.jwt(...))".
   - Bean "JwtDecoder":
     * Criado a partir da chave simétrica "app.security.jwt.secret" com segredo HMAC-SHA256 (SecretKeySpec com algoritmo "HmacSHA256").
   - Conversor de Claims JWT ("JwtAuthenticationConverter"):
     * O IAM Service emite o token com as claims:
       - "sub": ID do usuário (UUID).
       - "email": e-mail do usuário.
       - "role": role do usuário ("ADMIN", "DOCTOR", "PATIENT", "ATTENDANT").
     * Mapear a claim "role" para GrantedAuthority: "ROLE_" + role.toUpperCase().
     * Disponibilizar o objeto de autenticação customizado ou permitir extrair o userId facilmente via @AuthenticationPrincipal.

2. Classes de Exceção e DTO de Erro:
   - "ApiErrorResponse.java" (Record imutável):
     * timestamp (Instant), status (int), error (String), message (String), path (String), errors (Map<String, String> detalhando erros de validação campo a campo).
   - Exceções: ResourceNotFoundException, ConflictException, BusinessException, ForbiddenException.

3. "GlobalExceptionHandler.java" (@RestControllerAdvice):
   - Tratamento de @ExceptionHandler:
     * ResourceNotFoundException -> HTTP 404.
     * MethodArgumentNotValidException e ConstraintViolationException -> HTTP 400 (com lista de campos inválidos).
     * ConflictException -> HTTP 409.
     * AccessDeniedException -> HTTP 403.
     * Exception genérica -> HTTP 500 com mensagem amigável sem vazar stack trace.

Gere o código completo, garantindo que o formato JSON retornado nos erros seja exatamente idêntico ao padrão emitido pelo FastAPI legado.
```

### Critérios de Aceite
- Decodificação perfeita do JWT gerado pelo IAM Service em Python sem alteração no segredo.
- Respostas de erro padronizadas em conformidade com o Gateway.

---

## Fase 11: Controladores REST, Endpoints Canônicos e Health Check

### Objetivo
Construir os controladores REST mapeando 100% dos endpoints dos domínios de pacientes, prontuários, agendamentos, receitas, atestados e grades de horários, aplicando autorizações `@PreAuthorize` por role e mantendo os contratos de URL canônicos.

### Arquivos Alvo
- `modules/patient/controller/PatientController.java`
- `modules/clinical/controller/AppointmentController.java`
- `modules/clinical/controller/MedicalRecordController.java`
- `modules/clinical/controller/DoctorScheduleController.java`
- `shared/controller/HealthController.java`

### Prompt da Fase 11

```text
Atue como Desenvolvedor Fullstack/Backend Sênior especializado em Spring Web MVC e OpenAPI/Swagger.
Vamos implementar os Controladores REST com paridade total aos endpoints do FastAPI para o microsserviço unificado "patient-clinical-service".

1. "PatientController.java" (@RestController, @RequestMapping("/api/v1/patients")):
   - GET "" -> Listagem paginada (page, size, search) - @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'ATTENDANT')")
   - POST "" -> Criação de paciente - @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
   - GET "/me" -> Cadastro do paciente autenticado (extrai userId do JWT) - @PreAuthorize("hasRole('PATIENT')")
   - GET "/{id}" -> Detalhe do paciente - @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'ATTENDANT') or #id == authentication.principal.claims['patient_id']")
   - PATCH "/{id}" -> Atualização parcial
   - DELETE "/{id}" -> Anonimização LGPD - @PreAuthorize("hasRole('ADMIN')")
   - POST "/{id}/allergies" e GET "/{id}/allergies" -> Gestão de alergias
   - POST "/{id}/vaccines" e GET "/{id}/vaccines" -> Gestão de vacinas
   - POST "/{id}/medications" e GET "/{id}/medications" -> Gestão de medicações contínuas
   - POST "/{id}/documents/upload" (multipart/form-data) e GET "/{id}/documents" -> Upload e listagem de documentos
   - GET "/{id}/summary" -> Sumário clínico do paciente

2. "AppointmentController.java" (@RestController, @RequestMapping("/api/v1/appointments")):
   - GET "" -> Listagem com filtros (page, size, patientId, doctorId, status, fromDate, toDate, patientName)
   - POST "" -> Agendamento de consulta
   - GET "/{id}" -> Detalhe da consulta
   - PATCH "/{id}/cancel" -> Cancelamento com validação de antecedência
   - PATCH "/{id}/complete" -> Conclusão de atendimento

3. "MedicalRecordController.java" (@RestController, @RequestMapping("/api/v1/records")):
   - POST "" -> Criação de prontuário SOAP estruturado - @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
   - GET "/{id}" -> Busca prontuário por ID
   - PATCH "/{id}" -> Atualização de prontuário com auditoria imutável
   - GET "/patient/{patientId}" -> Histórico clínico completo do paciente
   - POST "/{recordId}/prescriptions" -> Emissão de receita vinculada ao prontuário
   - GET "/{recordId}/prescriptions" -> Listagem de receitas
   - POST "/{recordId}/exams" -> Solicitação de exames
   - PUT "/{recordId}/exams/{examId}/result" -> Inserção de laudo/resultado
   - POST "/{recordId}/certificates" -> Emissão de atestado médico

4. "DoctorScheduleController.java" (@RestController, @RequestMapping("/api/v1/schedules")):
   - POST "" -> Criação de grade de disponibilidade do médico
   - GET "" -> Listagem de grades
   - GET "/{id}/available-slots" -> Horários disponíveis (público)

5. "HealthController.java" (@RestController):
   - GET "/healthz" -> Retorna JSON {"status": "ok", "service": "patient-clinical-service", "version": "1.0.0-java"} compatível com o endpoint consumido pelo health check agregador do Gateway.

Gere o código completo com anotações OpenAPI (@Operation, @ApiResponse, @Tag) para geração de documentação Swagger interativa em "/docs".
```

### Critérios de Aceite
- Mapeamento exato de todos os paths de URL documentados em `PLANEJAMENTO_DE_ENDPOINTS_PI.md`.
- Verificação de autorização RBAC ativa em todos os endpoints sensíveis.

---

## Fase 12: Dockerfile Multi-Stage, Docker Compose e Testes de Integração

### Objetivo
Criar o `Dockerfile` multi-stage otimizado com Eclipse Temurin 21, atualizar a definição do serviço no `docker-compose.yml` (substituindo o serviço Python e removendo o container worker Celery legado) e implementar testes de integração com JUnit 5 e Testcontainers para validação ponta a ponta.

### Arquivos Alvo
- `backend-java/patient-clinical-service/Dockerfile`
- `backend/docker-compose.yml` (atualização)
- `backend-java/patient-clinical-service/src/test/java/br/edu/ifrn/promptuario/PatientClinicalIntegrationTest.java`

### Prompt da Fase 12

```text
Atue como Engenheiro de DevOps e QA especializado em Docker e Testcontainers para Spring Boot.
Vamos finalizar a migração com a containerização do microsserviço e a suíte de testes de integração.

1. "Dockerfile" em "backend-java/patient-clinical-service/Dockerfile":
   - Multi-stage build otimizado para produção:
     * Stage 1: Build com "maven:3.9-eclipse-temurin-21-alpine", copiando pom.xml, baixando dependências em cache e compilando o JAR sem testes ("mvn clean package -DskipTests").
     * Stage 2: Runtime leve com "eclipse-temurin:21-jre-alpine", criando usuário não-root "appuser", copiando o JAR executável ("app.jar"), expondo a porta 8000 e configurando JVM flags otimizadas para containers (-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0).
     * Health check interno usando wget no endpoint "/healthz".

2. Atualização no "backend/docker-compose.yml":
   - Atualizar a seção do "patient-clinical-service":
     * Trocar o build context para pointing no serviço Java: "context: ../backend-java/patient-clinical-service".
     * Manter as portas publicadas: "8002:8000".
     * Atualizar variáveis de ambiente para a nomenclatura do Spring Boot:
       - SPRING_DATASOURCE_URL=jdbc:postgresql://db-patient-clinical:5432/patient_clinical_db
       - SPRING_DATASOURCE_USERNAME=clinical
       - SPRING_DATASOURCE_PASSWORD=clinical_pass
       - SPRING_RABBITMQ_HOST=rabbitmq
       - APP_STORAGE_S3_ENDPOINT=http://minio:9000
       - APP_STORAGE_S3_ACCESS_KEY=${MINIO_ROOT_USER:-minioadmin}
       - APP_STORAGE_S3_SECRET_KEY=${MINIO_ROOT_PASSWORD:-minioadmin}
       - APP_SECURITY_JWT_SECRET=${JWT_SECRET_KEY}
     * Desativar formalmente ou remover o container "patient-clinical-worker" (Celery), pois o processamento assíncrono agora ocorre nativamente dentro do container Java através do Spring AMQP Worker implementado na Fase 9.

3. "PatientClinicalIntegrationTest.java":
   - Teste de integração completo usando @SpringBootTest e Testcontainers:
     * Containers: PostgreSQL 15, RabbitMQ e MinIO.
     * Testa o fluxo completo:
       1. Criação de paciente via POST /api/v1/patients.
       2. Agendamento de consulta via POST /api/v1/appointments.
       3. Atendimento e registro de prontuário SOAP via POST /api/v1/records com prescrição.
       4. Validação da publicação de eventos no RabbitMQ.
       5. Validação da geração e armazenamento do PDF no bucket S3.
       6. Validação do Health check /healthz.

Forneça os arquivos completos e comandos prontos para validação.
```

### Critérios de Aceite
- Build da imagem Docker bem-sucedido com imagem final inferior a 250MB.
- Subida de todos os serviços com `docker compose up -d` e passagem de 100% dos testes de integração.
- API Gateway roteando chamadas para a porta `8002` com respostas HTTP 200/201 idênticas.

---

## Matriz de Rastreabilidade e Riscos Técnicos

| Risco Identificado | Severidade | Medida Mitigatória nos Prompts |
| :--- | :--- | :--- |
| **Divergência no segredo JWT** | Crítica | A Fase 10 força o uso explícito do algoritmo HMAC-SHA256 (`HmacSHA256`) com a mesma variável de ambiente `JWT_SECRET_KEY`. |
| **Colunas JSON no PostgreSQL** | Alta | A Fase 3 e 4 padronizam o uso de `@JdbcTypeCode(SqlTypes.JSON)` do Hibernate 6, evitando erros de serialização no PostgreSQL. |
| **MinIO Path-Style Access** | Média | A Fase 7 configura `forcePathStyle(true)` no `S3Client`, impedindo falhas de resolução de DNS para subdomínios de bucket. |
| **Conflito de portas no Docker** | Alta | Porta interna mantida em `8000` e porta externa mapeada para `8002:8000`, preservando as rotas do Gateway configuradas em `PATIENT_CLINICAL_SERVICE_URL`. |
| **Orfandade de Tarefas Celery** | Alta | A Fase 9 migra a fila de geração de PDFs diretamente para RabbitMQ nativo, permitindo o desligamento seguro do Celery/Redis DB 3. |

---

## Comandos Úteis para o Ciclo de Desenvolvimento

```bash
# 1. Compilar e rodar testes locais no serviço Java:
cd backend-java/patient-clinical-service
mvn clean test

# 2. Executar a aplicação localmente com profile de desenvolvimento:
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Construir a imagem Docker:
docker build -t promptuario/patient-clinical-service:latest -f Dockerfile .

# 4. Subir a stack unificada completa via Docker Compose:
cd ../../backend
docker compose up -d --build patient-clinical-service

# 5. Validar o health check do novo serviço:
curl -i http://localhost:8002/healthz
```
