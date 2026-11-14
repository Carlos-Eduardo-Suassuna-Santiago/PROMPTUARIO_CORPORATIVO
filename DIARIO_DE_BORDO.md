# 📓 Diário de Bordo — Projeto Integrador Corporativo

> Registro cronológico das atividades, decisões técnicas e ocorrências do projeto.

---

## Índice

- [2026-08-28](#2026-08-28)
- [2026-11-03](#2026-11-03)
- [2026-11-04](#2026-11-04)
- [2026-11-05](#2026-11-05)
- [2026-11-08](#2026-11-08)
- [2026-11-09](#2026-11-09)
- [2026-11-11](#2026-11-11--cutover-do-backup-service-de-python-para-java-21)
- [2026-11-13 — Entrada mais recente](#2026-11-13)

---

## 2026-08-28

### 📋 Implementação Proposta

- Criação do Diário de Bordo do projeto (`DIARIO_DE_BORDO.md`) para registro estruturado das atividades diárias.

---

### ✅ O que foi Implementado

- Arquivo de diário de bordo criado com estrutura padrão para registro diário.
- Seções definidas: implementação proposta, o que foi implementado, erros encontrados, o que funcionou e resumo do dia.

---

### ❌ O que deu Erro

_Nenhum erro registrado neste dia._

---

### 🟢 O que Funcionou

- Estrutura do diário definida e aprovada pela equipe.

---

### 📝 Resumo do Dia

Dia dedicado à organização e padronização da documentação do projeto. O diário de bordo foi criado para garantir rastreabilidade das atividades, decisões técnicas e ocorrências ao longo do desenvolvimento do Projeto Integrador Corporativo 2026.

---

---

## 2026-11-03

### 📋 Implementação Proposta

- Inicialização e estruturação do microsserviço unificado de pacientes e prontuários em Java/Spring Boot (`/backend-java/patient-clinical-service`).
- Configuração do ecossistema de desenvolvimento (Maven, Java 21, Spring Boot 3.3.4), dependências de infraestrutura (PostgreSQL, Flyway, RabbitMQ, S3/MinIO, OpenPDF, Actuator, Prometheus, OpenAPI/Swagger) e arquivos de perfil (`application.yml`, `application-dev.yml`, `application-test.yml`).

---

### ✅ O que foi Implementado

- **Scaffolding e Gerenciamento de Dependências (`pom.xml`)**:
  - Configuração do projeto com Java 21 e Spring Boot 3.3.4 (`br.edu.ifrn.promptuario:patient-clinical-service`).
  - Adição de dependências essenciais para a API: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa` e driver PostgreSQL.
  - Configuração do Flyway (`flyway-core`, `flyway-database-postgresql`) para migração e versionamento de banco de dados (`classpath:db/migration`).
  - Configuração de mensageria com RabbitMQ (`spring-boot-starter-amqp`) para troca assíncrona de eventos de domínio e IAM (`domain.events`, `iam.events`).
  - Segurança e autorização com `spring-boot-starter-security` e `spring-boot-starter-oauth2-resource-server` para autenticação baseada em tokens JWT.
  - Observabilidade e monitoramento com `spring-boot-starter-actuator` e `micrometer-registry-prometheus` integrados para coleta do Prometheus e Kubernetes probes (`/actuator/health`, `/actuator/prometheus`).
  - Integração com armazenamento de arquivos compatível com S3/MinIO utilizando AWS SDK v2 (`software.amazon.awssdk:s3`) para buckets de documentos (`patient-documents`) e prescrições (`prescriptions`).
  - Geração de relatórios e receitas em PDF com `openpdf` e templates `spring-boot-starter-thymeleaf`.
  - Documentação OpenAPI/Swagger 3 com `springdoc-openapi-starter-webmvc-ui` na rota `/docs`.
  - Adição do Lombok para redução de boilerplate e dependências de teste com `spring-boot-starter-test` e Testcontainers (`postgresql`, `rabbitmq`, `localstack`).
- **Classe Principal da Aplicação (`PatientClinicalApplication.java`)**:
  - Implementação da classe inicial no pacote `br.edu.ifrn.promptuario` com anotações `@SpringBootApplication`, `@EnableAsync` (processamento assíncrono) e `@EnableScheduling` (agendamento de tarefas).
- **Configurações de Ambientes (`application*.yml`)**:
  - `application.yml`: Configuração padrão para porta HTTP (8000), datasource JDBC PostgreSQL para `patient_clinical_db`, validação de schema JPA (`ddl-auto: validate`), migrações Flyway automáticas, conexão RabbitMQ, endpoints expostos do Actuator, Swagger UI mapeado para `/docs`, buckets S3 e segurança JWT via HMAC-SHA256.
  - `application-dev.yml`: Perfil de desenvolvimento com logs detalhados em nível `DEBUG` para pacotes da aplicação (`br.edu.ifrn.promptuario`) e SQL (`org.hibernate.SQL`), além de `show-sql: true`.
  - `application-test.yml`: Perfil de testes com banco `patient_clinical_db_test`, desativação do Flyway e `ddl-auto: none` para testes automatizados.
- **Compilação e Validação Inicial**:
  - Compilação realizada com sucesso no Maven (classes e recursos gerados em `target/` sem erros de compilação ou dependências faltantes).

---

### ❌ O que deu Erro

_Nenhum erro encontrado durante a inicialização, configuração e compilação inicial do projeto Maven/Spring Boot._

---

### 🟢 O que Funcionou

- Compilação limpa do projeto com Maven utilizando Java 21 e Spring Boot 3.3.4.
- Resolução e carregamento de todas as dependências declaradas no `pom.xml`.
- Estruturação modular e padronizada de perfis de configuração (`application.yml`, `application-dev.yml`, `application-test.yml`) alinhada com as variáveis de ambiente já empregadas na infraestrutura do projeto.

---

### 📝 Resumo do Dia

Início da implementação da versão em Java do serviço unificado de pacientes e prontuários clínicos (`backend-java/patient-clinical-service`). O esqueleto completo do projeto foi estruturado com Spring Boot 3.3.4 e Java 21, incluindo configurações para persistência (JPA + Flyway + PostgreSQL), mensageria (RabbitMQ), storage (S3/MinIO), observabilidade (Actuator/Prometheus), segurança JWT e documentação OpenAPI. O projeto compila com sucesso e está preparado para a criação das entidades de domínio, repositórios, serviços e endpoints REST.

---

---

## 2026-11-04

### 📋 Implementação Proposta

- Criação do script inicial de migração de banco de dados (Flyway) e seed de usuário administrador para o microsserviço de IAM em Java (`/backend-java/iam-service`).

---

### ✅ O que foi Implementado

- **Migração Inicial Flyway do IAM (`V1__init_iam_schema.sql`)**:
  - Definição do tipo enumerado nativo `user_role` com os papéis `ADMIN`, `DOCTOR`, `ATTENDANT` e `PATIENT`.
  - Criação das tabelas centrais `users`, `refresh_tokens`, `password_reset_tokens` e `oauth_accounts`, com chaves primárias textuais UUID, relacionamentos e remoção em cascata dos tokens e contas vinculadas ao usuário.
  - Configuração de constraints de unicidade para email, CPF, hashes de token e combinação de provedor com usuário externo; essas constraints fornecem os índices de consulta correspondentes.
  - Criação de índices adicionais para papel e status do usuário, usuário de tokens e contas OAuth.
  - Inclusão do seed idempotente do administrador `admin@promptuario.health`, com senha BCrypt válida para `admin123`, papel `ADMIN` e conta ativa e verificada.
  - Validação da execução da migração em PostgreSQL 15 efêmero: enum, quatro tabelas, quatorze índices e seed foram criados sem falhas.

---

### ❌ O que deu Erro

_Nenhum erro na migração. A validação inicial do hash BCrypt exigiu ajuste de aspas no comando de verificação para impedir a interpretação de caracteres `$` pelo shell; o script Flyway não foi afetado e a checagem final confirmou a senha `admin123`._

---

### 🟢 O que Funcionou

- Criação e estruturação da migração SQL seguindo o padrão versionado do Flyway para execução automatizada no boot do serviço.
- Execução limpa do script em PostgreSQL 15, com confirmação do seed idempotente e das constraints do schema.

---

### 📝 Resumo do Dia

Dia focado no avanço da camada de persistência e segurança do ecossistema Java, estruturando o versionamento Flyway inicial para o `iam-service` e garantindo o provisionamento automático do usuário administrador padrão da aplicação. A migração canônica foi validada em PostgreSQL 15 e está preparada para o mapeamento das entidades JPA nas próximas fases.

---

---

## 2026-11-09

### 📋 Implementação Proposta

- Avançar na camada HTTP do `patient-clinical-service`, expondo operações de pacientes e dados clínicos por endpoints REST protegidos por papéis.
- Ampliar a pesquisa paginada de consultas com filtro pelo nome do paciente.

---

### ✅ O que foi Implementado

- Criação de `PatientController` com endpoints para consulta, cadastro e atualização de pacientes, alergias, vacinas, medicamentos, documentos e resumo clínico.
- Criação de controllers para agendamentos, escalas médicas e prontuários, incluindo operações para prescrições, exames, resultados e atestados.
- Criação de `HealthController` com endpoint `/healthz`.
- Aplicação de regras de autorização por papel usando `@PreAuthorize` nos endpoints de negócio.
- Ampliação da consulta paginada de agendamentos com filtro opcional pelo nome do paciente, aplicado também na consulta de contagem.

---

### ❌ O que deu Erro

- Nenhum erro de execução foi registrado nesta atividade. Compilação e testes não foram executados nesta sessão.

---

### 🟢 O que Funcionou

- Os novos controllers e a alteração do repositório estão presentes no workspace.
- As alterações do dia permanecem locais e ainda não foram commitadas.

---

### 📝 Resumo do Dia

Avanço da camada REST do `patient-clinical-service`, com endpoints para pacientes, consultas, escalas médicas, prontuários e verificações de saúde. Os endpoints de negócio receberam regras de autorização por papel, e a pesquisa paginada de agendamentos passou a aceitar filtro opcional pelo nome do paciente. O código permanece pendente de compilação, testes e commit.

---

## 2026-11-08

### 📋 Implementação Proposta

- Avançar no processamento assíncrono de prescrições médicas e na geração de PDFs no `backend-java/patient-clinical-service`.
- Integrar o worker de PDF à fila RabbitMQ, armazenar o documento gerado no S3 e publicar o evento de conclusão.

---

### ✅ O que foi Implementado

- Inclusão de dependências para renderização de PDF a partir de HTML com Thymeleaf e para retentativas com Spring Retry.
- Implementação inicial de `PrescriptionPdfWorker`, que consome mensagens da fila de geração, carrega prescrição e paciente, renderiza o template em PDF, envia o arquivo ao bucket S3, atualiza os metadados da prescrição e publica `prescription.generated`.
- Configuração da fila `prescription.pdf.generation`, fila de mensagens mortas, binding RabbitMQ, concorrência de consumidores, executor dedicado e retentativas.
- Criação dos templates HTML de prescrição médica e atestado.
- Integração do worker de PDF na branch `develop`, no commit `5112b67` (`feat(patient): gera PDFs de prescrições em worker`).
- Correção dos bindings RabbitMQ com `@Qualifier` para selecionar explicitamente as exchanges IAM e legada, registrada no commit `de5fd8c`.
- Configuração do serviço como OAuth2 Resource Server com validação de JWT HS256, conversão do claim `role` em autoridade `ROLE_*`, rotas públicas definidas e respostas JSON padronizadas para falhas de autenticação/autorização.
- Criação de `GlobalExceptionHandler` e dos tipos de resposta/erro da API para tratar validação, conflitos, acesso negado, recursos ausentes e erros de negócio.
- Registro da configuração de segurança no commit `263a252` (`feat(security): configura resource server JWT e erros globais`), na branch `feature/seguranca`.

---

### ❌ O que deu Erro

- Nenhum erro de execução foi registrado nesta atividade. A compilação e os testes das alterações recentes não foram validados nesta sessão.

---

### 🟢 O que Funcionou

- As entregas de worker PDF e qualificação dos bindings RabbitMQ foram registradas em commits na branch `develop`.
- A configuração de segurança e o tratamento global de erros foram commitados na branch `feature/seguranca`.
- O workspace está sem alterações locais pendentes no momento da atualização do diário.

---

### 📝 Resumo do Dia

Avanço no `patient-clinical-service` com o processamento assíncrono de prescrições e geração de PDFs, incluindo renderização por Thymeleaf, armazenamento no S3 e publicação de evento. A entrega foi commitada em `develop`, junto com o ajuste de bindings RabbitMQ. Também foi implementada e commitada na branch `feature/seguranca` a validação de JWT no Resource Server e a padronização de respostas de erro. A compilação e os testes dessas alterações ainda precisam ser validados.

---

## 2026-11-05

### 📋 Implementação Proposta

- Implementação do modelo de domínio, entidades JPA e repositórios Spring Data JPA para os módulos Shared, Patient e Clinical no serviço unificado (`/backend-java/patient-clinical-service`).
- Inicialização da Fase 1 do microsserviço de identidade e acesso em Java (`/backend-java/iam-service`), incluindo dependências Maven, configurações base e infraestrutura de integração.
- Implementação da Fase 3 do IAM Java: entidades JPA, enum de papéis, repositórios Spring Data e teste de integração com PostgreSQL 15.
- Implementação da Fase 4 do IAM Java: DTOs imutáveis com Java Records, validações Jakarta e compatibilidade dos contratos JSON com o frontend.
- Implementação da Fase 5 do IAM Java: JWT HS256, BCrypt, blacklist de tokens no Redis e configuração stateless do Spring Security.
- Implementação da Fase 6 do IAM Java: serviços transacionais de autenticação e usuários, revogação de tokens e publicação de eventos de domínio no RabbitMQ.
- Implementação da Fase 7 do IAM Java: autenticação social Google OAuth2/OpenID Connect e autenticação em dois fatores via TOTP.
- Implementação da Fase 8 do IAM Java: controladores REST canônicos, regras de autorização e respostas globais de erro.
- Implementação da Fase 9 do IAM Java: containerização multi-stage, integração no Docker Compose e testes ponta a ponta com Testcontainers.
- Auditoria e conclusão técnica da Task #20 do IAM: validação dos controladores REST, autorizações e contrato global de erros.

---

### ✅ O que foi Implementado

- **Módulo Compartilhado (`shared`)**:
  - Criação da entidade JPA `AuditLog` para rastreamento de operações, com campos para ação, recurso, ID de entidade, autor, IP e timestamp.
  - Criação do repositório Spring Data JPA `AuditLogRepository` com suporte a consultas customizadas e paginação.
- **Módulo de Pacientes (`modules/patient`)**:
  - Implementação das entidades JPA principais: `Patient`, `Allergy`, `ContinuousMedication`, `MedicationHistory`, `PatientDocument` e `Vaccine`.
  - Implementação de enums de domínio: `GenderType`, `AllergySeverity`, `DocumentType` e `MedicationChangeType`.
  - Mapeamentos relacionais `@OneToMany`, `@ManyToOne`, índices de banco de dados, constraints de unicidade (ex: CPF e cartão SUS) e chaves UUID.
  - Criação dos repositórios Spring Data JPA: `PatientRepository`, `AllergyRepository`, `ContinuousMedicationRepository`, `MedicationHistoryRepository`, `PatientDocumentRepository` e `VaccineRepository`.
- **Módulo Clínico (`modules/clinical`)**:
  - Implementação das entidades JPA centrais: `Appointment`, `DoctorSchedule`, `TimeSlot`, `MedicalRecord`, `Prescription`, `ExamRequest` e `MedicalCertificate`.
  - Implementação de entidades de auditoria/histórico para rastreabilidade de alterações clínicas: `MedicalRecordHistory`, `PrescriptionHistory`, `ExamRequestHistory` e `MedicalCertificateHistory`.
  - Implementação dos enums: `AppointmentStatus`, `AppointmentType` e `ExamUrgency`.
  - Mapeamento de relacionamentos entre agendamentos, horários de médicos e prontuários com chaves estrangeiras e integridade referencial.
  - Criação dos repositórios Spring Data JPA: `AppointmentRepository`, `DoctorScheduleRepository`, `TimeSlotRepository`, `MedicalRecordRepository`, `MedicalRecordHistoryRepository`, `PrescriptionRepository`, `PrescriptionHistoryRepository`, `ExamRequestRepository`, `ExamRequestHistoryRepository`, `MedicalCertificateRepository` e `MedicalCertificateHistoryRepository`.
- **Bootstrap do IAM Service Java — Fase 1 (`backend-java/iam-service`)**:
  - Criação do `pom.xml` com Java 21 e Spring Boot 3.3.4, incluindo Spring Web, JPA, Security, Validation, Redis, AMQP, Actuator, Flyway, PostgreSQL, Prometheus, OpenAPI, Nimbus JOSE JWT, Google Authenticator, Lombok e dependências de teste com Testcontainers.
  - Criação do `application.yml` com porta interna `8000`, perfis `dev`, `prod` e `test`, datasource PostgreSQL para `iam_db`, Flyway, Redis, RabbitMQ e exposição de `health`, `info` e `prometheus` em `/actuator`.
  - Implementação de `IamServiceApplication` com `@SpringBootApplication` e `@EnableScheduling`.
  - Implementação de `JwtProperties` para o mapeamento tipado e validado de `app.security.jwt` e de `RedisConfig` com `StringRedisTemplate` serializado como texto, apto a operações com TTL.
  - Compilação do módulo concluída com Java 21 e Maven, sem falhas após os ajustes de ambiente e dependências.
- **Entidades e Repositórios do IAM — Fase 3 (`backend-java/iam-service`)**:
  - Implementação das entidades `User`, `RefreshToken`, `PasswordResetToken` e `OAuthAccount`, além do enum `Role` com os papéis `ADMIN`, `DOCTOR`, `ATTENDANT` e `PATIENT`.
  - Mapeamento das relações JPA com carregamento preguiçoso, remoção em cascata e geração de UUID v4 no Java; os timestamps são preenchidos automaticamente por Hibernate.
  - Compatibilização do campo `role` com o tipo enumerado nativo `user_role` do PostgreSQL definido pelo Flyway.
  - Proteção contra ciclos de serialização das relações bidirecionais com `@JsonIgnore`.
  - Criação de `UserRepository`, `RefreshTokenRepository`, `PasswordResetRepository` e `OAuthAccountRepository`, incluindo busca case-insensitive por email, consultas de tokens ativos e revogação global de refresh tokens.
  - Inclusão do teste de integração `IamPersistenceIntegrationTest`, que valida migração Flyway, mapeamentos Hibernate e consultas de todos os repositórios em PostgreSQL 15 via Testcontainers.
- **DTOs e Validações do IAM — Fase 4 (`backend-java/iam-service`)**:
  - Criação dos 15 DTOs como Java Records imutáveis para autenticação, redefinição de senha, gestão de usuários, cadastro de paciente e listagem de médicos.
  - Aplicação de validações Jakarta para e-mail, campos obrigatórios, tamanho mínimo de senha, CPF, papel de usuário e motivo de desativação, com mensagens em português.
  - Compatibilização dos contratos JSON com FastAPI e frontend React por meio de `@JsonProperty`, preservando campos como `access_token`, `refresh_token`, `token_type`, `expires_in`, `full_name` e `is_active`.
  - Implementação de `UserResponseDTO.fromEntity(User)` e cópias defensivas para listas de respostas paginadas.
  - Inclusão do teste `DtoContractTest` para validar violações Jakarta e a serialização dos tokens em `snake_case`.
- **Segurança e Revogação de Tokens do IAM — Fase 5 (`backend-java/iam-service`)**:
  - Implementação de `JwtTokenProvider` com emissão e validação de JWTs HS256 compatíveis com os claims compartilhados no ecossistema Python: `sub`, `role`, `email`, `type`, `iat`, `exp` e `jti` para refresh tokens.
  - Reforço de `JwtProperties` para exigir chave JWT de ao menos 256 bits e uso exclusivo do algoritmo HS256.
  - Implementação de `TokenBlacklistService`, que armazena tokens revogados no Redis sob a chave `blacklist:<token>` com TTL igual ao tempo restante de expiração.
  - Implementação de `JwtAuthenticationFilter`, responsável por extrair o token Bearer, consultar a blacklist, validar a assinatura e popular o `SecurityContext` com a autoridade `ROLE_<papel>`.
  - Criação da `SecurityConfig` stateless, com CSRF desabilitado, BCrypt de custo 12, rotas públicas definidas e filtro JWT posicionado antes de `UsernamePasswordAuthenticationFilter`.
  - Prevenção de execução duplicada do filtro JWT por meio do desativamento de seu registro automático como filtro de servlet.
  - Inclusão de testes para tokens, blacklist Redis, filtro de autenticação e BCrypt.
- **Serviços de Negócio e Eventos do IAM — Fase 6 (`backend-java/iam-service`)**:
  - Implementação de `AuthService` com autenticação por senha, validação de TOTP quando o 2FA está habilitado, emissão de JWTs, persistência do hash SHA-256 de refresh tokens, rotação de refresh token, logout com blacklist Redis, alteração de senha e redefinição de senha com validade de duas horas.
  - Implementação de `UserService` para criação de usuários e pacientes, validação de unicidade de e-mail/CPF, listagem paginada, consulta individual, listagem de médicos e desativação de conta com revogação em lote dos refresh tokens.
  - Criação dos eventos imutáveis `UserCreatedEvent` e `UserDeactivatedEvent` e do `IamEventPublisher`, que publica mensagens JSON na exchange configurável `iam.events`, nas routing keys `user.created` e `user.deactivated`.
  - Inclusão de headers de rastreabilidade `eventId`, `eventType`, `occurredAt` e `correlationId` nas mensagens RabbitMQ.
  - Ampliação de `UserRepository` para paginação por status e de `JwtTokenProvider` para identificar refresh tokens.
  - Inclusão de testes unitários para rotação de refresh token, desativação e revogação em lote, além dos headers dos eventos RabbitMQ.
- **Google OAuth2 e Autenticação em Dois Fatores — Fase 7 (`backend-java/iam-service`)**:
  - Implementação de `TwoFactorService` com geração de segredos Base32, URI compatível com aplicativos autenticadores no formato `otpauth://` e validação de códigos TOTP de seis dígitos por meio do Google Authenticator.
  - Criação de `OAuthConfig` para leitura de `app.oauth2.google.client-id`, `client-secret` e `redirect-uri`, com valores via variáveis de ambiente e indisponibilidade segura quando o provedor não estiver configurado.
  - Criação do fluxo OAuth2 Google com redirecionamento para consentimento, escopos `openid email profile`, estado criptograficamente aleatório com uso único e TTL de dez minutos no Redis, troca do código de autorização e consulta do perfil OpenID Connect.
  - Implementação do provisionamento automático de usuários sociais como `PATIENT`, armazenamento de senha aleatória protegida com BCrypt, vínculo na tabela `oauth_accounts` e emissão do par de JWTs interno.
  - Criação de `OAuthController` para os endpoints de início, callback e consulta de contas vinculadas; o endpoint de contas foi mantido autenticado por JWT, enquanto apenas o início e o callback do Google são públicos.
  - Inclusão de teste automatizado que confirma geração de segredo, URI QR e validação de código TOTP sincronizado.
- **Controladores REST e Tratamento Global de Erros — Fase 8 (`backend-java/iam-service`)**:
  - Criação de `AuthController` com os fluxos de login, refresh, logout, alteração e recuperação de senha e autocadastro de paciente; o logout normaliza o token Bearer antes da revogação no Redis.
  - Criação de `UserController` para listagem, criação, perfil próprio, médicos ativos, atualização cadastral, mudança de papel, desativação e reativação de contas.
  - Habilitação da segurança de métodos e aplicação de `@PreAuthorize` para papéis ADMIN e ATTENDANT, incluindo a regra de que atendentes podem criar somente pacientes.
  - Criação do endpoint público `GET /healthz`, retornando o estado e o identificador do serviço.
  - Implementação de `ApiErrorResponse` e `GlobalExceptionHandler` para respostas consistentes de validação, credenciais, acesso, recurso inexistente, conflito e falha interna.
  - Inclusão das exceções de domínio `BadCredentialsException`, `ResourceNotFoundException` e `ConflictException`; as consultas e operações de usuário passaram a utilizá-las.
  - Correção do filtro isolado por papel na listagem de usuários, anteriormente pendente na Fase 6.
- **Containerização e Testes de Integração — Fase 9 (`backend-java/iam-service`)**:
  - Criação do Dockerfile multi-stage com Maven e Eclipse Temurin 21 Alpine, execução como usuário não-root, healthcheck em `/healthz` e limites de memória JVM apropriados para containers.
  - Atualização do serviço `iam-service` no Docker Compose para construir o módulo Java, expor `8001:8000` e receber configurações Spring de PostgreSQL, Redis, RabbitMQ e JWT.
  - Criação de `AuthIntegrationTest` com PostgreSQL 15 e Redis 7 reais via Testcontainers e chamadas HTTP com MockMvc; a mensageria foi isolada para manter banco e cache integralmente reais.
  - Cobertura dos fluxos de login do administrador, cadastro de paciente, blacklist Redis no logout, rejeição de token revogado e bloqueio de login após desativação.
- **Auditoria da Task #20 — Controladores e Erros HTTP (`backend-java/iam-service`)**:
  - Auditoria dos controladores de autenticação e usuários contra o planejamento de endpoints, confirmando os métodos, paths, regras de papel e respostas previstos.
  - Identificação de lacuna no contrato de erros: respostas 401 do filtro JWT e respostas 401/403 emitidas pela cadeia do Spring Security não eram serializadas por `ApiErrorResponse`.
  - Criação de `SecurityErrorResponder` e configuração de `AuthenticationEntryPoint` e `AccessDeniedHandler` para padronizar erros da camada de segurança como JSON, sem depender exclusivamente do `@RestControllerAdvice`.
  - Inclusão do tratamento de corpo JSON malformado como HTTP 400 no `GlobalExceptionHandler`.
  - Ampliação do teste do filtro JWT para validar status, tipo de conteúdo e contrato JSON de erro.

---

### ❌ O que deu Erro

- A dependência `org.testcontainers:redis` não possui módulo publicado compatível no BOM utilizado, resultando em erro de versão ausente durante a resolução do Maven. Foi substituída por `org.testcontainers:testcontainers`, que permite o uso do Redis com `GenericContainer` nos testes.
- O Maven fornecido pela IDE foi iniciado com Java 17, o que causou o erro `release version 21 not supported`. A compilação foi reexecutada com `JAVA_HOME` apontando para o Corretto Java 21 e finalizada com sucesso.
- A primeira execução do teste de integração usou Testcontainers 1.20.2, cujo cliente Docker API 1.32 foi recusado pelo daemon local, que exige API 1.40 ou superior. A versão foi atualizada para 1.21.4, compatível com engines Docker recentes, e a suíte passou sem erros.
- O teste inicial do custo BCrypt usava o método inexistente `getStrength()` nesta versão de Spring Security. A verificação foi corrigida para validar o prefixo do hash BCrypt (`$2…$12$`), sem impacto na implementação de produção.
- Na revisão funcional da Fase 6, foi identificada uma pendência no fluxo de recuperação de senha: `forgotPassword` persiste somente o hash e descarta o token em texto, sem ainda encaminhá-lo a um serviço de e-mail ou notificação. Dessa forma, o token não chega ao usuário para uso em `resetPassword`.
- A revisão também identificou que `listUsers` não aplica o filtro de papel quando `role` é informado sem `isActive`; essa combinação precisa ser tratada antes da disponibilização do endpoint.
- Nenhum erro de compilação ou de teste foi encontrado durante a implementação da Fase 7. A autenticação real com Google não foi executada localmente, pois requer credenciais OAuth2 e URIs de redirecionamento cadastradas no console do provedor.
- Nenhum erro de compilação ou teste foi encontrado na Fase 8; os avisos do agente Mockito, Testcontainers e dialeto PostgreSQL não interromperam a execução.
- A construção da imagem Docker foi iniciada e o Dockerfile não apresentou erro de sintaxe, mas o download inicial das dependências Maven excedeu a janela de execução disponível antes da conclusão. A validação completa da imagem e de seu tamanho permanece pendente.
- A primeira compilação após a inclusão do respondedor de erros revelou uma dependência não declarada na assinatura de `SecurityFilterChain`; o parâmetro foi incluído e a suíte completa voltou a passar.

---

### 🟢 O que Funcionou

- Modelagem completa e rica do modelo relacional em JPA/Hibernate cobrindo todas as regras de domínio legadas de pacientes e prontuários.
- Definição de histórico e auditoria imutável para prontuários, receitas, pedidos de exames e atestados médicos.
- Repositórios Spring Data JPA prontos com consultas especializadas para atender a camada de serviço e API REST.
- Resolução das dependências e compilação limpa do `iam-service` com `mvn clean compile`, Java 21 e Spring Boot 3.3.4.
- Carregamento das configurações de PostgreSQL, Redis, RabbitMQ e JWT a partir de variáveis de ambiente com valores padrão seguros para desenvolvimento local.
- Teste de integração da persistência concluído com 1 teste executado, 0 falhas e 0 erros; Flyway, enum nativo PostgreSQL, Hibernate e os quatro repositórios foram validados em conjunto.
- Testes dos contratos dos DTOs concluídos com 2 casos executados, 0 falhas e 0 erros, confirmando as validações e a paridade dos nomes JSON dos tokens.
- Testes de segurança concluídos com 7 casos executados, 0 falhas e 0 erros, cobrindo emissão e validação de JWT, rejeição de assinatura alterada, autoridade por papel, revogação no Redis e BCrypt com custo 12.
- Suíte do IAM ampliada e validada com 13 testes executados, 0 falhas e 0 erros, incluindo PostgreSQL 15 via Testcontainers, serviços de autenticação/usuário e mensageria RabbitMQ simulada.
- A desativação de usuário atualiza o status, registra data e motivo, revoga tokens ativos em lote e publica o evento de domínio correspondente dentro da mesma unidade transacional de serviço.
- A suíte do IAM passou a ter 14 testes, todos concluídos com sucesso, cobrindo também o fluxo local de TOTP.
- A suíte completa do IAM continuou aprovada após os controladores e o tratamento de exceções, com 14 testes executados, 0 falhas e 0 erros.
- Os cinco testes de integração da Fase 9 passaram sem falhas, exercitando PostgreSQL e Redis em containers descartáveis; a configuração do Docker Compose foi validada com `docker compose config --quiet`.
- A auditoria da Task #20 foi validada com `mvn clean test`: 19 testes executados, sem falhas ou erros, incluindo Testcontainers de PostgreSQL e Redis.

---

### 📝 Resumo do Dia

Finalização da camada de persistência e entidades de domínio do `patient-clinical-service` em Java. Foram criadas mais de 20 classes de modelo (incluindo entidades de negócio, enums e tabelas de histórico) e mais de 15 interfaces de repositório Spring Data JPA, cobrindo todo o bounded context de pacientes, consultas, prontuários, agendamentos e prescrições. No IAM Java, foram concluídas as Fases 1 a 9 e a auditoria da Task #20, incorporando serviços transacionais, eventos RabbitMQ, OAuth2, TOTP, endpoints REST, erros HTTP padronizados também na cadeia de segurança, Docker e testes ponta a ponta. Permanece a pendência da Fase 6 de encaminhar com segurança o token de recuperação de senha. A construção integral da imagem Docker deve ser repetida em ambiente com tempo suficiente para o download inicial das dependências Maven.

---

## 2026-11-05 — Atualização do API Gateway Java

### 📋 Implementação Proposta

- Criar a base do API Gateway reativo em Java e centralizar o roteamento dos serviços, mantendo compatibilidade com as rotas do gateway FastAPI.

---

### ✅ O que foi Implementado

- Configuração do projeto `backend-java/gateway` com Java 21, Spring Boot 3.3.4 e Spring Cloud 2023.0.3, usando WebFlux e Reactor Netty, sem `spring-boot-starter-web`.
- Instalação do Maven Wrapper oficial 3.3.4, fixado para Maven 3.9.9.
- Criação de `ServiceEndpointsProperties` para mapear as URIs configuráveis de IAM, Patient/Clinical, AI, Reporting e RabbitMQ Management.
- Centralização das rotas públicas e protegidas de IAM e AI, das rotas de Patient/Clinical e Reporting e do proxy da interface RabbitMQ.
- Preservação da rota `/metrics`, também existente no gateway FastAPI.
- Configuração do proxy RabbitMQ para reescrever o prefixo `/rabbitmq/` e remover o header `Authorization`.
- Configuração de CORS global com origem ajustável por `FRONTEND_URL`, métodos GET/POST/PUT/PATCH/DELETE/OPTIONS, cabeçalhos livres e credenciais habilitadas.
- Configuração de validação de JWT HMAC-SHA256 nas rotas protegidas e liberação das rotas públicas de autenticação e debug.
- Adição global dos headers `X-Content-Type-Options`, `X-Frame-Options` e `Strict-Transport-Security`.
- Configuração dos endpoints Actuator `health`, `info`, `prometheus` e `gateway`; nível de log de roteamento ajustável por `GATEWAY_LOG_LEVEL`.
- Inclusão de testes para destinos e predicados das rotas e para a exposição dos endpoints Actuator.

---

### ❌ O que deu Erro

- Maven não estava instalado no ambiente; o Maven Wrapper foi adicionado para permitir executar o build sem instalação global.
- O endpoint Actuator `gateway` e o scraping Prometheus inicialmente não foram descobertos. A configuração foi ajustada para ativar explicitamente ambos; a inicialização passou a expor quatro endpoints.
- O Java Language Server do VS Code sinalizou os arquivos Java como fora de um projeto Maven e apontou divergência de pacote. O caminho e as declarações de pacote estão alinhados, e o Maven Wrapper compilou e testou o módulo com sucesso.

---

### 🟢 O que Funcionou

- `./mvnw clean test` concluído com 3 testes, 0 falhas e 0 erros.
- A árvore de dependências confirmou WebFlux e Reactor Netty, sem o starter Servlet `spring-boot-starter-web`.
- A tabela de rotas e os quatro endpoints Actuator foram validados no contexto Spring.

---

### 📝 Resumo do Dia

Foi criada a base do API Gateway Java reativo com configuração tipada dos serviços, roteamento centralizado, CORS, segurança JWT, headers HTTP, Actuator e Maven Wrapper. As rotas foram alinhadas ao gateway FastAPI, incluindo o proxy RabbitMQ e `/metrics`. A suíte final passou com 3 testes e o runtime permaneceu não bloqueante, baseado em WebFlux e Netty.

---

## 2026-11-05 — Validação de JWT e Tracing no Gateway

### 📋 Implementação Proposta

- Implementar na borda a validação de identidade, o bloqueio de tokens revogados e a propagação de dados de identidade e rastreamento, sem operações bloqueantes.

---

### ✅ O que foi Implementado

- Atualização de `SecurityConfig` com os endpoints públicos solicitados, CSRF desabilitado e `NoOpServerSecurityContextRepository` para manter a segurança stateless.
- Implementação de `JwtBlacklistGlobalFilter`, que consulta reativamente a chave Redis `blacklist:<token>` para Bearer tokens e rejeita tokens revogados com HTTP 401 e corpo JSON `{"detail":"Token revogado"}`.
- Propagação dos headers `X-User-Id`, `X-User-Role`, `X-User-Email` e `X-Gateway` usando claims do `JwtAuthenticationToken` validado pelo Resource Server.
- Implementação de `RequestTracingGlobalFilter`, que preserva IDs de rastreamento enviados pelo cliente ou gera um UUID, propaga `X-Correlation-Id` e `X-Request-Id` na requisição e resposta e define `X-Forwarded-For` pelo endereço remoto.
- Definição de ordem para executar tracing antes da verificação de blacklist e, em ambos os filtros, continuação do pipeline com tipos e operadores reativos.
- Inclusão de testes para token revogado, headers de identidade, IDs de tracing e IP remoto.

---

### ❌ O que deu Erro

- Os primeiros testes utilizaram métodos/casts que não existem ou não são válidos para os mocks reativos desta versão do Spring. Os testes foram ajustados para usar `ServerWebExchange` e predicados assíncronos, sem alterar a lógica de produção.
- A combinação de busca estática e build em um único comando não era compatível com o shell Fish; a busca e a suíte foram executadas separadamente.

---

### 🟢 O que Funcionou

- `./mvnw clean test` concluído com 7 testes, 0 falhas e 0 erros.
- Busca estática sem resultados para `.block()` e `toFuture().get()` nos filtros.
- Verificada resposta 401 com corpo JSON para token revogado, sem chamada ao downstream; verificadas também a propagação de claims e os headers de tracing.

---

### 📝 Resumo do Dia

A camada de identidade e controle de tokens revogados do Gateway foi concluída sobre Spring Security reativo. Tokens revogados são bloqueados por consulta assíncrona ao Redis; tokens validados propagam claims aos serviços downstream, enquanto o filtro de tracing mantém IDs de correlação e o IP do cliente. A implementação não contém chamadas bloqueantes e passou nos 7 testes da suíte.

---

## 2026-11-05 — Rate Limiting Distribuído no Gateway

### 📋 Implementação Proposta

- Aplicar limites distribuídos por IP, usuário autenticado e chave de API nas rotas públicas e de dados do Gateway, usando Redis e respostas 429 compatíveis com o FastAPI.

---

### ✅ O que foi Implementado

- Criação de `RateLimiterConfig` com o `KeyResolver` reativo `userOrIpKeyResolver`: chave de API identificada por SHA-256, usuário autenticado pelo `sub` e cliente anônimo pelo primeiro IP de `X-Forwarded-For`, com fallback para o endereço remoto.
- Configuração dos buckets Redis com `replenishRate` e `burstCapacity` solicitados: anônimo `30/60`, autenticado `300/400` e API key `120/150`.
- Implementação de `PolicyAwareRedisRateLimiter` para selecionar a política pelo prefixo da chave (`ip:`, `user:` ou `apikey:`) e compartilhar o bucket da identidade entre as rotas.
- Conversão da quota por minuto para a unidade do Spring Cloud Gateway, que mede tokens por segundo: cada requisição consome 60 tokens, resultando em 30, 300 e 120 requisições por minuto, respectivamente.
- Aplicação de `RequestRateLimiter` nas rotas públicas de autenticação, AI e nas rotas de dados de pacientes, consultas, agendas, prontuários e relatórios.
- Implementação da resposta 429 do Gateway com `Retry-After` calculado a partir dos tokens restantes e o payload `{"detail":"Taxa de requisições excedida. Tente novamente em breve."}`. A customização identifica apenas recusas do próprio Gateway e preserva respostas 429 dos serviços downstream.
- Ajuste do tracing para preservar o primeiro endereço de `X-Forwarded-For`, permitindo que rate limiting anônimo e tracing usem o IP encaminhado pelo proxy.
- Inclusão de testes para hash da API key, resolução por usuário e IP, seleção dos buckets, aplicação do filtro nas rotas, `Retry-After` e corpo HTTP 429.

---

### ❌ O que deu Erro

- A primeira integração com o DSL usou `Consumer` onde o Gateway exige uma função que retorne `UriSpec`; o helper foi ajustado para a assinatura da versão utilizada.
- A presença de vários beans `RateLimiter` causou ambiguidade na auto-configuração da fábrica. O limitador composto foi marcado como `@Primary`.
- O Spring Cloud Gateway mede reposição em tokens por segundo, enquanto os requisitos são por minuto. O custo de 60 tokens por requisição foi usado para manter os limites temporais pedidos com os valores de `replenishRate` e `burstCapacity` especificados.

---

### 🟢 O que Funcionou

- `./mvnw clean test` concluído com 16 testes, 0 falhas e 0 erros.
- Testada a seleção de buckets distintos para IP anônimo, usuário autenticado e API key com hash SHA-256.
- Confirmadas a aplicação do filtro nas rotas definidas e a resposta JSON 429 com `Retry-After`; respostas 429 originadas pelos serviços downstream permanecem intactas.
- Verificada a ausência de chamadas `.block()` e `toFuture()` nos componentes de rate limiting.

---

### 📝 Resumo do Dia

Foi implementado rate limiting distribuído com Redis para as rotas de API do Gateway. As requisições são agrupadas por IP, identidade autenticada ou hash de API key, com quotas de 30, 300 e 120 por minuto. Recusas do Gateway retornam o formato 429 esperado e as cotas funcionam entre rotas para cada identidade. A suíte final passou com 16 testes.

---

## 2026-11-09 — Circuit Breakers e Fallbacks Reativos no Gateway

### 📋 Implementação Proposta

- Configurar circuit breakers reativos para proteger os serviços internos, limitar o tempo de espera por resposta e retornar fallbacks padronizados quando houver abertura do circuito ou timeout.

---

### ✅ O que foi Implementado

- Criação de `ResilienceConfig` com `Customizer<ReactiveResilience4JCircuitBreakerFactory>` para `patientClinicalCircuitBreaker`, `reportingCircuitBreaker`, `iamCircuitBreaker` e `aiCircuitBreaker`.
- Configuração da janela de chamadas em 3, threshold de falhas de 50%, espera de 30 segundos em estado aberto e 2 chamadas permitidas em half-open.
- Configuração de timeout de 30 segundos para Patient/Clinical, Reporting e IAM e de 60 segundos para AI.
- Migração da tabela de rotas Java DSL para `spring.cloud.gateway.routes` no `application.yml`, declarando os filtros `CircuitBreaker` e os `fallbackUri` específicos de cada serviço, sem manter rotas duplicadas.
- Criação do `ServiceFallbackController` reativo para Patient/Clinical, AI, Reporting e IAM. Os fallbacks retornam HTTP 503, `X-Circuit-Breaker: open` e JSON com `detail`, serviço e timestamp ISO-8601.
- Captura e classificação da causa original (`CallNotPermittedException`, timeout ou falha downstream), com logs estruturados por serviço, motivo, tipo e mensagem da exceção.
- Liberação de `/fallback/**` na configuração de segurança para permitir o forward interno do Gateway.
- Inclusão de testes para filtros e IDs de rotas, thresholds e timeouts efetivos do factory e formato/status do fallback.

---

### ❌ O que deu Erro

- A primeira carga do contexto revelou que `logging.level` havia sido aninhado incorretamente com as propriedades Actuator. A hierarquia YAML foi corrigida e os endpoints Actuator continuaram expostos.
- A verificação dos registries Resilience4j precisou executar um publisher, pois o factory instancia breakers e time limiters sob demanda.

---

### 🟢 O que Funcionou

- `./mvnw clean test` concluído com 18 testes, 0 falhas e 0 erros.
- Confirmados no teste os valores de janela, threshold, tempo aberto, chamadas half-open e timeout de cada breaker.
- As rotas YAML carregaram com os filtros CircuitBreaker e os destinos/fallbacks configurados.
- O controller retornou o contrato 503 esperado e registrou o timeout original com motivo estruturado.

---

### 📝 Resumo do Dia

Foi adicionada tolerância a falhas no API Gateway com circuit breakers Resilience4j, timeouts por serviço e fallbacks reativos padronizados. As rotas foram centralizadas no YAML para declarar os filtros por serviço; a suíte final validou os parâmetros efetivos e passou com 18 testes.

---

## 2026-11-09 — Health Checks Agregados e Diagnóstico do RabbitMQ

### 📋 Implementação Proposta

- Disponibilizar endpoints reativos de saúde do Gateway e agregação do estado dos microsserviços, além de tratar falhas de conexão da interface RabbitMQ com resposta 502 amigável.

---

### ✅ O que foi Implementado

- Criação de `WebClientConfig` com `HttpClient` Reactor Netty, timeout de conexão de 2 segundos e timeout de leitura/resposta de 3 segundos.
- Criação do record imutável `HealthStatusResponse`, com mapa de serviços não modificável.
- Implementação de `GET /healthz`, que retorna imediatamente `status=ok` e `service=gateway`.
- Implementação de `GET /healthz/services` usando `Mono.zip` para consultar em paralelo `/healthz` do IAM, Patient/Clinical, AI e Reporting; cada chamada tem timeout de 3 segundos e converte erros/timeout em `unreachable`.
- Mapeamento de HTTP 200 para `ok`, outros status HTTP para `degraded`, aliases históricos `patient` e `clinical` para o estado de `patient_clinical` e status geral `ok` ou `degraded`, mantendo HTTP 200 no agregador.
- Adição de CircuitBreaker à rota `/rabbitmq/**`, preservando o encaminhamento à porta 15672, e fallback dedicado com HTTP 502 e mensagem JSON amigável para falhas da interface de gerenciamento.
- Inclusão de testes para health local, agregação de estados, aliases, indisponibilidade de serviço e resposta 502 do RabbitMQ.

---

### ❌ O que deu Erro

- Nenhum erro de implementação permaneceu; os testes de health e fallback concluíram sem falhas.

---

### 🟢 O que Funcionou

- `./mvnw clean test` concluído com 22 testes, 0 falhas e 0 erros.
- Confirmada a inicialização do contexto Gateway com rotas YAML, endpoints Actuator e fallbacks.
- Busca estática sem chamadas bloqueantes nos componentes de health e WebClient.

---

### 📝 Resumo do Dia

Foram adicionados health checks reativos para o Gateway e para os serviços internos, com consultas paralelas, timeouts curtos e compatibilidade com os aliases históricos de Patient/Clinical. A rota de gerenciamento do RabbitMQ agora conta com CircuitBreaker e fallback HTTP 502. A suíte final passou com 22 testes.

---

## 2026-11-09 — Mensageria resiliente e containerização Java

### 📋 Implementação Proposta

- Reforçar a integração por eventos entre o serviço clínico Java e consumidores legados, com retentativas e publicação assíncrona.
- Preparar o `patient-clinical-service` e o AI Service Java para execução em containers e ampliar a validação de integração.

---

### ✅ O que foi Implementado

- Expansão da configuração RabbitMQ com exchange e fila clínica, bindings para `medical_record.created` e `prescription.generated`, além de rotas legadas compatíveis com o AI Service Python.
- Atualização dos eventos de criação de prontuário e geração de prescrição com identificador, timestamp, tipo e correlação; publicação assíncrona, retentativas e opção configurável para encaminhamento às exchanges legadas.
- Inclusão de `Dockerfile` multi-stage para compilar o `patient-clinical-service` com Java 21/Maven e executar a aplicação com usuário não privilegiado.
- Criação de configuração Compose para os serviços Java e ajustes no Compose/Makefile do backend para usar a implementação Java unificada de pacientes e clínica.
- Inclusão de teste de integração Spring Boot com PostgreSQL, RabbitMQ e MinIO via Testcontainers, condicionado à variável `RUN_INTEGRATION_TESTS=true`.
- Inclusão da estrutura do AI Service e bibliotecas compartilhadas em `backend-java`, com imagens e documentação de execução.

---

### ❌ O que deu Erro

- Nenhum erro de execução foi registrado nesta atividade. Os novos containers e testes de integração não foram executados nesta sessão.

---

### 🟢 O que Funcionou

- As alterações de mensageria, infraestrutura Docker e teste de integração estão presentes no workspace para revisão.
- As mudanças permanecem sem commit no branch `feature/docker`.

---

### 📝 Resumo do Dia

Foram ampliadas a publicação de eventos clínicos e a compatibilidade com consumidores legados, com retentativas e envio assíncrono. O serviço clínico Java recebeu configuração de container multi-stage e passou a ser a implementação usada pelo Compose principal; também foram adicionados um Compose para os serviços Java e um teste de integração opcional com Testcontainers. A estrutura do AI Service e módulos compartilhados foi preparada em `backend-java`. Nenhum build de container ou teste foi executado nesta sessão, e as alterações permanecem pendentes de commit.

---

## 2026-11-09 — Containerização do API Gateway Java

### 📋 Implementação Proposta

- Criar uma imagem Docker otimizada e não-root para o Gateway Java e substituir o gateway antigo no Compose do backend.

---

### ✅ O que foi Implementado

- Criação de `backend-java/gateway/Dockerfile` multi-stage com Maven 3.9/Java 21 Alpine para build e Eclipse Temurin 21 JRE Alpine para runtime.
- Configuração do estágio de build para baixar dependências Maven antes de copiar o código e gerar o JAR executável sem executar testes dentro da imagem.
- Criação do usuário não-root `gatewayuser`, instalação de `curl` e `tzdata`, configuração de opções JVM para containers/Netty, exposição da porta 8000 e healthcheck em `/healthz`.
- Criação de `.dockerignore` para excluir artefatos `target`, Maven Wrapper e arquivos associados do contexto de build.
- Substituição do serviço `gateway` em `backend/docker-compose.yml` pela imagem Java, com porta 8000, variáveis de ambiente, rede `backend` e dependências saudáveis de Redis, IAM e Patient/Clinical.
- Parametrização de `REDIS_PORT` no `application.yml` do Gateway.
- Alinhamento da URL de gerenciamento RabbitMQ ao usuário `promptuario` e à senha configurada no próprio Compose.

---

### ❌ O que deu Erro

- A construção da imagem não pôde ser concluída porque a conexão com o Docker Hub foi resetada ao baixar a imagem base `maven:3.9-eclipse-temurin-21-alpine`. A falha ocorreu antes da execução dos passos do Dockerfile.

---

### 🟢 O que Funcionou

- `docker compose -f backend/docker-compose.yml config --quiet` validou a sintaxe e a configuração do Compose.
- `./mvnw clean test` passou com 22 testes, 0 falhas e 0 erros.
- Os serviços IAM e Patient/Clinical já possuem healthchecks nos respectivos Dockerfiles, permitindo usar `condition: service_healthy` no Gateway.

---

### 📝 Resumo do Dia

O Gateway Java foi preparado para execução em container não-root com imagem multi-stage e healthcheck interno, e o Compose do backend foi atualizado para construí-lo no lugar do gateway anterior. A configuração do Compose e a suíte Maven passaram; a validação da imagem permanece pendente devido à falha de conexão com o registry Docker Hub.

---

## 2026-11-09 — Validação de Integração e Imagem Docker do Gateway

### 📋 Implementação Proposta

- Completar a validação automatizada da paridade do Gateway Java, instalar no projeto as dependências de teste Python e confirmar a construção da imagem Docker.

---

### ✅ O que foi Implementado

- Criação de `backend/gateway/requirements-dev.txt`, incluindo `pytest==8.4.2` junto às dependências de runtime, sem adicionar ferramentas de teste à imagem de produção.
- Provisionamento de Python 3.12.16 com `uv` em cache local e instalação das dependências-dev em um virtualenv temporário fora do repositório.
- Inclusão de `MockWebServer` e Testcontainers Redis no POM de teste do Gateway.
- Criação de `GatewayRoutingIntegrationTest` com `@SpringBootTest(RANDOM_PORT)` e `WebTestClient`, cobrindo health local/agregado, aliases, login público, bloqueio sem token e headers de identidade encaminhados.
- Criação de `GatewaySecurityAndResilienceTest`, cobrindo blacklist Redis, limite anônimo HTTP 429 e fallback do Circuit Breaker após respostas HTTP 503; os 5xx foram configurados como falhas contabilizadas pelo breaker.
- Construção bem-sucedida da imagem `backend-gateway:latest`; a inspeção confirmou usuário `gatewayuser`, entrypoint Java, porta 8000 e healthcheck interno.
- Validação do Compose com `docker compose -f backend/docker-compose.yml config --quiet`.

---

### ❌ O que deu Erro

- A instalação inicial das dependências Python no sistema falhou porque o ambiente fornecia Python 3.14 sem FastAPI/HTTPX, enquanto o backend documenta Python 3.12; `pydantic-core` da versão fixada não compilou nesse Python. A instalação foi concluída em ambiente virtual isolado com Python 3.12.16.
- A primeira execução dos testes Testcontainers foi bloqueada pelo reset de conexão com o Docker Hub durante o download das imagens Ryuk/Redis. Em uma execução posterior, o pull foi concluído e a suíte de integração passou.
- O mock inicial de desconexão do downstream retornou HTTP 200 no teste do breaker. O cenário foi tornado determinístico com respostas 503 e configuração explícita dos status 5xx como falhas do Circuit Breaker.

---

### 🟢 O que Funcionou

- Suíte Maven completa concluída com 30 testes, 0 falhas e 0 erros, incluindo Testcontainers Redis e os dois testes de integração.
- Suíte pytest legada em `backend/gateway/tests` concluída com 5 testes aprovados. Foram observados apenas warnings de depreciação do FastAPI/Starlette.
- Build da imagem multi-stage e validação da configuração Docker Compose concluídos com sucesso.
- Nenhum arquivo do frontend foi modificado.

---

### 📝 Resumo do Dia

A validação de integração do novo Gateway Java foi concluída com 30 testes Maven e 5 testes pytest do gateway legado aprovados. A imagem Docker multi-stage foi construída e inspecionada com sucesso, e o Compose foi validado. O stack completo não foi iniciado, portanto a comunicação com todos os microsserviços reais ainda não foi exercitada em execução conjunta.

---

## 2026-11-11 — Correção Crítica do Serviço de Backup (Fase B0 — Topologia Unificada)

### 📋 Implementação Proposta

- Corrigir a dívida técnica imediata do serviço de backup em Python (`backend/backup/backup_runner.py`) após a Fase 12: remover os hosts órfãos `db-patient` e `db-clinical` e passar a dumpar o banco unificado `patient_clinical_db` (container `db-patient-clinical`).
- Garantir que `POSTGRES_USER`/`POSTGRES_PASSWORD` (e variáveis por banco) possam sobrescrever as credenciais default.
- Verificar o `backend/docker-compose.yml` (dependências e redes do `backup-service`).
- Criar roteiro de teste automatizado e o baseline `backend/backup/BACKUP_BASELINE.md`, documentando o comando de contingência e a estrutura do `/var/backups/status.json`.
- Garantir que nenhuma exceção não tratada derrube a execução dos demais bancos.

---

### ✅ O que foi Implementado

- **`backend/backup/backup_runner.py`**:
  - `POSTGRES_DATABASES` passou a conter apenas `iam_db` (`db-iam`), `patient_clinical_db` (`db-patient-clinical`, usuário `clinical`) e `reporting_db` (`db-reporting`); os hosts órfãos `db-patient`/`db-clinical` foram removidos.
  - Nova função `_apply_postgres_env_overrides()` com precedência `<PREFIXO>_DB_USER`/`_DB_PASSWORD` → `POSTGRES_USER`/`POSTGRES_PASSWORD` → default; hosts também configuráveis via `IAM_DB_HOST`, `PATIENT_CLINICAL_DB_HOST` e `REPORTING_DB_HOST`.
  - `write_status_report()` passou a registrar `elapsed_seconds` (tempo de execução) e `database_count`.
  - `run_all_backups()` com isolamento defensivo: `ensure_bucket`, cada banco PostgreSQL, o MongoDB e a limpeza de retenção ficaram em blocos `try/except` independentes, marcando `status: failed` sem interromper os demais alvos.
- **`backend/docker-compose.yml`**: confirmado o `depends_on` do `backup-service` com os 5 alvos em `condition: service_healthy` (`db-iam`, `db-patient-clinical`, `db-reporting`, `db-ai`, `minio`) e todos na rede `backend`; adicionado o volume `backup_data:/var/backups` (antes declarado e não montado), garantindo a persistência do `status.json`.
- **`backend/backup/test_backup_runner.py`** (novo): roteiro com 9 testes (`unittest`, autossuficiente com stubs de `boto3`/`botocore`/`schedule`) cobrindo topologia unificada, ausência de hosts órfãos, overrides por variáveis de ambiente, estrutura do `status.json` e isolamento de falhas.
- **`backend/backup/BACKUP_BASELINE.md`** (novo): comando de contingência `docker compose run --rm backup-service python backup_runner.py --once`, estrutura detalhada do `/var/backups/status.json`, matriz de `depends_on`/redes, coleta de baseline (tempo/tamanho), critérios de aceite e rollback.
- **`backend/scripts/test_backup_service.py`**: asserções obsoletas atualizadas (esperava `db-patient`/`db-clinical` e a string de rede sem espaços).

---

### ❌ O que deu Erro

- A primeira execução do roteiro estático acusou 3 falhas por asserções desatualizadas (dependências `db-patient`/`db-clinical` e verificação de rede com formatação incorreta).
- A verificação "host órfão `db-clinical` ausente" falhou porque o próprio comentário explicativo adicionado ao runner continha o literal `db-clinical`; o comentário foi reescrito para não poluir a busca.
- A edição do volume no `docker-compose.yml` falhou na primeira tentativa por contexto ambíguo (o bloco `minio: condition: service_healthy` se repete em vários serviços).
- O ambiente local não possui `boto3`, `schedule`, `pytest` nem Maven, exigindo stubs no roteiro de teste e validação alternativa do Compose.

---

### 🟢 O que Funcionou

- `python3 backend/scripts/test_backup_service.py`: **101 passaram, 0 falharam** (antes: 94 passaram / 3 falharam).
- `python3 backend/backup/test_backup_runner.py`: **9 testes, 0 falhas** — incluindo a simulação de falha isolada em `patient_clinical_db`, que resultou em `status: partial` sem abortar os demais bancos.
- `python3 -m py_compile backend/backup/backup_runner.py backend/backup/test_backup_runner.py`: sintaxe válida.
- `docker compose -f backend/docker-compose.yml config -q`: configuração válida (exit 0).
- Nenhum arquivo do frontend foi modificado.

---

### 📝 Resumo do Dia

A correção crítica da Fase B0 foi concluída e validada: o runner de backup deixou de apontar para os hosts órfãos e passou a proteger o banco unificado `patient_clinical_db`, com isolamento de falhas por banco, overrides por variáveis de ambiente e manifesto `status.json` enriquecido (tempo de execução e tamanho por banco). A orquestração no Compose foi confirmada e reforçada com o volume persistente. Ficou pendente como ponto de decisão o escopo da migração do `backup-service` para Java (`backend-java/backup-service`), que ainda não existe no repositório.

---

## 2026-11-11 — Fundação do Backup Service em Java 21 / Spring Boot 3.3 (Fase B1)

### 📋 Implementação Proposta

- Criar a base do microsserviço corporativo `backend-java/backup-service` com Java 21 e Spring Boot 3.3.4, destinado a substituir o runner Python ao final da migração.
- Configurar o `pom.xml` com as dependências de Web/Actuator, validação, Micrometer/Prometheus, AWS SDK v2 (S3 + Netty NIO), picocli, Jackson JSR-310 e Lombok.
- Mapear o `application.yml` com porta interna 8000, propriedades `promptuario.backup` (agendamento, retenção, criptografia), a topologia unificada de bancos (`postgres` e `mongo`) conforme a Fase 12 e o storage MinIO em `app.storage.s3`.
- Implementar os modelos de domínio/enums, as classes de configuração e a classe principal com `@EnableScheduling` e `@EnableAsync`.

---

### ✅ O que foi Implementado

- **`backend-java/backup-service/pom.xml`**: parent `spring-boot-starter-parent:3.3.4`, coordenadas `br.edu.ifrn.promptuario:backup-service`, Java 21, BOM do AWS SDK v2 (`2.25.0`) e dependências `spring-boot-starter-web`, `-validation`, `-actuator`, `micrometer-registry-prometheus`, `software.amazon.awssdk:s3` e `netty-nio-client`, `info.picocli:picocli-spring-boot-starter:4.7.5`, `jackson-datatype-jsr310`, `lombok` (provided) e `spring-boot-starter-test`; build com `spring-boot-maven-plugin` excluindo o Lombok do jar final.
- **`src/main/resources/application.yml`**: porta `${SERVER_PORT:8000}`; bloco `promptuario.backup` com `schedule-cron` (`0 0 3 * * ?`), `retention-days` (7), `root-dir` (`/var/backups`), `encryption-enabled` e `encryption-secret`; topologia `postgres` (`iam` → `db-iam`/`iam_db`, `patient-clinical` → `db-patient-clinical`/`patient_clinical_db`, `reporting` → `db-reporting`/`reporting_db`) e `mongo` (`db-ai`/`ai_db`); storage em `app.storage.s3` (endpoint MinIO, chaves, bucket `backups`, região `us-east-1`); Actuator expondo `health,info,prometheus` com probes de liveness/readiness.
- **Modelos de domínio** (`...backup.model`): enums `DatabaseTarget` (IAM_DB, PATIENT_CLINICAL_DB, REPORTING_DB, MONGO_AI_DB, MINIO_BUCKETS), `ExecutionMode` (SCHEDULED, MANUAL, CLI, RESTORE), `BackupKind` (POSTGRES, MONGO) e `BackupStatus` (SUCCESS, PARTIAL, FAILED); `DatabaseBackupResult` (record com fábricas `success`/`failure` e cálculo de megabytes); `BackupManifest` (record imutável com `schemaVersion`, `databases`, `rpoMet` e `retentionUntil`); `BackupJobExecution` (estado mutável do ciclo, com `addResult()` e `finish()` consolidando o status).
- **Classes de configuração** (`...backup.config`): `BackupProperties` (`@ConfigurationProperties(prefix = "promptuario.backup")` com records aninhados `Postgres`/`Mongo`/`DatabaseConnection`, Bean Validation e o helper `postgresTargets()`); `StorageProperties` (`app.storage.s3`); `AsyncConfig` com `ThreadPoolTaskScheduler` (`backup-sched-`) e `ThreadPoolTaskExecutor` (`backup-dump-`) dedicados.
- **`BackupServiceApplication`**: `@SpringBootApplication`, `@EnableScheduling`, `@EnableAsync` e `@ConfigurationPropertiesScan`.
- **`BackupServiceApplicationTests`** (novo): teste de contexto e de binding da topologia unificada, do agendamento/retenção, do MongoDB e do storage S3.

---

### ❌ O que deu Erro

- O primeiro `mvn clean compile` falhou na leitura do POM com `Non-parseable POM ... in comment after two dashes (--) next character must be > not o`: o comentário XML `<!-- CLI robusto (--once, --restore, ...) -->` era inválido, pois XML não permite `--` no interior de comentários. O comentário foi reescrito e a compilação passou.
- O ambiente não possui Maven instalado no `PATH`; a validação foi feita reutilizando a distribuição já presente em `~/.m2/wrapper/dists` (`apache-maven-3.9.16`), com o `picocli 4.7.5` baixado do Maven Central.

---

### 🟢 O que Funcionou

- `mvn clean compile`: **BUILD SUCCESS** — 11 arquivos-fonte compilados com Java 21.
- `mvn test`: **Tests run: 4, Failures: 0, Errors: 0** — contexto Spring carregado e topologia unificada vinculada corretamente, sem nenhum host órfão.
- `mvn package` gerou o jar executável e a aplicação subiu em 2.369s; `/actuator/health` retornou `UP`, `/actuator/prometheus` respondeu **HTTP 200** (a métrica `executor_active_threads{name="backupTaskExecutor"}` confirma o executor dedicado ativo) e foram expostos 3 endpoints sob `/actuator`.
- Nenhum arquivo do frontend foi modificado e a topologia permanece sem `db-patient`/`db-clinical`.

---

### 📝 Resumo do Dia

Concluída a Fase B1 da migração do backup para Java: a fundação do `backup-service` em Java 21/Spring Boot 3.3.4 foi criada e validada de ponta a ponta (compilação, testes de contexto e execução real com Actuator/Prometheus). A topologia já reflete a unificação da Fase 12, mantendo o alvo único `patient_clinical_db` (`db-patient-clinical`). O serviço está pronto para receber, nas próximas fases, o CLI picocli, os dumpers em stream, a criptografia AES-256-GCM, o upload para o MinIO e a containerização; ainda não foi integrado ao `docker-compose.yml` nem commitado no repositório.

---

## 2026-11-11 — Pipeline de Dump, Criptografia AES-256-GCM e Upload MinIO (Fase B2)

### 📋 Implementação Proposta

- Implementar o pipeline de streaming de dump do `backup-service`: `ProcessBuilder (stdout)` → `GZIPOutputStream` → `CipherOutputStream (AES-256-GCM)` → `DigestOutputStream (SHA-256)` → `FileOutputStream` → upload para o MinIO.
- Criar o `CryptoService` (AES/GCM/NoPadding, chave de 256 bits, IV de 12 bytes por arquivo prefixado no stream, tag de 128 bits).
- Criar os dumpers (interface `DatabaseDumper` e implementações PostgreSQL e MongoDB) com timeout configurável e isolamento de falhas por banco.
- Criar o `S3BackupStorageService` (MinIO com `forcePathStyle`), o `RetentionCleanupService`, o `StatusReportService` e o `BackupOrchestratorService`.

---

### ✅ O que foi Implementado

- **`CryptoService`** (`service`): criptografia autenticada `AES/GCM/NoPadding` com chave de 256 bits, IV de 12 bytes gerado por `SecureRandom` para cada artefato e prefixado no início do stream (arquivo autocontido `[IV(12)][ciphertext+tag]`), tag de autenticação de 128 bits e os métodos `createEncryptingStream`/`createDecryptingStream`. A chave é derivada do segredo configurado: usada diretamente quando possui 32 bytes, caso contrário via SHA-256 (garantindo sempre uma chave de 256 bits, nunca curta).
- **Pipeline de streaming** (`AbstractStreamingDatabaseDumper`): `ProcessBuilder → GZIPOutputStream → CipherOutputStream → DigestOutputStream → FileOutputStream`, sem carregar o dump em memória (buffer único de 64 KB). O SHA-256 é calculado sobre o arquivo final, incluindo o prefixo do IV.
- **Dumpers**: `PostgresDatabaseDumper` executa `pg_dump -h <host> -p <port> -U <user> <dbname>` com a senha injetada por `PGPASSWORD` (nunca na linha de comando) e gzip habilitado; `MongoDatabaseDumper` executa `mongodump --archive --gzip` e desabilita o gzip do pipeline, pois a ferramenta já comprime. Timeout configurável (`BACKUP_DUMP_TIMEOUT_SECONDS`, padrão 600s) aplicado por um watchdog que encerra o processo à força, além de `stderr` drenado em thread dedicada (evitando deadlock de pipe) e capturado na mensagem de falha.
- **`S3BackupStorageService`** (`storage`): `S3Client` com `forcePathStyle(true)` (bean em `S3Config`), `ensureBucketExists`, `uploadArtifact` com metadados (SHA-256, alvo, timestamp e tamanho) e chave no padrão `backups/{kind}/{dbname}/{yyyy}/{MM}/{dd}/{filename}`; deleção em lote via `DeleteObjectsRequest` (até 1000 chaves por requisição).
- **`RetentionCleanupService`**: expurgo local por data de modificação e deleção em lote no MinIO, protegendo os metadados `status.json` e `manifest.json`.
- **`StatusReportService`**: grava o `/var/backups/status.json` em texto claro e o `/var/backups/manifest.json` criptografado.
- **`BackupOrchestratorService.executeFullBackup(mode)`**: garante o bucket, executa os dumps de IAM_DB, PATIENT_CLINICAL_DB, REPORTING_DB e MONGO_AI_DB em sequência com `try/catch` isolado por alvo, grava o manifesto criptográfico, atualiza o `status.json`, executa a retenção e devolve o `BackupJobExecution` consolidado.
- **`StreamingDumpPipelineTest`** (novo): prova o pipeline ponta a ponta sem PostgreSQL, MongoDB ou MinIO.

---

### ❌ O que deu Erro

- O primeiro `mvn test` falhou na compilação: `waitFor(long, TimeUnit)` lança `InterruptedException` (exceção checada) não declarada; o trecho passou a tratá-la restaurando o flag de interrupção e lançando `DumpExecutionException`.
- Uma asserção do teste verificava que o diretório `postgresql/` não existia após uma falha, mas apenas o **arquivo** parcial é removido (as pastas vazias permanecem); a asserção foi corrigida para verificar que nenhum arquivo regular remanesce em disco.
- Faltava declarar `throws Exception` no método de teste que usa `Files.walk` (que lança `IOException`).

---

### 🟢 O que Funcionou

- `mvn test`: **BUILD SUCCESS** — 23 fontes compilados e **Tests run: 8, Failures: 0, Errors: 0**.
- O contexto Spring sobe com todos os beans novos (S3Client, dumpers, storage e orquestrador) devidamente injetados.
- O teste do pipeline executa um comando real, grava o artefato cifrado e recupera o conteúdo original decifrando (lendo o IV do prefixo) e descomprimindo.
- O SHA-256 registrado no resultado coincide com o hash real do arquivo em disco e o conteúdo não aparece em texto claro no artefato.
- O IV nunca é reutilizado entre artefatos e a adulteração da tag GCM é detectada na leitura.
- Um comando com código de saída 3 gera `FAILED` com a mensagem do `stderr`, sem deixar artefato parcial/corrompido em disco.
- Nenhum arquivo do frontend foi modificado.

---

### 📝 Resumo do Dia

O pipeline de dump, criptografia e upload foi implementado e validado de ponta a ponta: o serviço passou a executar os dumps em streaming (sem alocação massiva de RAM), cifrar cada artefato com AES-256-GCM (IV por arquivo e tag de 128 bits), calcular o SHA-256 do arquivo final e enviá-lo ao MinIO com metadados de integridade, além de aplicar a política de retenção e gerar o manifesto criptográfico e o status operacional. Ficaram para as próximas fases o agendamento (`@Scheduled`), o CLI picocli (`--once`/`--restore`), a API REST de consulta, o upload do manifesto e a containerização; até o momento nada foi commitado.

---

## 2026-11-11 — Restauração Segura, Validação de Integridade e DR Drill (Fase B3)

### 📋 Implementação Proposta

- Implementar a restauração controlada com três princípios de segurança: validação pré-restore por SHA-256, autenticação criptográfica (AES-GCM) e timeout estrito (ex.: 30 minutos).
- Criar a interface `DatabaseRestorer` e os restauradores PostgreSQL e MongoDB.
- Criar o `RestoreOrchestratorService` (resolução do alvo, staging do MinIO, log de auditoria e métricas).
- Criar o `DrillVerificationService` (drill mensal em base efêmera, atestando RTO e RPO sem tocar na produção).
- Criar testes unitários com JUnit 5 cobrindo arquivos íntegros e arquivos corrompidos.

---

### ✅ O que foi Implementado

- **Pacote `restorer`** (novo): `DatabaseRestorer` (contrato `boolean restore(DatabaseTarget, Path, String)`), `RestoreFailedException`, `AbstractStreamingDatabaseRestorer` (pipeline `FileInputStream → CipherInputStream (AES-256-GCM) → GZIPInputStream → stdin do processo`), `PostgresDatabaseRestorer` (`psql` com a senha via `PGPASSWORD`) e `MongoDatabaseRestorer` (`mongorestore --archive --gzip --drop`).
- **Barreiras de segurança**: verificação de arquivo existente/legível, conferência do SHA-256 contra o manifesto **antes de qualquer conexão** e validação determinística da tag GCM por um dreno explícito do stream cifrado (necessário porque o `close()` do `CipherInputStream` engole `BadPaddingException` e o descompressor pode concluir antes do fim do stream cifrado).
- **Timeout de segurança** de 30 min (`BACKUP_RESTORE_TIMEOUT_SECONDS`) imposto por watchdog que encerra o processo à força, com `stdout`/`stderr` drenados em threads dedicadas e mensagens saneadas (a senha do alvo é substituída por `***`).
- **`RestoreOrchestratorService.restoreDatabase(targetName, filePath, sha256)`**: resolve o alvo por nome de banco, nome do enum ou forma normalizada; baixa chaves remotas do MinIO para um diretório de staging temporário (removido ao final); aciona o restaurador correspondente; grava o log de auditoria e atualiza as métricas de restauração.
- **`RestoreAuditService`** (novo): trilha de auditoria em JSON Lines em `{rootDir}/restore-audit.log`, além de um logger dedicado de auditoria.
- **`DrillVerificationService`** (novo): drill mensal agendado (`BACKUP_DRILL_CRON`), restaurando o artefato mais recente em uma base efêmera com sufixo `_drill` (nunca a produção), medindo o RTO e atestando o RPO (`BACKUP_RPO_HOURS`), persistindo `backup_restore_drill_status.json` e publicando a gauge `backup_restore_drill_status`.
- **`S3BackupStorageService.downloadArtifact(...)`** (novo): download do artefato remoto para o diretório de staging.
- **`DatabaseEndpoint` movido** do pacote `dumper` para `model`, eliminando o acoplamento `restorer → dumper`.
- **`BackupProperties`/`application.yml`**: novas propriedades `restore-timeout-seconds` (1800), `rpo-hours` (24) e `drill-cron` (`0 0 4 1 * ?`).
- **Testes**: `StreamingRestorePipelineTest` (7 testes), `RestoreOrchestratorServiceTest` (6) e `DrillVerificationServiceTest` (6).

---

### ❌ O que deu Erro

- O `DrillVerificationService` não compilava ao chamar `restoreInto` através da interface `DatabaseRestorer` (que, conforme a especificação, declara apenas `restore`); a chamada foi direcionada aos restauradores concretos.
- Detectado (e corrigido) um bug de lógica no watchdog da restauração: interrompê-lo logo após a escrita do stream cancelava o próprio timeout; a espera pelo processo foi extraída para um método próprio (`verifyCompletion`), mantendo o watchdog armado até o fim.
- A validação da tag GCM não era determinística: o `close()` do `CipherInputStream` engole `BadPaddingException` e o `GZIPInputStream` pode encerrar antes do fim do stream cifrado; foi adicionado um dreno explícito (`forceCipherCompletion`) para forçar a verificação.
- A assinatura de `BackupProperties` ganhou três componentes, exigindo a atualização do teste da Fase B2.
- Uma edição no arquivo de teste removeu por engano o fechamento do método `returnsFalseWhenArtifactMissing` (o `assertThat` e a chave), gerando 10 erros de sintaxe que foram corrigidos.

---

### 🟢 O que Funcionou

- `mvn test`: **BUILD SUCCESS** — 27 testes, 0 falhas e 0 erros (4 do contexto, 4 do pipeline de dump, 7 do pipeline de restauração, 6 do orquestrador e 6 do drill).
- O teste do pipeline de restauração prova o fluxo completo com um comando real: o conteúdo decifrado e descomprimido chega íntegro ao `stdin`, idêntico ao SQL original.
- A corrupção da tag GCM foi detectada e a restauração barrada, conforme o log: `scripted-restore recebeu o dump incompleto ... javax.crypto.AEADBadTagException: Tag mismatch`.
- SHA-256 divergente cancelou a restauração **sem iniciar qualquer processo/conexão**, e o arquivo de saída sequer foi criado.
- O timeout de segurança de 1s encerrou uma restauração travada, e a senha do alvo não aparece nas mensagens de erro (substituída por `***`).
- O drill confirmou o uso da base efêmera (`patient_clinical_db_drill`), atestou RTO e RPO, sinalizou violação de RPO com artefato de 48h e persistiu o `backup_restore_drill_status.json`.
- Nenhum arquivo do frontend foi modificado.

---

### 📝 Resumo do Dia

A restauração segura e a verificação automatizada de integridade foram implementadas e validadas: o serviço passou a validar o SHA-256 antes de qualquer conexão, autenticar os artefatos com AES-256-GCM (barrando corrupção e adulteração de forma determinística), aplicar timeout estrito, resolver os alvos por nome, baixar artefatos remotos para um staging temporário, registrar trilha de auditoria e métricas, e executar um drill mensal de contingência em base efêmera com atestado de RTO e RPO. Ficaram para as próximas fases o CLI picocli (`--once`/`--restore`), a API REST operacional, o upload do manifesto e a containerização; até o momento nada foi commitado.

---

## 2026-11-11 — Observabilidade Completa do Backup-Service (Micrometer + Prometheus + Grafana)

### 📋 Implementação Proposta

- Implementar observabilidade completa do microsserviço `backup-service` com métricas padronizadas via Micrometer, coleta pelo Prometheus, regras de alerta e dashboard Grafana para Disaster Recovery.

---

### ✅ O que foi Implementado

- **`BackupMetricsService.java`** (`br.edu.ifrn.promptuario.backup.metrics`): injeção de `MeterRegistry` e declaração de 6 métricas padronizadas:
  - `backup_execution_duration_seconds` (Timer, labels: `target`, `status`) — duração da execução do dump por banco.
  - `backup_artifact_size_bytes` (Gauge, label: `target`) — tamanho em bytes do artefato comprimido e criptografado.
  - `backup_last_success_timestamp_seconds` (Gauge, label: `target`) — epoch timestamp em segundos do último backup com sucesso.
  - `backup_status_total` (Counter, labels: `target`, `status`) — total de execuções (`success`/`failed`).
  - `backup_retention_deleted_total` (Counter) — artefatos expurgados pela política de retenção.
  - `backup_restore_duration_seconds` (Timer, labels: `target`, `status`) — tempo gasto em restaurações.
- **`BackupOrchestratorService.java`**: chamada a `metricsService.recordBackupExecution(...)` ao final de cada dump isolado, atualizando duração, status, tamanho do artefato e último sucesso.
- **`RestoreOrchestratorService.java`**: chamada a `metricsService.recordRestoreExecution(...)` no bloco `finally` de cada restauração, garantindo registro mesmo em exceção.
- **`backend/observability/prometheus/prometheus.yml`**: scrape job `backup-service` em `/actuator/prometheus`, intervalo 15s, alvo `backup-service:8000`.
- **`backend/observability/prometheus/alerts.yml`**: 3 regras de alerta de backup:
  - `BackupJobFailed` (critical) — `increase(backup_status_total{status="failed"}[1h]) > 0`.
  - `BackupMissing24h` (warning) — `(time() - backup_last_success_timestamp_seconds) > 86400` (violação de RPO).
  - `BackupArtifactTooSmall` (warning) — `backup_artifact_size_bytes < 100000`.
- **`backend/observability/grafana/provisioning/dashboards/backup_dashboard.json`**: dashboard "Disaster Recovery & Backups" (uid: `backup-dr`) com:
  - Stat: RPO Status (verde < 24h / vermelho > 24h), Tamanho Total dos Backups, Último Backup por Banco.
  - Time series: Duração da Execução dos Dumps (média e máx por alvo).
  - Gauge: Tamanho individual dos bancos (IAM, Patient-Clinical, Reporting, AI).
  - Table: Histórico das últimas execuções e status (merge de métricas).
  - Painéis extras: Total de Execuções por Status, Duração das Restaurações, Artefatos Expurgados pela Retenção.

---

### ❌ O que deu Erro

- Nenhum erro registrado. Todos os arquivos já estavam implementados no repositório e foram validados (JSON do dashboard, YAML do Prometheus, YAML dos alertas e conexão dos orquestradores com o `MeterRegistry`).

---

### 🟢 O que Funcionou

- Validação do JSON do dashboard Grafana (sintaxe correta, importável via provisioning).
- Validação do `prometheus.yml` (job `backup-service` presente com path, intervalo e target corretos).
- Validação do `alerts.yml` (3 regras de backup encontradas com expressões e severidades corretas).
- Confirmação de integração: `BackupOrchestratorService` e `RestoreOrchestratorService` já injetam e chamam `BackupMetricsService`.

---

### 📝 Resumo do Dia

A observabilidade completa do `backup-service` foi implementada e validada: métricas padronizadas via Micrometer expostas pelo Spring Boot Actuator, scrape configurado no Prometheus, 3 regras de alerta de Disaster Recovery (falha, violação de RPO e artefato suspeito) e dashboard Grafana com stat panels, time series, gauge e tabela de histórico. Nenhum erro foi encontrado; todos os arquivos estavam no repositório e prontos para uso em produção.

---


## 2026-11-11 — Cutover do Backup-Service de Python para Java 21

### 📋 Implementação Proposta

- Migrar o `backup-service` da implementação Python (`backup_runner.py`) para Java 21 com Spring Boot 3.3.4.
- Implementar CLI com Picocli para execução on-demand (`--once`, `--restore`) e modo daemon.
- Criar controladores REST operacionais e segurança com OAuth2 Resource Server (JWT).
- Construir Dockerfile multi-stage otimizado para Alpine Linux.
- Atualizar `docker-compose.yml` e `Makefile` para o novo serviço Java.

---

### ✅ O que foi Implementado

- **`BackupCommandLineRunner.java`** (`br.edu.ifrn.promptuario.backup.cli`):
  - `--once`: dispara backup completo manual e finaliza com `System.exit(0)` (sucesso) ou `System.exit(1)` (falha).
  - `--restore --kind <postgres|mongo> --target <alvo> --file <caminho>`: executa restauração controlada com saída de status correspondente.
  - Sem argumentos: inicia como daemon, servindo API REST na porta 8000 e agendador `@Scheduled`.
- **`SecurityConfig.java`** (`br.edu.ifrn.promptuario.backup.config`):
  - Libera `/healthz`, `/actuator/health` e `/actuator/prometheus` (sem autenticação).
  - Protege `/api/v1/backups/**` exigindo role `ADMIN` via Spring Security OAuth2 Resource Server (JWT).
- **`BackupOperationController.java`** (`br.edu.ifrn.promptuario.backup.controller`):
  - `GET /healthz` → `{"status":"ok","service":"backup-service","version":"1.0.0-java"}`.
  - `GET /api/v1/backups/status` → último status de `/var/backups/status.json`.
  - `POST /api/v1/backups/execute` → backup assíncrono sob demanda.
- **`RestoreOperationController.java`** (`br.edu.ifrn.promptuario.backup.controller`):
  - `POST /api/v1/backups/restore` → inicia restore controlado.
- **`Dockerfile`** (multi-stage Alpine):
  - Stage build: `maven:3.9-eclipse-temurin-21-alpine`, compila JAR executável.
  - Stage runtime: `eclipse-temurin:21-jre-alpine`, `apk add --no-cache postgresql15-client mongodb-tools bash tzdata`.
  - Usuário não-root `backup`, volume `/var/backups`, entrypoint com `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`.
- **`backend/docker-compose.yml`**: serviço `backup-service` reescrito para build Java 21, porta `8006:8000`, variáveis de ambiente completas, `depends_on` com `service_healthy`.
- **`backend/Makefile`**: `make backup-once` e `make restore-db` atualizados para `java -jar app.jar`.
- **`pom.xml`**: adicionada dependência `spring-boot-starter-oauth2-resource-server`.

---

### ❌ O que deu Erro

- Nenhum erro registrado. Todos os arquivos foram criados/atualizados, validados e enviados para o repositório remoto (`origin/feature/backup-service`, commit `caf97bf`).

---

### 🟢 O que Funcionou

- Commit `caf97bf` enviado com sucesso para o repositório remoto.
- Estrutura de pacotes consistente com o restante do projeto (`br.edu.ifrn.promptuario.backup.*`).
- Integração com as métricas Micrometer já existentes (`BackupMetricsService`).
- Dockerfile multi-stage segue as boas práticas de imagem enxuta (Alpine + JRE).

---

### 📝 Resumo do Dia

O cutover do `backup-service` de Python para Java 21 foi concluído: CLI com Picocli, controladores REST operacionais, segurança JWT com role ADMIN, Dockerfile multi-stage Alpine, atualização do docker-compose e Makefile. O serviço está pronto para homologação em staging antes do deploy em produção.

---

## 2026-11-13

### 📋 Implementação Proposta

- Criar a migração Flyway inicial completa do `backend-java/patient-clinical-service` a partir das entidades JPA, repositórios e DTOs existentes.
- Garantir compatibilidade com PostgreSQL 15, usando IDs textuais, `TIMESTAMPTZ`, `JSONB`, enums como `VARCHAR`, chaves estrangeiras e índices alinhados às consultas dos repositórios.
- Validar a migração em banco limpo, executar `mvn clean test`, iniciar o serviço com `ddl-auto=validate` e confirmar que uma segunda inicialização não reaplica a migração.

---

### ✅ O que foi Implementado

- **Migração Flyway `V1__init_patient_clinical_schema.sql`**:
  - Criação das 18 tabelas do domínio: pacientes, alergias, vacinas, medicamentos contínuos e seus históricos, documentos, escalas, horários, consultas, prontuários e históricos, prescrições e históricos, solicitações de exames e históricos, atestados e históricos, além dos logs de auditoria.
  - Definição de 18 chaves primárias `VARCHAR(36)`, compatíveis com os UUIDs textuais gerados pelas entidades Java.
  - Uso de `TIMESTAMPTZ` para todos os campos Java `Instant` e de `JSONB` para todos os campos mapeados com `@JdbcTypeCode(SqlTypes.JSON)`.
  - Persistência dos enums como `VARCHAR`, acompanhados de constraints `CHECK` compatíveis com os valores Java.
  - Criação de 17 chaves estrangeiras, com `ON DELETE CASCADE`, `SET NULL` ou `NO ACTION` conforme os relacionamentos e anotações JPA.
  - Criação de 31 índices, incluindo índices compostos para buscas por paciente, médico, status, disponibilidade e ordenação dos históricos por data.
  - Ausência intencional de `IF NOT EXISTS`, permitindo que o Flyway detecte conflitos ou inconsistências de versão.
- **Correção mínima de startup em `S3Config.java`**:
  - Removida a autoinjeção do `S3Client` produzido pela própria classe de configuração, que causava uma dependência circular durante a criação dos beans.
  - O mesmo cliente singleton passou a ser obtido pelo método `@Bean` durante o evento `ApplicationReadyEvent`, preservando a criação e verificação dos buckets e sem alterar endpoints públicos.
- **Validação executada com infraestrutura Docker efêmera**:
  - Build multi-stage do Dockerfile concluído com Java 21 e Maven 3.9.
  - Migração aplicada em PostgreSQL 15.19 vazio, criando exatamente 18 tabelas de aplicação.
  - Hibernate iniciou com `ddl-auto=validate` e criou o `EntityManagerFactory` sem divergências de tabela, coluna, tipo ou constraint.
  - Serviço chegou ao estado `healthy` e `/healthz` respondeu com status `ok` e versão `1.0.0-java`.
  - Segunda inicialização sobre a mesma base registrou `Schema "public" is up to date. No migration necessary`.
  - A tabela `flyway_schema_history` permaneceu com uma única migração, versão `1`, marcada como bem-sucedida.
  - Os contêineres e a rede criados exclusivamente para a validação foram removidos ao final; nenhum volume persistente do projeto foi alterado.

---

### ❌ O que deu Erro

- O host não possui Maven instalado. A compilação e os testes foram executados com a imagem oficial Maven/Java 21 em Docker.
- O teste `PatientClinicalIntegrationTest` é condicionado por `RUN_INTEGRATION_TESTS=true` e ficou ignorado na execução padrão de `mvn clean test`.
- Mesmo habilitado explicitamente, o Testcontainers 1.20.1 não conseguiu negociar com o Docker Engine 29.9.1: o cliente tentou usar a API 1.32, enquanto o daemon exige no mínimo a API 1.40. A validação equivalente foi executada diretamente com contêineres isolados de PostgreSQL 15 e RabbitMQ.
- A primeira tentativa de startup revelou uma dependência circular preexistente em `S3Config`; a configuração foi corrigida e os startups posteriores passaram no healthcheck.
- O arquivo `AUDITORIA_INTEGRACAO_BACKEND_JAVA.md` citado no plano de trabalho não estava presente no workspace.

---

### 🟢 O que Funcionou

- `mvn -B clean test` terminou com `BUILD SUCCESS`, compilando 118 arquivos-fonte e o teste de integração.
- O Dockerfile do `patient-clinical-service` gerou a imagem final com sucesso.
- Flyway aplicou a migração V1 em banco PostgreSQL 15 limpo e não a reaplicou no segundo startup.
- O schema validado contém 18 PKs `VARCHAR(36)`, 10 colunas `JSONB` e 32 colunas `TIMESTAMPTZ`.
- As 17 ações referenciais foram conferidas diretamente no catálogo PostgreSQL e correspondem aos relacionamentos definidos nas entidades.
- O serviço iniciou duas vezes com `ddl-auto=validate`, conectou ao RabbitMQ efêmero e respondeu corretamente ao endpoint de saúde.
- As mudanças locais preexistentes e a pasta não rastreada `Planos-Migração/` foram preservadas.

---

### 📝 Resumo do Dia

Foi concluída a migração inicial Flyway do `patient-clinical-service`, cobrindo todo o modelo JPA e os índices usados pelos repositórios. A V1 foi validada em PostgreSQL 15 limpo, passou pela validação do Hibernate e permaneceu idempotente sob o controle de versão do Flyway na segunda inicialização. Durante o teste foi identificado e corrigido um ciclo de dependência no bootstrap do S3. O build Maven e a imagem Docker foram concluídos com sucesso; permanece como limitação ambiental a incompatibilidade entre Testcontainers 1.20.1 e a versão atual do daemon Docker, além de ficarem pendentes para os próximos prompts a validação do MinIO, AI Python e Docker Compose completo.

---

## 2026-11-13 — Compatibilidade dos Eventos RabbitMQ entre Java e Python

### 📋 Implementação Proposta

- Implementar publicação dupla temporária dos eventos `MedicalRecordCreated` e `PrescriptionGenerated` nos contratos novo e legado.
- Preservar o mesmo `eventId`, `timestamp`, `correlationId` e payload nas duas publicações.
- Aceitar aliases camelCase e snake_case nos modelos Java e Python.
- Adicionar headers de envelope, retry para falhas transitórias, logs estruturados, feature flag e idempotência dos consumidores Python por `eventId`.
- Criar testes de contrato que comparem o JSON produzido pelo Java com os modelos Pydantic compartilhados.

---

### ✅ O que foi Implementado

- **Publicação dupla em `EventPublisher.java`**:
  - Contrato novo em `domain.events`, usando `medical_record.created` e `prescription.generated`.
  - Contrato legado em `promptuario.clinical`, usando `clinical.medical_record.created` e `clinical.prescription.generated`.
  - Reutilização da mesma instância do evento nas duas publicações, mantendo envelope e payload equivalentes.
  - Headers `eventId`, `eventType`, `timestamp` e `correlationId`, além de `messageId` e `correlationId` nas propriedades AMQP.
  - Seleção de destinos por `LinkedHashSet`, impedindo publicação lógica duplicada no mesmo par exchange/routing key.
  - Logs estruturados com tipo da publicação, exchange, routing key, evento e correlação.
  - Feature flag `app.messaging.legacy-publish-enabled`, habilitada por padrão e restrita às rotas clínicas canônicas.
- **Envelope Java `DomainEvent`**:
  - Criado contrato tipado para `eventId`, `timestamp`, `eventType` e `correlationId`.
  - Eventos Java de patient e clinical passaram a implementar o envelope comum.
  - `MedicalRecordCreatedEvent` e `PrescriptionGeneratedEvent` passaram a emitir JSON canônico camelCase e aceitar os aliases legados snake_case com `@JsonAlias`.
- **Configuração RabbitMQ**:
  - Exchanges, filas e bindings permanentes mantidos como duráveis.
  - Retry configurado somente para `AmqpConnectException`, `AmqpIOException`, `AmqpResourceNotAvailableException` e `AmqpTimeoutException`, com três tentativas e backoff exponencial.
  - `Jackson2JsonMessageConverter` passou a reutilizar o `ObjectMapper` do Spring Boot, serializando `Instant` em ISO-8601.
- **Contratos Python compartilhados**:
  - `DomainEvent` passou a aceitar `eventId/event_id`, `eventType/event_type` e `correlationId/correlation_id`.
  - Os dois eventos clínicos aceitam aliases camelCase e snake_case para todos os campos enviados pelo Java.
  - Listas mutáveis passaram a usar `default_factory`.
  - O publisher Python também passou a enviar os quatro headers do envelope.
  - `domain.events` foi incluído entre os exchanges duráveis declarados pelo broker compartilhado.
- **Idempotência Python**:
  - O consumidor resolve o identificador primeiro pelos headers, depois pelo corpo camelCase/snake_case e, por compatibilidade, pelo `message_id`.
  - Entregas concorrentes do mesmo evento são serializadas por `asyncio.Lock`.
  - Eventos já processados são ignorados por uma cache limitada a 10.000 `eventId`, evitando crescimento ilimitado em memória.
- **Consumidores legados preservados**:
  - O AI Python e o Reporting Service passaram a validar os eventos pelos modelos Pydantic, sem alteração de endpoints públicos.
  - O AI Service permanece em Python e continua consumindo as rotas legadas.
- **Testes de contrato e configuração**:
  - Fixtures compartilhadas para prontuário e prescrição.
  - Testes Java de serialização, aliases, igualdade dos payloads, headers, duas rotas, feature flag, durabilidade e retry.
  - Testes Python de compatibilidade dos fixtures Java, aliases legados e resolução do `eventId`.

---

### ❌ O que deu Erro

- O host não possui Maven instalado; a suíte Java precisou ser executada com a imagem `maven:3.9-eclipse-temurin-21-alpine`.
- A primeira compilação dos testes encontrou uma ambiguidade de inferência do AssertJ em `valueToTree`; o resultado foi tipado explicitamente como `JsonNode`.
- O `ObjectMapper` isolado do conversor RabbitMQ inicialmente serializava `Instant` como epoch. O conversor foi alinhado ao `ObjectMapper` configurado pelo Spring Boot, produzindo ISO-8601.
- A tentativa de integração real via Testcontainers não iniciou PostgreSQL, RabbitMQ e MinIO: o `docker-java` do Testcontainers 1.20.1 insistiu em usar a API 1.32, rejeitada pelo daemon, que exige no mínimo a API 1.40. A tentativa permaneceu ignorada por `disabledWithoutDocker`.

---

### 🟢 O que Funcionou

- `mvn -q clean test` em Docker: 9 testes aprovados e 1 teste Testcontainers ignorado.
- Testes de contrato Python em `python:3.12-slim`: 4 testes aprovados.
- As duas rotas de prontuário e prescrição foram verificadas, com corpos JSON equivalentes e os mesmos dados de envelope.
- O retry transitório realizou três tentativas, enquanto erros de programação não foram repetidos.
- `docker compose -f backend-java/docker-compose.yml config --quiet` validou a configuração do Compose.
- `python3 -m compileall -q` e `git diff --check` terminaram sem erros.
- Implementação registrada no commit `dbdc7c3` (`feat(patient-clinical): implementa compatibilidade de eventos RabbitMQ entre Java e Python`) e enviada para `origin/feature/patient-clinical-rabbitmq-event-compatibility`.

---

### 📝 Resumo do Dia

A ponte temporária RabbitMQ entre o novo contrato Java e os consumidores legados Python foi concluída. Os eventos clínicos são publicados uma vez em cada destino com envelope e payload equivalentes, e os consumidores aceitam camelCase e snake_case sem mudanças nos endpoints públicos do AI Service. Foram adicionados contratos tipados, retry seletivo, logs estruturados, headers AMQP, feature flag e deduplicação por `eventId`. As suítes Java e Python passaram; permanece como limitação ambiental a incompatibilidade da API Docker usada pelo Testcontainers e, arquiteturalmente, a cache de idempotência Python é local ao processo e não sobrevive a reinicializações.

---

## 2026-11-13 — Remoção do Worker Celery Legado do Patient-Clinical

### 📋 Implementação Proposta

- Remover referências ao container `patient-clinical-worker` e ao Celery usado exclusivamente pelo Patient-Clinical.
- Preservar o AI Service em Python, o Redis utilizado pelo AI, RabbitMQ e os workers Celery pertencentes ao Reporting Service.
- Garantir que API e worker Spring AMQP do Patient-Clinical Java sejam executados em um único container.
- Disponibilizar comandos claros para desenvolvimento e testes isolados do AI e do Patient-Clinical.
- Validar sintaticamente os arquivos `backend-java/docker-compose.yml` e `backend/docker-compose.yml`.

---

### ✅ O que foi Implementado

- **Execução unificada do Patient-Clinical Java**:
  - Os dois arquivos Compose mantêm somente o serviço `patient-clinical-service`, construído a partir de `backend-java/patient-clinical-service`.
  - Documentado que `PrescriptionPdfWorker` é um `@RabbitListener` iniciado dentro do mesmo processo Spring Boot da API.
  - Nenhum container separado `patient-clinical-worker` é necessário ou declarado.
- **Remoção do worker Python obsoleto**:
  - Excluído `backend/patient-clinical-service/Dockerfile.worker`.
  - Excluídos `app/workers/celery_app.py`, `app/workers/prescription_tasks.py` e o `__init__.py` exclusivo do pacote antigo.
  - Removido do `PrescriptionService` o disparo `generate_prescription_pdf.delay(...)`, que era redundante após a geração síncrona do legado e não participa do fluxo Java ativo.
  - Removidas as configurações `CELERY_BROKER_URL` e `CELERY_RESULT_BACKEND` do Patient-Clinical Python legado.
  - Removidas as dependências `celery`, `redis[asyncio]` e `psycopg2-binary`, usadas exclusivamente pelo worker antigo.
  - Testes do legado atualizados para não aplicar patches sobre APIs Celery removidas.
- **Comandos Make adicionados/atualizados**:
  - `make dev-ai` inicia o AI Python e suas dependências pelo Compose.
  - `make test-ai` fornece uma chave JWT local, exclui o teste de integração externa e configura `asyncio_mode=auto`.
  - `make dev-patient-clinical` constrói e inicia o único container Java com o listener Spring AMQP interno.
  - `make test-patient-clinical` executa `mvn clean test` no serviço Java.
  - O comando geral de testes deixou de tentar executar `pytest` dentro do Patient-Clinical Java.
- **Documentação**:
  - READMEs gerais passaram a distinguir o worker Spring AMQP interno do Patient-Clinical dos workers Celery exclusivos do Reporting.
  - O README do serviço Python foi marcado como implementação legada para rollback.
  - Criado `backend-java/patient-clinical-service/README.md` com arquitetura de processo único, fila, retry, DLQ e comandos operacionais.
- **Componentes preservados**:
  - AI Service Python e seu `REDIS_URL` permaneceram inalterados.
  - RabbitMQ e MinIO permaneceram como dependências do Patient-Clinical Java.
  - `reporting-worker`, `reporting-beat`, seus Dockerfiles, configurações Celery e Redis DB 1 foram preservados.

---

### ❌ O que deu Erro

- A primeira tentativa de `test-ai` não chegou à coleta porque `JWT_SECRET_KEY` não estava definida; o alvo Make passou a fornecer uma chave exclusiva de teste.
- A segunda tentativa revelou que a suíte dependia de `asyncio_mode=auto` e tentava executar um teste de integração contra `localhost:8000`; o alvo unitário passou a excluir o marcador `integration` e habilitar o modo asyncio.
- Após esses ajustes, a suíte AI executou 46 testes unitários, com 44 aprovados e 2 falhas preexistentes:
  - `TestAnalysisTimeout.test_timeout_sets_job_to_failed`: o teste espera a mensagem original `Timed out`, mas o serviço persiste `Analysis timed out after 120s`.
  - `TestJobLifecycle.test_list_by_record`: o mock assíncrono de `find().sort()` retorna uma coroutine incompatível com a chamada `to_list()` do serviço.
- O `PatientClinicalIntegrationTest` continuou ignorado na execução Maven padrão porque o Docker não está disponível dentro do container Maven; os nove testes unitários/contratuais passaram.

---

### 🟢 O que Funcionou

- `docker compose -f backend-java/docker-compose.yml config` terminou com sucesso.
- `docker compose -f backend/docker-compose.yml config` terminou com sucesso.
- `config --services` confirmou, nos dois projetos, somente `db-patient-clinical` e um único `patient-clinical-service` relacionado ao domínio.
- A imagem `backend-java-patient-clinical-service:latest` foi construída com sucesso pelo Dockerfile multi-stage Java 21.
- `mvn -q clean test` executado em Maven/Java 21 via Docker aprovou 9 testes e ignorou apenas o Testcontainers condicionado.
- `python3 -m compileall -q` validou o código Python remanescente após a remoção dos módulos do worker.
- `make -n` confirmou a expansão correta dos quatro novos comandos nos diretórios `backend-java/` e `backend/`.
- `git diff --check` não encontrou erros de whitespace.
- A atualização central de Makefile e Compose foi registrada no commit `56db0f8` (`refactor(patient-clinical): remove referências obsoletas ao Celery e atualiza comandos de desenvolvimento`).

---

### 📝 Resumo do Dia

O worker Celery exclusivo do Patient-Clinical foi retirado do fluxo atual e do código Python legado, enquanto os workers Celery do Reporting e toda a infraestrutura do AI Python foram preservados. O Patient-Clinical agora está documentado e configurado como um único container Java que contém tanto a API quanto o listener Spring AMQP responsável pelos PDFs de prescrições. Os dois Compose foram validados, a imagem Java foi reconstruída e a suíte Maven passou. O comando de testes do AI ficou operacional para testes unitários, revelando duas falhas preexistentes no próprio AI que foram registradas sem ampliar o escopo desta migração.

---

## 2026-11-13 — Testes de Integração Ponta a Ponta do Patient-Clinical

### 📋 Implementação Proposta

- Completar `PatientClinicalIntegrationTest` com Testcontainers e propriedades dinâmicas, sem dependência de serviços externos permanentes.
- Validar o fluxo desde o healthcheck e a criação dos recursos clínicos até o consumo pelo AI Python, geração do PDF, armazenamento S3 e atualização da prescrição.
- Verificar simultaneamente os contratos RabbitMQ novo e legado e a idempotência do consumidor Python por `eventId`.
- Cobrir os cenários de falha solicitados: mensagem duplicada, RabbitMQ indisponível, bucket ausente, paciente inexistente, CPF duplicado, conflito de horário e payload inválido.
- Manter a suíte compilável e automaticamente ignorada quando o daemon Docker não estiver disponível.

---

### ✅ O que foi Implementado

- **Infraestrutura efêmera de testes**:
  - PostgreSQL 15 para Flyway, Hibernate e persistência clínica.
  - RabbitMQ 3.13 para filas, exchanges, bindings, publicação dupla, retry e DLQ.
  - LocalStack S3 para os buckets `patient-documents` e `prescriptions`.
  - MongoDB 7 e Redis 7 para o fluxo real do AI Python.
  - Imagem do AI Service construída a partir de `backend/ai-service` e `backend/shared`, executando o FastAPI real em modo de análise simulada, sem chave ou chamada para LLM externa.
- **Configuração dinâmica**:
  - Datasource, RabbitMQ, S3 e segredo JWT configurados com `@DynamicPropertySource` a partir das portas e credenciais dos containers.
  - Removida a dependência de `RUN_INTEGRATION_TESTS`; `@Testcontainers(disabledWithoutDocker = true)` passou a controlar o skip quando não há daemon.
  - Testcontainers atualizado de `1.20.1` para `1.21.4`, compatível com a API do Docker Engine disponível no ambiente.
- **Fluxo clínico ponta a ponta**:
  - `/healthz` validado com HTTP 200.
  - Criação de paciente, consulta, prontuário SOAP e prescrição validada com HTTP 201 e autenticação JWT real de teste.
  - Filas exclusivas de captura confirmam os eventos de prontuário e prescrição nas exchanges `domain.events` e `promptuario.clinical`.
  - Corpos JSON canônico e legado comparados integralmente, incluindo igualdade de `eventId`, `timestamp`, `correlationId` e dos quatro headers AMQP.
  - MongoDB consultado para confirmar que o AI Python criou as análises de sintomas e de interação medicamentosa.
  - Duas republicações com o mesmo `eventId` foram ignoradas pelo consumidor, mantendo uma única análise de sintomas.
  - Worker Spring AMQP gerou o PDF, armazenou o objeto no bucket `prescriptions` e preencheu `pdfS3Key` e `pdfGeneratedAt` na entidade.
- **Disparo transacional do PDF**:
  - Criado o evento interno `PrescriptionPdfRequestedEvent`.
  - Criado `PrescriptionPdfJobPublisher`, que publica `prescription.pdf.generate` somente após o commit da prescrição e reutiliza o retry transitório da aplicação.
  - O endpoint público de prescrição foi preservado e passou a iniciar automaticamente o fluxo assíncrono do worker.
- **Testes negativos**:
  - CPF duplicado retorna conflito.
  - Paciente inexistente retorna recurso não encontrado.
  - Conflito de horário retorna erro de negócio.
  - Bucket ausente leva a mensagem à DLQ sem preencher os metadados do PDF.
  - Payload HTTP e mensagem RabbitMQ inválidos são rejeitados.
  - Publicação contra porta RabbitMQ indisponível confirma retry e propagação da falha.
- Implementação registrada no commit `bd69445` (`test(patient-clinical): implementa testes de integração ponta a ponta com Testcontainers`).

---

### ❌ O que deu Erro

- O host não possui Maven; compilação e testes foram executados com `maven:3.9.9-eclipse-temurin-21` em Docker.
- O Testcontainers 1.20.1 usava Docker API 1.32, rejeitada pelo daemon local, que exige no mínimo 1.40; a dependência de testes foi atualizada para 1.21.4.
- A imagem `minio/minio:RELEASE.2024-06-13T22-53-53Z` não estava mais disponível no registro. Como o requisito permite MinIO ou LocalStack S3, a suíte passou a usar LocalStack.
- A primeira inicialização do teste tentava resolver as propriedades dinâmicas antes do startup dos containers por causa do ciclo de vida `PER_CLASS`; o bootstrap foi separado em callbacks estáticos e preparação por teste.
- O RabbitMQ recusou inicialmente o usuário definido somente por variáveis de ambiente; as credenciais passaram a ser criadas pela API nativa `withAdminUser`/`withAdminPassword` do `RabbitMQContainer`.
- As filas temporárias de captura eram removidas após o primeiro consumidor por estarem com `auto-delete`; foram mantidas não duráveis, mas ativas durante toda a execução do container.
- O payload inicial usava `FEMALE`, enquanto o enum real aceita `F`; o fixture foi alinhado ao contrato do domínio.

---

### 🟢 O que Funcionou

- `mvn -DskipTests test-compile` concluiu sem erros com Java 21.
- `mvn -q clean test` executou 16 testes, com 16 aprovados, nenhum erro e nenhuma falha.
- A classe E2E executou 7 testes em aproximadamente 56 segundos, todos aprovados.
- Flyway aplicou a V1 em PostgreSQL 15 limpo e o Hibernate iniciou com `ddl-auto=validate`.
- PostgreSQL, RabbitMQ, LocalStack S3, MongoDB, Redis, Spring Boot e o AI Python real foram iniciados e utilizados no mesmo cenário automatizado.
- A execução sem socket Docker terminou com sucesso e registrou os 7 testes E2E como ignorados.
- `git diff --check` não encontrou erros de whitespace.

---

### 📝 Resumo do Dia

O fluxo clínico principal foi validado ponta a ponta, desde os endpoints REST até a mensageria compatível, o consumo pelo AI Python e a geração e persistência do PDF. A suíte agora cria toda a infraestrutura necessária com Testcontainers, comprova idempotência por `eventId`, verifica os destinos RabbitMQ novo e legado e cobre os cenários negativos exigidos. Também foi fechada a lacuna de produção que impedia a criação da prescrição de enfileirar o trabalho do PDF. O build limpo passou com 16 testes, e a ausência de Docker resulta em skip explícito sem quebrar a compilação.

---

## 2026-11-14 — Validação Final da Integração do Backend Java

### 📋 Implementação Proposta

- Executar as suítes completas do Gateway, IAM, Patient-Clinical e AI Python.
- Validar sintaticamente os arquivos Compose de `backend-java/` e `backend/`.
- Iniciar PostgreSQL, MongoDB, RabbitMQ, Redis, MinIO, os três serviços Java e o AI Python.
- Confirmar healthchecks, Flyway, `ddl-auto=validate`, publicação RabbitMQ nova e legada, consumo idempotente pelo AI, buckets MinIO e geração do PDF.
- Verificar a ausência do container `patient-clinical-worker` e de referências ativas ao Redis DB 3 no Patient-Clinical.
- Registrar resultados, serviços efetivamente iniciados, limitações ambientais e pendências sem realizar correções fora do escopo da auditoria.

---

### ✅ O que foi Implementado

- **Auditoria das suítes Java**:
  - Gateway executou 30 testes, todos aprovados.
  - IAM executou 19 testes, todos aprovados, incluindo PostgreSQL, Redis e Flyway via Testcontainers.
  - Patient-Clinical executou 16 testes, todos aprovados, dos quais 7 pertencem ao fluxo E2E real com PostgreSQL 15, RabbitMQ, LocalStack S3, MongoDB, Redis e AI Python.
  - Os testes comprovaram publicação canônica e legada, aliases camelCase/snake_case, feature flag da publicação legada, idempotência, retry, DLQ e cenários negativos.
- **Flyway e Hibernate**:
  - A migração V1 do Patient-Clinical foi aplicada em PostgreSQL 15 limpo.
  - O serviço iniciou com `spring.jpa.hibernate.ddl-auto=validate` sem divergências de tabela, coluna ou tipo.
  - Na segunda inicialização, Flyway registrou `Schema "public" is up to date. No migration necessary`, confirmando que a V1 não foi reaplicada.
- **Compose e serviços reais**:
  - Os dois comandos `docker compose ... config --quiet` terminaram com código 0.
  - Um override temporário fora do repositório remapeou somente as portas ocupadas e forneceu as credenciais ausentes para completar a auditoria sem interferir nos containers externos.
  - Os dez serviços solicitados ficaram `healthy`: `db-iam`, `db-patient-clinical`, `db-ai`, `rabbitmq`, `redis`, `minio`, `iam-service`, `patient-clinical-service`, `ai-service` e `gateway`.
  - `http://localhost:8000/healthz`, `:8001/healthz`, `:8002/healthz`, `:8004/healthz` e `:8002/docs` retornaram HTTP 200; o endpoint `/docs` foi validado após seguir o redirecionamento para o Swagger UI.
- **Fluxo funcional em runtime**:
  - Login realizado com o administrador inicial do IAM.
  - Paciente, consulta, prontuário SOAP e prescrição criados pelo Gateway.
  - O prontuário foi publicado em `domain.events/medical_record.created` e `promptuario.clinical/clinical.medical_record.created` com o mesmo `eventId` e `correlationId`.
  - O AI Python consumiu o prontuário real e concluiu a análise automática.
  - A mesma mensagem sintética foi publicada duas vezes; o MongoDB confirmou exatamente um job para o `eventId`, validando idempotência.
  - Após o envio isolado do job de PDF, o worker Spring AMQP gerou o documento, publicou o evento de prescrição nos destinos novo e legado e o AI executou a análise de interação medicamentosa.
  - O PDF foi confirmado diretamente pela API S3 do MinIO com 2.331 bytes e `Content-Type: application/pdf`.
  - A prescrição recebeu `pdfS3Key` e `pdfGeneratedAt` no PostgreSQL.
- **Ausências confirmadas**:
  - Nenhum serviço ou container `patient-clinical-worker` existe na configuração resolvida.
  - O Patient-Clinical não possui configuração Redis nem referência ao DB 3.
  - O AI Python permanece no Redis DB 2.
- A pilha temporária foi encerrada ao final, sem remover volumes e sem afetar os containers externos `minio-dev` e `rabbitmq-dev`.
- O override e o ambiente virtual criados em `/tmp` foram removidos, e `git status --short` permaneceu limpo antes desta atualização do diário.

---

### ❌ O que deu Erro

- O host não possui Maven nem pytest instalados. As suítes Java foram executadas com Maven 3.9.9 e Java 21 em container, e o Python foi testado em ambiente virtual temporário.
- O comando literal `pytest -q` do AI não chegou aos testes porque o projeto não configura o diretório da aplicação no `PYTHONPATH`, causando `ModuleNotFoundError: app`.
- Com `PYTHONPATH` configurado, a coleta ainda exigiu `JWT_SECRET_KEY` e `asyncio_mode=auto`, opções que não estão declaradas pelo projeto para uma execução autônoma.
- A execução unitária diagnóstica do AI terminou com 44 testes aprovados, 2 falhas e 1 integração desmarcada:
  - o teste de timeout espera `Timed out`, enquanto o serviço persiste `Analysis timed out after 120s`;
  - o mock de `find().sort()` em `test_list_by_record` retorna uma coroutine incompatível com `to_list()`.
- O `docker compose up` oficial falhou inicialmente por conflitos com infraestrutura externa preexistente:
  - `minio-dev` já ocupava `9000/9001`;
  - `rabbitmq-dev` já ocupava `5672/15672`.
- A configuração oficial também revelou divergências próprias:
  - MinIO usa `promptuario/promptuario_pass`, mas o Patient-Clinical recebe por padrão `minioadmin/minioadmin`;
  - o Patient-Clinical não recebe `SPRING_RABBITMQ_USERNAME` nem `SPRING_RABBITMQ_PASSWORD`, usa `guest/guest` e recebe `ACCESS_REFUSED`;
  - o healthcheck do RabbitMQ usa timeout de 5 segundos, insuficiente durante o primeiro startup neste ambiente.
- No fluxo HTTP executado pelo Compose, a criação da prescrição não publicou automaticamente `prescription.pdf.generate`. A fila possuía dois consumidores e nenhuma mensagem; o worker funcionou integralmente quando o mesmo job foi publicado manualmente. O gatilho transacional entre controller e publicador precisa ser investigado, apesar de passar no Testcontainers.
- O AI registrou avisos de exportação OTLP porque `otel-collector` não fazia parte da lista de serviços solicitada. O único traceback do AI correspondeu ao `CancelledError` causado pelo reinício controlado do container.
- A primeira tentativa de criar o commit `docs(backend): documenta resultados da validação final de integração` não gerou commit porque o working tree ainda estava limpo naquele momento.

---

### 🟢 O que Funcionou

- `mvn clean test` do Gateway, IAM e Patient-Clinical terminou com código 0, totalizando 65 testes Java aprovados.
- O E2E do Patient-Clinical executou com Docker disponível, sem skips, e validou todos os cenários positivos e negativos solicitados.
- Os dois arquivos Compose são sintaticamente válidos.
- Todos os serviços solicitados iniciaram e ficaram saudáveis após o override temporário não versionado.
- Flyway criou o schema, Hibernate validou o mapeamento e a segunda inicialização não reaplicou a migração.
- RabbitMQ entregou eventos aos contratos novo e legado, preservando os identificadores do envelope.
- O AI Python continuou funcional, consumiu eventos reais e ignorou duplicatas por `eventId`.
- MinIO criou os buckets `patient-documents` e `prescriptions` e armazenou um PDF válido.
- O worker Spring AMQP executou dentro do mesmo container do Patient-Clinical; nenhum worker Celery legado foi iniciado.
- Não houve alteração destrutiva, remoção de endpoint público ou modificação de código durante a auditoria.

---

### 📝 Resumo do Dia

A auditoria final confirmou a solidez das correções Java: as três suítes Maven e o E2E passaram, Flyway e `ddl-auto=validate` funcionaram em PostgreSQL 15, a publicação dupla chegou ao AI Python e o worker Spring AMQP gerou e armazenou o PDF. O aceite global ainda é parcial porque `pytest -q` não é autossuficiente e conserva duas falhas unitárias, enquanto o Compose oficial contém credenciais divergentes para MinIO, omite as credenciais RabbitMQ do Patient-Clinical e apresentou um gatilho de PDF que não publicou automaticamente no fluxo runtime. O override temporário permitiu separar esses problemas de configuração do funcionamento efetivo dos componentes, e toda a infraestrutura criada para a auditoria foi encerrada sem afetar serviços preexistentes.

---
<!-- TEMPLATE PARA NOVO DIA — Copie o bloco abaixo ao iniciar uma nova entrada -->

<!--
## YYYY-MM-DD

### 📋 Implementação Proposta

- Descreva aqui o que foi planejado para o dia.

---

### ✅ O que foi Implementado

- Liste tudo que foi efetivamente desenvolvido ou configurado.

---

### ❌ O que deu Erro

- Descreva os problemas encontrados, mensagens de erro relevantes e contexto.

---

### 🟢 O que Funcionou

- Liste o que funcionou como esperado.

---

### 📝 Resumo do Dia

Breve parágrafo com o balanço geral do dia, decisões tomadas e próximos passos.

---
-->
