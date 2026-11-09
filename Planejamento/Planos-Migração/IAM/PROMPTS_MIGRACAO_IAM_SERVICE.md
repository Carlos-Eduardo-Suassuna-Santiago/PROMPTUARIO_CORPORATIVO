# Guia de Prompts de Implementação: Migração do IAM Service (FastAPI -> Spring Boot 3.x / Java 21)

Este documento contém o conjunto completo de **prompts técnicos estruturados e detalhados**, organizados em **9 fases sequenciais**, para guiar a implementação assistida por IA da migração do **IAM Service** (Identity and Access Management) de Python (FastAPI / SQLAlchemy / Redis / python-jose / passlib) para a stack corporativa padronizada: **Java 21 (LTS), Spring Boot 3.3+, Spring Data JPA / Hibernate 6, Spring Security 6, Spring Data Redis e Spring AMQP (RabbitMQ)**.

O conteúdo foi desenhado com base nas especificações de [PLANO_MIGRACAO_IAM_SERVICE.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/Planos-Migra%C3%A7%C3%A3o/IAM/PLANO_MIGRACAO_IAM_SERVICE.md) e nos contratos de autenticação vigentes do PROMPTUÁRIO.

---

## Índice das Fases de Implementação

1. [Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base](#fase-1-bootstrap-do-projeto-dependências-maven-e-configurações-base)
2. [Fase 2: Migração Flyway e Schema do Banco de Dados (`iam_db`) com Seed Admin](#fase-2-migração-flyway-e-schema-do-banco-de-dados-iam_db-com-seed-admin)
3. [Fase 3: Entidades JPA e Repositórios Spring Data (Users, Tokens e OAuth)](#fase-3-entidades-jpa-e-repositórios-spring-data-users-tokens-e-oauth)
4. [Fase 4: Java Records (DTOs) e Validações de Contrato Jakarta](#fase-4-java-records-dtos-e-validações-de-contrato-jakarta)
5. [Fase 5: Criptografia de Senhas, Emissão de JWT e Blacklist no Redis](#fase-5-criptografia-de-senhas-emissão-de-jwt-e-blacklist-no-redis)
6. [Fase 6: Camada de Serviços de Negócio (AuthService, UserService) e Eventos RabbitMQ](#fase-6-camada-de-serviços-de-negócio-authservice-userservice-e-eventos-rabbitmq)
7. [Fase 7: Provedor Social Google OAuth2 e Autenticação de Dois Fatores (TOTP)](#fase-7-provedor-social-google-oauth2-e-autenticação-de-dois-fatores-totp)
8. [Fase 8: Controladores REST, Endpoints Canônicos e Tratamento Global de Erros](#fase-8-controladores-rest-endpoints-canônicos-e-tratamento-global-de-erros)
9. [Fase 9: Dockerfile Multi-Stage, Docker Compose e Testes Automatizados (Testcontainers)](#fase-9-dockerfile-multi-stage-docker-compose-e-testes-automatizados-testcontainers)

---

## Diretrizes Gerais para o Ciclo de Execução

- **Compatibilidade Criptográfica Estrita:**
  * **Hash de Senha:** BCrypt com fator de custo 12 (`BCryptPasswordEncoder(12)`), garantindo que usuários cadastrados no banco legado continuem autenticando normalmente sem reset forçado de credenciais.
  * **Tokens JWT:** HMAC-SHA256 utilizando a mesma chave simétrica compartilhada (`JWT_SECRET_KEY`) e o mesmo padrão de claims (`sub`, `role`, `email`, `type`, `iat`, `exp`).
- **Revogação Instantânea no Redis:**
  * O logout adiciona a chave `blacklist:<token>` com TTL dinâmico equivalente ao tempo restante para expiração do token.
  * O `JwtAuthenticationFilter` deve consultar o Redis (`StringRedisTemplate`) antes de autenticar a requisição.
- **Autoridade de Contas e Eventos de Domínio:**
  * O IAM Service é a fonte da verdade de identidades. Ao cadastrar um paciente (`register-patient`) ou desativar uma conta (`deactivate`), ele deve publicar obrigatoriamente os eventos `UserCreatedEvent` e `UserDeactivatedEvent` no RabbitMQ na exchange `iam.events` (ou `promptuario.iam`).

---

## Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base

### Objetivo
Inicializar a estrutura do projeto `backend-java/iam-service` com Java 21 e Spring Boot 3.3+, configurando o `pom.xml`, o arquivo `application.yml` tipado com suporte a profiles (`dev`, `prod`, `test`), a conexão com PostgreSQL `iam_db`, o cliente Redis e a infraestrutura do Spring Boot Actuator.

### Arquivos Alvo
- `backend-java/iam-service/pom.xml`
- `backend-java/iam-service/src/main/resources/application.yml`
- `backend-java/iam-service/src/main/java/br/edu/ifrn/promptuario/iam/IamServiceApplication.java`
- `backend-java/iam-service/src/main/java/br/edu/ifrn/promptuario/iam/config/JwtProperties.java`
- `backend-java/iam-service/src/main/java/br/edu/ifrn/promptuario/iam/config/RedisConfig.java`

### Prompt da Fase 1

```text
Atue como Engenheiro de Software Sênior especialista em Java 21 e Spring Boot 3.3+.
Vamos criar a base do novo microsserviço "iam-service" em "backend-java/iam-service/".

Gere os arquivos estruturais iniciais do projeto:

1. "pom.xml":
   - Java 21, Spring Boot versão 3.3.4 (ou superior compatível).
   - GroupId: "br.edu.ifrn.promptuario"
   - ArtifactId: "iam-service"
   - Dependências obrigatórias:
     * spring-boot-starter-web
     * spring-boot-starter-data-jpa
     * org.postgresql:postgresql (runtime)
     * org.flywaydb:flyway-core e org.flywaydb:flyway-database-postgresql
     * spring-boot-starter-data-redis
     * spring-boot-starter-security
     * com.nimbusds:nimbus-jose-jwt:9.37.3 (ou java-jwt para geração/validação de tokens)
     * spring-boot-starter-validation
     * spring-boot-starter-amqp (RabbitMQ)
     * spring-boot-starter-actuator
     * io.micrometer:micrometer-registry-prometheus
     * org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0
     * com.warrenstrange:googleauth:1.5.0 (para 2FA / TOTP)
     * org.projectlombok:lombok (scope provided)
     * spring-boot-starter-test, testcontainers (postgresql, redis, rabbitmq) em escopo test.

2. "src/main/resources/application.yml":
   - Porta interna do container: 8000 (mapeada no host como 8001:8000).
   - Datasource PostgreSQL para o banco "iam_db":
     * url: "jdbc:postgresql://${DB_IAM_HOST:localhost}:5432/iam_db"
     * username: "${DB_IAM_USER:iam}"
     * password: "${DB_IAM_PASS:iam_pass}"
   - JPA / Hibernate com dialect PostgreSQL, "ddl-auto: validate", "open-in-view: false".
   - Flyway ativado em "classpath:db/migration".
   - Redis:
     * host: "${REDIS_HOST:localhost}"
     * port: 6379
     * database: 0
   - RabbitMQ:
     * host: "${RABBITMQ_HOST:localhost}"
     * port: 5672
     * exchange: "iam.events" (ou "promptuario.iam")
   - Propriedades de segurança customizadas mapeadas em "app.security.jwt":
     * secret-key: "${JWT_SECRET_KEY:change-me-in-production-super-secret-key-min-32-chars}"
     * algorithm: "HS256"
     * access-token-expiration-minutes: 30
     * refresh-token-expiration-days: 7
   - Actuator: expondo "health,info,prometheus" sob "/actuator".

3. "IamServiceApplication.java":
   - Classe principal com anotação @SpringBootApplication e @EnableScheduling.

4. "JwtProperties.java" e "RedisConfig.java" (pacote "br.edu.ifrn.promptuario.iam.config"):
   - "JwtProperties": @ConfigurationProperties(prefix = "app.security.jwt") para injeção tipada de expiração e segredos.
   - "RedisConfig": Bean "StringRedisTemplate" configurado para escrita e leitura de strings simples e controle de TTL.

Valide se a compilação "mvn clean compile" ocorre sem falhas.
```

### Critérios de Aceite
- Compilação do projeto sem erros via `mvn clean compile`.
- Carregamento de propriedades de banco, Redis e RabbitMQ a partir de variáveis de ambiente com defaults seguros para desenvolvimento local.

---

## Fase 2: Migração Flyway e Schema do Banco de Dados (`iam_db`) com Seed Admin

### Objetivo
Criar o script de migração Flyway `V1__init_iam_schema.sql` refletindo fielmente a estrutura relacional do PostgreSQL `iam_db`, com tabelas `users`, `refresh_tokens`, `password_reset_tokens`, `oauth_accounts` e a inserção idempotente do usuário administrador padrão (`admin@promptuario.health`).

### Arquivos Alvo
- `backend-java/iam-service/src/main/resources/db/migration/V1__init_iam_schema.sql`

### Prompt da Fase 2

```text
Atue como DBA e Engenheiro de Banco de Dados especializado em PostgreSQL 15 e Flyway.
Precisamos criar o script DDL canônico unificado para o microsserviço "iam-service" em "backend-java/iam-service/src/main/resources/db/migration/V1__init_iam_schema.sql".

O script deve conter:

1. Enum nativo ou constraints para papéis de usuário:
   - "user_role": ('ADMIN', 'DOCTOR', 'ATTENDANT', 'PATIENT')

2. Tabela "users":
   - id: VARCHAR(36) (UUID em formato texto padrão) PRIMARY KEY.
   - email: VARCHAR(255) NOT NULL UNIQUE.
   - cpf: VARCHAR(14) UNIQUE NULL.
   - hashed_password: VARCHAR(255) NOT NULL.
   - full_name: VARCHAR(255) NOT NULL.
   - role: VARCHAR(50) NOT NULL.
   - is_active: BOOLEAN NOT NULL DEFAULT TRUE.
   - is_verified: BOOLEAN NOT NULL DEFAULT FALSE.
   - two_factor_enabled: BOOLEAN NOT NULL DEFAULT FALSE.
   - two_factor_secret: VARCHAR(255) NULL.
   - deactivated_at: TIMESTAMPTZ NULL.
   - deactivation_reason: VARCHAR(255) NULL.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.
   - updated_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.

3. Tabela "refresh_tokens":
   - id: VARCHAR(36) PRIMARY KEY.
   - user_id: VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE.
   - token_hash: VARCHAR(128) NOT NULL UNIQUE.
   - expires_at: TIMESTAMPTZ NOT NULL.
   - revoked: BOOLEAN NOT NULL DEFAULT FALSE.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.

4. Tabela "password_reset_tokens":
   - id: VARCHAR(36) PRIMARY KEY.
   - user_id: VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE.
   - token_hash: VARCHAR(128) NOT NULL UNIQUE.
   - expires_at: TIMESTAMPTZ NOT NULL.
   - used: BOOLEAN NOT NULL DEFAULT FALSE.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.

5. Tabela "oauth_accounts":
   - id: VARCHAR(36) PRIMARY KEY.
   - user_id: VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE.
   - provider: VARCHAR(50) NOT NULL.
   - provider_user_id: VARCHAR(255) NOT NULL.
   - provider_email: VARCHAR(255) NULL.
   - created_at: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP.
   - CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id).

6. Índices de Otimização:
   - Índices em "users(email)", "users(cpf)", "users(role)", "users(is_active)".
   - Índices em "refresh_tokens(user_id)", "refresh_tokens(token_hash)".
   - Índices em "password_reset_tokens(token_hash)".
   - Índices em "oauth_accounts(user_id)".

7. Seed Inicial do Administrador (Idempotente):
   - Inserir o administrador padrão se não existir:
     * id: '00000000-0000-0000-0000-000000000001'
     * email: 'admin@promptuario.health'
     * full_name: 'Administrador do Sistema'
     * role: 'ADMIN'
     * hashed_password: hash BCrypt correspondente à senha inicial padrão ('admin123' ou '$2b$12$...')
     * is_active: true
     * is_verified: true

Forneça o arquivo SQL completo, compatível com PostgreSQL 15, garantindo que seja executado perfeitamente pelo Flyway no startup.
```

### Critérios de Aceite
- Execução limpa do script pelo Flyway na inicialização do serviço.
- Garantia de constraints de unicidade (`email`, `cpf`, `token_hash` e `(provider, provider_user_id)`).

---

## Fase 3: Entidades JPA e Repositórios Spring Data (Users, Tokens e OAuth)

### Objetivo
Mapear as entidades de persistência JPA no pacote `model/` com Hibernate 6 e implementar os repositórios Spring Data JPA (`UserRepository`, `RefreshTokenRepository`, `PasswordResetRepository`, `OAuthAccountRepository`), provendo consultas otimizadas e auditoria automática de timestamps.

### Arquivos Alvo
- `model/User.java`
- `model/Role.java` (Enum)
- `model/RefreshToken.java`
- `model/PasswordResetToken.java`
- `model/OAuthAccount.java`
- `repository/UserRepository.java`
- `repository/RefreshTokenRepository.java`
- `repository/PasswordResetRepository.java`
- `repository/OAuthAccountRepository.java`

### Prompt da Fase 3

```text
Atue como Desenvolvedor Java Especialista em Spring Data JPA e Hibernate 6.
Vamos implementar as entidades de banco e repositórios para o "iam-service" no pacote "br.edu.ifrn.promptuario.iam".

1. Enums e Entidades JPA no pacote "model":
   - "Role.java" (Enum): ADMIN, DOCTOR, ATTENDANT, PATIENT.
   - "User.java" (@Entity, @Table(name = "users")):
     * id: String (UUID v4 gerado no Java).
     * email: String (unique, lowercase).
     * cpf: String (nullable, unique).
     * hashedPassword: String.
     * fullName: String.
     * role: Role (@Enumerated(EnumType.STRING)).
     * isActive, isVerified, twoFactorEnabled: boolean.
     * twoFactorSecret: String (nullable).
     * deactivatedAt: Instant, deactivationReason: String.
     * createdAt: Instant (@CreationTimestamp), updatedAt: Instant (@UpdateTimestamp).
     * Relacionamentos: @OneToMany cascade ALL para RefreshToken, PasswordResetToken e OAuthAccount.
   - "RefreshToken.java" (@Entity, @Table(name = "refresh_tokens")):
     * id: String (UUID).
     * @ManyToOne com User (lazy).
     * tokenHash: String (SHA-256 do token para busca rápida).
     * expiresAt: Instant, revoked: boolean, createdAt: Instant.
   - "PasswordResetToken.java" (@Entity, @Table(name = "password_reset_tokens")):
     * id, user, tokenHash, expiresAt, used: boolean, createdAt.
   - "OAuthAccount.java" (@Entity, @Table(name = "oauth_accounts")):
     * id, user, provider ("google", "github"), providerUserId, providerEmail, createdAt.

2. Repositórios Spring Data JPA no pacote "repository":
   - "UserRepository":
     * Optional<User> findByEmailIgnoreCase(String email);
     * Optional<User> findByCpf(String cpf);
     * boolean existsByEmailIgnoreCase(String email);
     * boolean existsByCpf(String cpf);
     * Page<User> findByRoleAndIsActive(Role role, Boolean isActive, Pageable pageable);
     * List<User> findByRoleAndIsActiveTrue(Role role); // Para listar médicos disponíveis
     * Page<User> findAll(Pageable pageable);
   - "RefreshTokenRepository":
     * Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
     * @Modifying @Query para revogar todos os refresh tokens de um usuário no logout global.
   - "PasswordResetRepository":
     * Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
   - "OAuthAccountRepository":
     * Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
     * List<OAuthAccount> findByUserId(String userId);

Gere código com imports Jakarta (jakarta.persistence.*), anotações adequadas e construtores necessários.
```

### Critérios de Aceite
- Compilação correta e integração de repositórios validada com `JpaRepository`.
- Relacionamentos mapeados sem problemas de lazy-loading ou loop de serialização.

---

## Fase 4: Java Records (DTOs) e Validações de Contrato Jakarta

### Objetivo
Implementar todos os DTOs como **Java Records imutáveis**, reproduzindo com fidelidade os schemas Pydantic de entrada e saída, decorados com validações Jakarta (`@NotBlank`, `@Email`, `@Size`, `@Pattern` para CPF) e mensagens de erro padronizadas.

### Arquivos Alvo
- `dto/LoginRequestDTO.java`
- `dto/RefreshRequestDTO.java`
- `dto/TokenResponseDTO.java`
- `dto/ChangePasswordDTO.java`
- `dto/ForgotPasswordDTO.java`
- `dto/ResetPasswordDTO.java`
- `dto/PatientRegisterDTO.java`
- `dto/UserCreateDTO.java`
- `dto/UserUpdateDTO.java`
- `dto/AssignRoleDTO.java`
- `dto/DeactivateUserDTO.java`
- `dto/UserResponseDTO.java`
- `dto/UserListResponseDTO.java`
- `dto/DoctorResponseDTO.java`
- `dto/DoctorListResponseDTO.java`

### Prompt da Fase 4

```text
Atue como Engenheiro de Software especialista em API Design e Java 21.
Vamos criar todos os Data Transfer Objects (DTOs) como Java Records imutáveis para o "iam-service" em "br.edu.ifrn.promptuario.iam.dto".

Mapeie os seguintes Records com validações Jakarta (jakarta.validation.constraints.*):

1. Autenticação e Credenciais:
   - "LoginRequestDTO": (
       @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
       @NotBlank(message = "Senha é obrigatória") String password,
       String totpCode // opcional para usuários com 2FA ativo
     )
   - "RefreshRequestDTO": (@NotBlank(message = "Refresh token é obrigatório") String refreshToken)
   - "TokenResponseDTO": (
       String accessToken,
       String refreshToken,
       String tokenType, // "bearer"
       long expiresIn,   // segundos
       UserResponseDTO user
     )
   - "ChangePasswordDTO": (
       @NotBlank String currentPassword,
       @NotBlank @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres") String newPassword
     )
   - "ForgotPasswordDTO": (@NotBlank @Email String email)
   - "ResetPasswordDTO": (@NotBlank String token, @NotBlank @Size(min = 8) String newPassword)

2. Gestão de Usuários:
   - "PatientRegisterDTO": (
       @NotBlank @Email String email,
       @NotBlank @Size(min = 8) String password,
       @NotBlank String fullName,
       @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{11}$", message = "CPF inválido") String cpf,
       LocalDate dateOfBirth,
       String gender,
       String phone
     )
   - "UserCreateDTO": (
       @NotBlank @Email String email,
       @NotBlank @Size(min = 8) String password,
       @NotBlank String fullName,
       @NotNull Role role,
       String cpf
     )
   - "UserUpdateDTO": (String fullName, @Email String email, String cpf)
   - "AssignRoleDTO": (@NotNull Role role)
   - "DeactivateUserDTO": (@NotBlank String reason)
   - "UserResponseDTO": (
       String id,
       String email,
       String fullName,
       Role role,
       String cpf,
       boolean isActive,
       boolean isVerified,
       boolean twoFactorEnabled,
       Instant createdAt
     ) onde métodos utilitários estáticos "fromEntity(User user)" convertem a entidade para DTO.
   - "UserListResponseDTO": (List<UserResponseDTO> items, long total, int page, int size)
   - "DoctorResponseDTO": (String id, String fullName, String email, String crm, String specialty)
   - "DoctorListResponseDTO": (List<DoctorResponseDTO> items, int total)

Assegure que os nomes de atributos JSON (via anotações Jackson @JsonProperty se necessário, como snake_case para access_token e refresh_token) preservem 100% de paridade com as respostas do FastAPI.
```

### Critérios de Aceite
- Nomenclatura dos campos JSON compatível com o Frontend React (`access_token`, `refresh_token`, `token_type`, `expires_in`).
- Validação automática de payloads anotados com `@Valid` nos controladores.

---

## Fase 5: Criptografia de Senhas, Emissão de JWT e Blacklist no Redis

### Objetivo
Implementar a infraestrutura de segurança com Spring Security 6: o componente de geração e validação de JWTs simétricos (`JwtTokenProvider` compatível com o algoritmo HMAC-SHA256), o gerenciador de revogação de tokens no Redis com TTL (`TokenBlacklistService`), o filtro de requisições `JwtAuthenticationFilter` e o bean `BCryptPasswordEncoder(12)`.

### Arquivos Alvo
- `security/JwtTokenProvider.java`
- `security/JwtAuthenticationFilter.java`
- `service/TokenBlacklistService.java`
- `config/SecurityConfig.java`

### Prompt da Fase 5

```text
Atue como Especialista em Segurança da Informação e Spring Security 6.
Vamos implementar a camada de criptografia, geração de tokens JWT e controle de blacklist no Redis para o "iam-service".

1. "JwtTokenProvider.java" em "br.edu.ifrn.promptuario.iam.security":
   - Utiliza a chave simétrica configurada em "app.security.jwt.secret-key" (mínimo 256 bits).
   - "generateAccessToken(User user)":
     * Claims obrigatórias:
       - "sub": user.getId()
       - "role": user.getRole().name()
       - "email": user.getEmail()
       - "type": "access"
       - "iat": timestamp atual em segundos
       - "exp": timestamp de expiração (30 minutos)
   - "generateRefreshToken(User user)":
     * Claims:
       - "sub": user.getId()
       - "type": "refresh"
       - "jti": UUID aleatório único
       - "iat", "exp" (7 dias)
   - "validateToken(String token)": verifica assinatura e validade temporal.
   - "extractUserId(String token)", "extractRole(String token)", "extractExpiration(String token)".

2. "TokenBlacklistService.java" em "br.edu.ifrn.promptuario.iam.service":
   - Injeta "StringRedisTemplate".
   - "blacklistToken(String token)":
     * Extrai a data de expiração do token.
     * Calcula o tempo restante: TTL = expiração - agora.
     * Se TTL > 0, grava no Redis: chave "blacklist:" + token, valor "revoked", com expiração TTL.
   - "isBlacklisted(String token)": retorna true se a chave "blacklist:" + token existir no Redis.

3. "JwtAuthenticationFilter.java" (estende OncePerRequestFilter):
   - Extrai o token do header "Authorization: Bearer <token>".
   - Verifica se o token está na blacklist via TokenBlacklistService: se estiver, rejeita imediatamente.
   - Valida o token e configura o SecurityContextHolder com UsernamePasswordAuthenticationToken contendo o usuário e as GrantedAuthorities baseadas na role ("ROLE_" + role).

4. "SecurityConfig.java" em "br.edu.ifrn.promptuario.iam.config":
   - Bean "PasswordEncoder": BCryptPasswordEncoder(12).
   - "SecurityFilterChain":
     * CSRF desabilitado.
     * SessionCreationPolicy.STATELESS.
     * Endpoints públicos (permitAll):
       - "/healthz", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"
       - "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/register-patient", "/api/v1/auth/oauth/**"
     * Todos os demais requerem autenticação.
     * Adicionar "JwtAuthenticationFilter" antes de UsernamePasswordAuthenticationFilter.

Forneça o código completo e garanta que tokens gerados pelo Java possam ser decodificados pelos outros microsserviços do PROMPTUÁRIO usando a mesma secret key.
```

### Critérios de Aceite
- Compatibilidade mútua de assinatura JWT com o `shared.utils.security` do ecossistema Python.
- Tokens revogados no logout impedidos de realizar novas requisições em menos de 1ms via consulta O(1) no Redis.

---

## Fase 6: Camada de Serviços de Negócio (AuthService, UserService) e Eventos RabbitMQ

### Objetivo
Implementar a lógica de negócios de autenticação (`AuthService`) e gestão de usuários (`UserService`), com transações ACID (`@Transactional`), validação de unicidade de e-mail/CPF, revogação de tokens e publicação dos eventos de domínio `UserCreatedEvent` e `UserDeactivatedEvent` no RabbitMQ via `IamEventPublisher`.

### Arquivos Alvo
- `messaging/IamEventPublisher.java`
- `messaging/events/UserCreatedEvent.java`
- `messaging/events/UserDeactivatedEvent.java`
- `service/AuthService.java`
- `service/UserService.java`

### Prompt da Fase 6

```text
Atue como Engenheiro de Backend especializado em Lógica de Domínio e Arquitetura Orientada a Eventos (EDA).
Vamos construir os serviços de aplicação e a mensageria do "iam-service" em "br.edu.ifrn.promptuario.iam".

1. Records de Eventos e Publicador RabbitMQ (pacote "messaging"):
   - "UserCreatedEvent": (String userId, String email, String fullName, String role, String cpf, Instant createdAt).
   - "UserDeactivatedEvent": (String userId, String reason, String deactivatedBy, Instant deactivatedAt).
   - "IamEventPublisher":
     * Injeta RabbitTemplate.
     * Publica na exchange "iam.events" com as routing keys:
       - "user.created"
       - "user.deactivated"

2. "AuthService.java" (pacote "service"):
   - "login(LoginRequestDTO dto)":
     * Busca usuário por e-mail (case-insensitive).
     * Valida senha com PasswordEncoder.matches().
     * Se usuário estiver inativo, lança ForbiddenException("Usuário inativo").
     * Se 2FA estiver ativado, valida código TOTP.
     * Gera Access Token e Refresh Token.
     * Salva o hash do Refresh Token na tabela "refresh_tokens".
     * Retorna "TokenResponseDTO".
   - "refresh(RefreshRequestDTO dto)":
     * Valida o refresh token no banco (não expirado e não revogado).
     * Emite novo par de tokens (rotação de refresh token).
     * Revoga o refresh token antigo.
   - "logout(String refreshToken, String rawAccessToken)":
     * Adiciona o access token na blacklist do Redis via TokenBlacklistService.
     * Marca o refresh token como revoked = true no banco.
   - "changePassword(String userId, ChangePasswordDTO dto)":
     * Valida senha atual e atualiza para a nova senha criptografada.
   - "forgotPassword(String email)":
     * Gera token seguro de recuperação com expiração de 2 horas e persiste em "password_reset_tokens".
   - "resetPassword(ResetPasswordDTO dto)":
     * Valida token de recuperação, atualiza a senha do usuário e marca o token como usado.

3. "UserService.java" (pacote "service"):
   - "createUser(UserCreateDTO dto)":
     * Valida duplicidade de e-mail e CPF.
     * Salva usuário com role especificada.
     * Publica "UserCreatedEvent" no RabbitMQ.
   - "registerPatient(PatientRegisterDTO dto)":
     * Cria usuário com role PATIENT.
     * Publica "UserCreatedEvent" (consumido pelo patient-clinical-service para criação do prontuário).
   - "deactivateUser(String userId, String reason, String adminId)":
     * Define isActive = false, deactivatedAt = Instant.now(), deactivationReason = reason.
     * Revoga todos os refresh tokens ativos do usuário.
     * Publica "UserDeactivatedEvent" (que dispara o cancelamento automático de consultas no patient-clinical-service).
   - "listUsers(...)", "getUserById(String id)", "listDoctors()".

Aplique @Transactional em todos os métodos que realizem operações de escrita no banco de dados.
```

### Critérios de Aceite
- Publicação correta dos eventos com headers de rastreabilidade no RabbitMQ.
- Desativação atômica com revogação em lote de tokens.

---

## Fase 7: Provedor Social Google OAuth2 e Autenticação de Dois Fatores (TOTP)

### Objetivo
Implementar o fluxo de autenticação social Google OAuth2 (`OAuthService` e `OAuthController`) e o suporte a 2FA/MFA via TOTP (`TwoFactorService` com biblioteca GoogleAuth), permitindo auto-provisionamento de contas sociais e login com verificação em duas etapas.

### Arquivos Alvo
- `service/TwoFactorService.java`
- `service/OAuthService.java`
- `controller/OAuthController.java`
- `config/OAuthConfig.java`

### Prompt da Fase 7

```text
Atue como Especialista em Protocolos de Autenticação OAuth2, OpenID Connect e MFA/TOTP.
Vamos implementar o suporte a Google OAuth2 e 2FA no "iam-service".

1. "TwoFactorService.java" em "br.edu.ifrn.promptuario.iam.service":
   - Utilizar biblioteca "com.warrenstrange:googleauth:GoogleAuthenticator".
   - "generateSecretKey()": gera segredo TOTP base32.
   - "getQrCodeUrl(String email, String secret)": gera URI no formato "otpauth://totp/Promptuario:{email}?secret={secret}&issuer=Promptuario".
   - "verifyCode(String secret, int code)": valida o código de 6 dígitos gerado pelo app autenticador.

2. "OAuthService.java" e "OAuthController.java":
   - Configurações Google OAuth lidas do "application.yml": "app.oauth2.google.client-id", "client-secret", "redirect-uri".
   - Endpoint GET "/api/v1/auth/oauth/google":
     * Redireciona para o consent screen do Google ("https://accounts.google.com/o/oauth2/v2/auth") solicitando scopes "openid email profile".
   - Endpoint GET "/api/v1/auth/oauth/google/callback":
     * Recebe "code" e "state".
     * Troca o código pelo id_token e access_token junto ao Google.
     * Extrai dados do usuário (email, full_name, google_user_id).
     * Provisionamento automático de conta:
       - Se usuário já existir pelo e-mail, vincula a conta em "oauth_accounts".
       - Se for novo usuário, cria registro com role padrão PATIENT e vincula OAuthAccount.
     * Emite os tokens JWT do sistema (access e refresh).
     * Redireciona o navegador para o frontend:
       "${app.frontend-callback-url}?access_token={token}&refresh_token={refresh}".
   - Endpoint GET "/api/v1/auth/oauth/accounts":
     * Lista provedores vinculados à conta do usuário autenticado.

Forneça código robusto com tratamento de erros de autorização e fallback seguro.
```

### Critérios de Aceite
- Fluxo de login social funcional integrando com endpoints públicos do Google OAuth2.
- Validação precisa de códigos TOTP sincronizados por tempo.

---

## Fase 8: Controladores REST, Endpoints Canônicos e Tratamento Global de Erros

### Objetivo
Implementar os controladores REST (`AuthController`, `UserController`) mapeando 100% dos endpoints definidos no contrato original do FastAPI, aplicando autorizações de role (`@PreAuthorize`) e construindo o `@RestControllerAdvice` para formatação padronizada de erros HTTP 400, 401, 403, 404 e 409.

### Arquivos Alvo
- `controller/AuthController.java`
- `controller/UserController.java`
- `exception/GlobalExceptionHandler.java`
- `exception/ApiErrorResponse.java`
- Exceções customizadas (`BadCredentialsException`, `ResourceNotFoundException`, `ConflictException`, `ForbiddenException`).

### Prompt da Fase 8

```text
Atue como Desenvolvedor Fullstack/Backend Sênior especializado em Spring Web MVC e Tratamento de Exceções.
Vamos construir os Controladores REST finais do "iam-service" mantendo absoluta compatibilidade com o Gateway e o Frontend.

1. "AuthController.java" (@RestController, @RequestMapping("/api/v1/auth")):
   - POST "/login" -> Login de usuário
   - POST "/refresh" -> Rotação de refresh token
   - POST "/logout" -> Revogação do access token e refresh token (HTTP 204)
   - POST "/change-password" -> Alteração de senha (autenticado)
   - POST "/forgot-password" -> Solicitação de recuperação de senha
   - POST "/reset-password" -> Conclusão da redefinição de senha
   - POST "/register-patient" -> Auto-cadastro público de paciente (HTTP 201)

2. "UserController.java" (@RestController, @RequestMapping("/api/v1/users")):
   - GET "" -> Listagem paginada de usuários - @PreAuthorize("hasRole('ADMIN')")
   - POST "" -> Criação de usuário - @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')") (Regra: ATTENDANT só pode criar role PATIENT)
   - GET "/me" -> Dados do usuário logado (extraído da autenticação)
   - GET "/doctors" -> Listagem pública/autenticada de médicos ativos
   - GET "/{id}" -> Detalhe do usuário - @PreAuthorize("hasRole('ADMIN')")
   - PUT "/{id}" -> Atualização cadastral (o próprio usuário ou ADMIN)
   - PUT "/{id}/role" -> Atribuição de permissão - @PreAuthorize("hasRole('ADMIN')")
   - DELETE "/{id}" -> Desativação de conta com justificativa - @PreAuthorize("hasRole('ADMIN')") (HTTP 204)
   - POST "/{id}/reactivate" -> Reativação de conta - @PreAuthorize("hasRole('ADMIN')") (HTTP 204)
   - GET "/healthz" -> Retorna { "status": "ok", "service": "iam-service" }

3. "ApiErrorResponse.java" (Record imutável):
   - Campos: Instant timestamp, int status, String error, String message, String path, Map<String, String> errors.

4. "GlobalExceptionHandler.java" (@RestControllerAdvice):
   - BadCredentialsException -> HTTP 401 ("Email ou senha inválidos").
   - ResourceNotFoundException -> HTTP 404 ("Usuário não encontrado").
   - ConflictException -> HTTP 409 ("E-mail ou CPF já cadastrado").
   - AccessDeniedException / ForbiddenException -> HTTP 403 ("Acesso negado").
   - MethodArgumentNotValidException -> HTTP 400 com mapa de campos inválidos.
   - Exception genérica -> HTTP 500 sem vazar stack trace.

Gere o código completo com OpenAPI / Swagger annotations (@Operation, @ApiResponse).
```

### Critérios de Aceite
- Mapeamento exato de todos os paths de URL documentados em `PLANEJAMENTO_DE_ENDPOINTS_PI.md`.
- Respostas de erro padronizadas e idempotentes em conformidade com o Gateway.

---

## Fase 9: Dockerfile Multi-Stage, Docker Compose e Testes Automatizados (Testcontainers)

### Objetivo
Empacotar o serviço em um `Dockerfile` multi-stage com `eclipse-temurin:21-jre-alpine`, atualizar o container no `backend/docker-compose.yml` (porta interna `8000`, host `8001:8000`) e criar a suíte de testes de integração com Testcontainers (PostgreSQL e Redis) cobrindo fluxos de ponta a ponta.

### Arquivos Alvo
- `backend-java/iam-service/Dockerfile`
- `backend/docker-compose.yml` (atualização do serviço iam-service)
- `backend-java/iam-service/src/test/java/br/edu/ifrn/promptuario/iam/AuthIntegrationTest.java`

### Prompt da Fase 9

```text
Atue como Especialista em DevOps e Engenheiro de QA em Java.
Vamos concluir a migração do "iam-service" com containerização e testes automatizados.

1. "Dockerfile" em "backend-java/iam-service/Dockerfile":
   - Multi-stage build com foco em leveza e segurança:
     * Stage 1 (Builder): "maven:3.9-eclipse-temurin-21-alpine", copia pom.xml, baixa dependências, copia código e compila JAR sem testes ("mvn clean package -DskipTests").
     * Stage 2 (Runtime): "eclipse-temurin:21-jre-alpine", cria usuário "appuser", copia "app.jar", expõe porta 8000.
     * Flags JVM: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0".
     * Health check interno: "curl -f http://localhost:8000/healthz || exit 1".
     * Entrypoint: ["java", "-jar", "app.jar"].

2. Atualização no "backend/docker-compose.yml":
   - Atualizar a entrada "iam-service":
     ```yaml
     iam-service:
       build:
         context: ../backend-java/iam-service
         dockerfile: Dockerfile
       container_name: iam-service
       restart: unless-stopped
       ports:
         - "8001:8000"
       environment:
         - SPRING_DATASOURCE_URL=jdbc:postgresql://db-iam:5432/iam_db
         - SPRING_DATASOURCE_USERNAME=iam
         - SPRING_DATASOURCE_PASSWORD=iam_pass
         - SPRING_DATA_REDIS_HOST=redis
         - SPRING_DATA_REDIS_PORT=6379
         - SPRING_RABBITMQ_HOST=rabbitmq
         - APP_SECURITY_JWT_SECRET_KEY=${JWT_SECRET_KEY}
       depends_on:
         db-iam:
           condition: service_healthy
         redis:
           condition: service_healthy
         rabbitmq:
           condition: service_healthy
       networks:
         - backend
     ```

3. "AuthIntegrationTest.java" em "src/test/java/...":
   - Teste de integração ponta a ponta usando @SpringBootTest e Testcontainers (PostgreSQL 15 e Redis 7):
     * Teste 1: Autenticar com o administrador padrão (admin@promptuario.health / admin123) e receber token JWT válido.
     * Teste 2: Cadastrar novo paciente via POST /api/v1/auth/register-patient e verificar inserção no banco.
     * Teste 3: Chamar POST /api/v1/auth/logout e validar que o token entra imediatamente na blacklist do Redis.
     * Teste 4: Tentar acessar endpoint protegido com o token deslogado e receber HTTP 401 Unauthorized.
     * Teste 5: Desativar usuário via DELETE /api/v1/users/{id} e validar bloqueio de novo login.

Forneça os arquivos completos e comandos prontos para validação.
```

### Critérios de Aceite
- Construção da imagem Docker bem-sucedida (< 250MB).
- Passagem de 100% dos testes de integração no Testcontainers sem mocks de banco/Redis.
- API Gateway autenticando requisições com o novo IAM em Java na porta 8001.

---

## Matriz de Rastreabilidade e Riscos Técnicos

| Risco Identificado | Severidade | Impacto | Medida Mitigatória nos Prompts |
| :--- | :--- | :--- | :--- |
| **Incompatibilidade de hash de senha** | Crítica | Usuários legados não conseguem logar | A Fase 5 utiliza `BCryptPasswordEncoder(12)`, 100% compatível com as senhas existentes salvas pelo passlib. |
| **Divergência de algoritmo JWT** | Crítica | Gateway e outros microsserviços rejeitam tokens | A Fase 1 e 5 padronizam o algoritmo HMAC-SHA256 (`HS256`) com a variável compartilhada `JWT_SECRET_KEY`. |
| **Falha de expiração na Blacklist Redis** | Alta | Acúmulo descontrolado de memória no Redis | A Fase 5 calcula dinamicamente o TTL da chave Redis com base no tempo restante para expiração do token. |
| **Perda de criação de perfil de paciente** | Crítica | Paciente criado no IAM sem prontuário clínico | A Fase 6 publica obrigatoriamente `UserCreatedEvent` no RabbitMQ, consumido pelo `patient-clinical-service`. |

---

## Comandos Rápidos de Validação

```bash
# 1. Compilar e rodar testes do IAM Service:
cd backend-java/iam-service
mvn clean test

# 2. Subir o serviço no Docker Compose:
cd ../../backend
docker compose up -d --build iam-service

# 3. Validar health check:
curl -i http://localhost:8001/healthz

# 4. Testar login do administrador padrão:
curl -s -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@promptuario.health","password":"admin"}' | jq .
```
