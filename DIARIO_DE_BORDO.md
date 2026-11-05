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
- Inicialização da Fase 1 do microsserviço de identidade e acesso em Java (`/backend-java/iam-service`), incluindo dependências Maven, configurações base e infraestrutura de integração.
- Implementação da Fase 3 do IAM Java: entidades JPA, enum de papéis, repositórios Spring Data e teste de integração com PostgreSQL 15.
- Implementação da Fase 4 do IAM Java: DTOs imutáveis com Java Records, validações Jakarta e compatibilidade dos contratos JSON com o frontend.
- Implementação da Fase 5 do IAM Java: JWT HS256, BCrypt, blacklist de tokens no Redis e configuração stateless do Spring Security.
- Implementação da Fase 6 do IAM Java: serviços transacionais de autenticação e usuários, revogação de tokens e publicação de eventos de domínio no RabbitMQ.
- Implementação da Fase 7 do IAM Java: autenticação social Google OAuth2/OpenID Connect e autenticação em dois fatores via TOTP.
- Implementação da Fase 8 do IAM Java: controladores REST canônicos, regras de autorização e respostas globais de erro.
- Implementação da Fase 9 do IAM Java: containerização multi-stage, integração no Docker Compose e testes ponta a ponta com Testcontainers.

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

---

### 📝 Resumo do Dia

Finalização da camada de persistência e entidades de domínio do `patient-clinical-service` em Java. Foram criadas mais de 20 classes de modelo (incluindo entidades de negócio, enums e tabelas de histórico) e mais de 15 interfaces de repositório Spring Data JPA, cobrindo todo o bounded context de pacientes, consultas, prontuários, agendamentos e prescrições. No IAM Java, foram concluídas as Fases 1, 3, 4, 5, 6, 7, 8 e 9, incorporando serviços transacionais, eventos RabbitMQ, OAuth2, TOTP, endpoints REST, erros HTTP padronizados, Docker e testes ponta a ponta. Permanece a pendência da Fase 6 de encaminhar com segurança o token de recuperação de senha. A construção integral da imagem Docker deve ser repetida em ambiente com tempo suficiente para o download inicial das dependências Maven.

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
