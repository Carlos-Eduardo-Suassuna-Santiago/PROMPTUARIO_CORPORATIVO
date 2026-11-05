# 📓 Diário de Bordo — Projeto Integrador Corporativo

> Registro cronológico das atividades, decisões técnicas e ocorrências do projeto.

---

## Índice

- [2026-08-28](#2026-08-28)
- [2026-11-03](#2026-11-03)
- [2026-11-04](#2026-11-04)
- [2026-11-05 — Entrada mais recente](#2026-11-05)

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

## 2026-11-05

### 📋 Implementação Proposta

- Implementação do modelo de domínio, entidades JPA e repositórios Spring Data JPA para os módulos Shared, Patient e Clinical no serviço unificado (`/backend-java/patient-clinical-service`).
- Criação e estruturação da camada de DTOs (Data Transfer Objects) e validações com Jakarta Bean Validation (`jakarta.validation.constraints.*`) para os módulos Patient e Clinical na branch `feature/DTO-validacoes-jakarta`.
- Inicialização da Fase 1 do microsserviço de identidade e acesso em Java (`/backend-java/iam-service`), incluindo dependências Maven, configurações base e infraestrutura de integração.
- Implementação da Fase 3 do IAM Java: entidades JPA, enum de papéis, repositórios Spring Data e teste de integração com PostgreSQL 15.
- Implementação da Fase 4 do IAM Java: DTOs imutáveis com Java Records, validações Jakarta e compatibilidade dos contratos JSON com o frontend.
- Implementação da Fase 5 do IAM Java: JWT HS256, BCrypt, blacklist de tokens no Redis e configuração stateless do Spring Security.

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
  - **Camada de DTOs e Validações Jakarta (`modules/patient/dto`)**:
    - Criação de 18 DTOs com Java Records para entrada e saída de dados: `PatientCreateDto`, `PatientUpdateDto`, `PatientResponseDto`, `PatientListResponseDto`, `PatientSummaryResponseDto`, `AllergyCreateDto`, `AllergyDto`, `AllergyResponseDto`, `MedicationCreateDto`, `MedicationUpdateDto`, `MedicationDto`, `MedicationResponseDto`, `DocumentDto`, `DocumentResponseDto`, `DocumentUploadResponseDto`, `VaccineCreateDto`, `VaccineDto` e `VaccineResponseDto`.
    - Validações estritas com Jakarta Validation: `@NotBlank`, `@Size`, `@Email`, `@Pattern` para validação de formato de CPF (`^\d{3}\.\d{3}\.\d{3}-\d{2}$|^\d{11}$`), etc.
    - Métodos declarativos de conversão bidirecional entre DTOs e entidades (`toEntity()`).
- **Módulo Clínico (`modules/clinical`)**:
  - Implementação das entidades JPA centrais: `Appointment`, `DoctorSchedule`, `TimeSlot`, `MedicalRecord`, `Prescription`, `ExamRequest` e `MedicalCertificate`.
  - Implementação de entidades de auditoria/histórico para rastreabilidade de alterações clínicas: `MedicalRecordHistory`, `PrescriptionHistory`, `ExamRequestHistory` e `MedicalCertificateHistory`.
  - Implementação dos enums: `AppointmentStatus`, `AppointmentType` e `ExamUrgency`.
  - Mapeamento de relacionamentos entre agendamentos, horários de médicos e prontuários com chaves estrangeiras e integridade referencial.
  - Criação dos repositórios Spring Data JPA: `AppointmentRepository`, `DoctorScheduleRepository`, `TimeSlotRepository`, `MedicalRecordRepository`, `MedicalRecordHistoryRepository`, `PrescriptionRepository`, `PrescriptionHistoryRepository`, `ExamRequestRepository`, `ExamRequestHistoryRepository`, `MedicalCertificateRepository` e `MedicalCertificateHistoryRepository`.
  - **Camada de DTOs e Validações Jakarta (`modules/clinical/dto`)**:
    - Criação de 18 DTOs com Java Records para fluxo de consultas, prontuários, prescrições e exames: `AppointmentCreateDto`, `AppointmentCancelDto`, `AppointmentResponseDto`, `AppointmentListResponseDto`, `MedicalRecordCreateDto`, `MedicalRecordUpdateDto`, `MedicalRecordResponseDto`, `MedicalRecordHistoryResponseDto`, `PrescriptionCreateDto`, `PrescriptionResponseDto`, `MedicationItemDto`, `ExamRequestCreateDto`, `ExamRequestResponseDto`, `ExamResultUpdateDto`, `MedicalCertificateCreateDto`, `MedicalCertificateResponseDto`, `ScheduleCreateDto` e `TimeSlotDto`.
    - Validações de integridade com Jakarta Validation: campos obrigatórios (`@NotBlank`, `@NotNull`), limites de caracteres (`@Size`) e regras de agendamento.
    - Métodos `toEntity()` integrados para instanciação segura do modelo de domínio.
- **Refatoração dos Construtores das Entidades**:
  - Ajuste de visibilidade dos construtores padrão de `protected` para `public` nas 11 entidades de domínio (`Appointment`, `DoctorSchedule`, `ExamRequest`, `MedicalCertificate`, `MedicalRecord`, `Prescription`, `TimeSlot`, `Allergy`, `ContinuousMedication`, `Patient`, `Vaccine`), viabilizando a conversão via métodos `toEntity()` dos DTOs.
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

---

### ❌ O que deu Erro

- A dependência `org.testcontainers:redis` não possui módulo publicado compatível no BOM utilizado, resultando em erro de versão ausente durante a resolução do Maven. Foi substituída por `org.testcontainers:testcontainers`, que permite o uso do Redis com `GenericContainer` nos testes.
- O Maven fornecido pela IDE foi iniciado com Java 17, o que causou o erro `release version 21 not supported`. A compilação foi reexecutada com `JAVA_HOME` apontando para o Corretto Java 21 e finalizada com sucesso.
- A primeira execução do teste de integração usou Testcontainers 1.20.2, cujo cliente Docker API 1.32 foi recusado pelo daemon local, que exige API 1.40 ou superior. A versão foi atualizada para 1.21.4, compatível com engines Docker recentes, e a suíte passou sem erros.
- O teste inicial do custo BCrypt usava o método inexistente `getStrength()` nesta versão de Spring Security. A verificação foi corrigida para validar o prefixo do hash BCrypt (`$2…$12$`), sem impacto na implementação de produção.

---

### 🟢 O que Funcionou

- Modelagem completa e rica do modelo relacional em JPA/Hibernate cobrindo todas as regras de domínio legadas de pacientes e prontuários.
- Definição de histórico e auditoria imutável para prontuários, receitas, pedidos de exames e atestados médicos.
- Repositórios Spring Data JPA prontos com consultas especializadas para atender a camada de serviço e API REST.
- Criação de 36 DTOs imutáveis utilizando records do Java 21, garantindo validação de entrada declarativa com Jakarta Validation e desacoplamento das entidades de persistência.
- Conversão limpa entre DTOs e entidades mantendo integridade dos dados e regras de negócio.
- Resolução das dependências e compilação limpa do `iam-service` com `mvn clean compile`, Java 21 e Spring Boot 3.3.4.
- Carregamento das configurações de PostgreSQL, Redis, RabbitMQ e JWT a partir de variáveis de ambiente com valores padrão seguros para desenvolvimento local.
- Teste de integração da persistência concluído com 1 teste executado, 0 falhas e 0 erros; Flyway, enum nativo PostgreSQL, Hibernate e os quatro repositórios foram validados em conjunto.
- Testes dos contratos dos DTOs concluídos com 2 casos executados, 0 falhas e 0 erros, confirmando as validações e a paridade dos nomes JSON dos tokens.
- Testes de segurança concluídos com 7 casos executados, 0 falhas e 0 erros, cobrindo emissão e validação de JWT, rejeição de assinatura alterada, autoridade por papel, revogação no Redis e BCrypt com custo 12.

---

### 📝 Resumo do Dia

Finalização da camada de persistência, dos DTOs e da segurança do `patient-clinical-service` e do `iam-service` Java. Foram concluídas as Fases 1, 3, 4 e 5 do IAM, incluindo persistência JPA, DTOs validados, autenticação JWT stateless, revogação no Redis e testes de integração. O serviço de pacientes passou a cobrir o bounded context de pacientes, consultas, prontuários, agendamentos e prescrições, preparando os módulos para as próximas etapas de serviços e endpoints REST.

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
---

## 2026-11-05 — Fase 5: Serviços de Domínio e Transacionalidade de Pacientes

### Implementação Proposta

- Implementar os serviços de domínio do módulo Patient com transações ACID, regras de negócio, auditoria, eventos de domínio e anonimização conforme a LGPD.

### O que foi Implementado

- Criados `PatientService`, `AllergyService`, `VaccineService`, `MedicationService` e `PatientDocumentService`.
- Implementados gerenciamento de pacientes, alergias, vacinas, doses subsequentes, medicações contínuas e exclusão lógica de documentos.
- Registrado `MedicationHistory` imutável com snapshot JSON do estado anterior.
- Criado `AuditService` com `Propagation.MANDATORY` e adicionados eventos de paciente e alergia.
- Adicionadas as exceções `ResourceNotFoundException`, `ConflictException` e `BusinessException`.
- Alterações registradas nos commits `70c1a4e` e `7ccbc61` da branch `feature/modulo-patient`.

### O que deu Erro

- O Maven não estava no PATH e o Java padrão era a versão 8; foram utilizados o JDK 21 do Eclipse Adoptium e o Maven 3.9.16 já disponíveis localmente.

### O que Funcionou

- Compilação do `patient-clinical-service` concluída com sucesso usando JDK 21 e Maven 3.9.16 com `-DskipTests compile`.
- Todas as operações de escrita foram configuradas com `@Transactional(rollbackFor = Exception.class)`.

### Resumo do Dia

Fase 5 concluída com serviços de domínio, transacionalidade, auditoria mandatória, eventos, histórico de medicações, anonimização LGPD e exclusão lógica de documentos.

