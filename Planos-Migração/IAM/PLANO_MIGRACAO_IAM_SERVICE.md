# Plano de Migração do IAM Service: Python (FastAPI) para Java (Spring Boot)

Este documento detalha o passo a passo completo e o mapeamento técnico necessário para migrar o **IAM Service** (Identity and Access Management) da sua stack atual em Python (FastAPI / SQLAlchemy / Redis) para a stack corporativa exigida no Projeto Integrador: **Java 21 (LTS) e Spring Boot 3.x**.

---

## 1. Mapeamento de Stack Tecnológica

Equivalência 1-para-1 de cada tecnologia que compõe o serviço atualmente:

| Componente | Atual (Python) | Destino (Java 21 / Spring Boot 3.x) |
| :--- | :--- | :--- |
| **Linguagem Base** | Python 3.12 | Java 21 (LTS) |
| **Framework Web** | FastAPI + Uvicorn | Spring Boot 3.3+ (Spring Web / MVC) |
| **Gerenciador de Dependências** | pip / requirements.txt | Maven (`pom.xml`) |
| **ORM / Banco de Dados** | SQLAlchemy 2.0 (Async) | Spring Data JPA / Hibernate 6.x |
| **Banco de Dados Relacional** | PostgreSQL 15 (`iam_db`) | PostgreSQL 15 (`iam_db`) |
| **Migrações de Banco** | Alembic | Flyway (`V1__init_iam_schema.sql`) |
| **Serialização e Validação** | Pydantic v2 | Java Records + Jakarta Validation (`@Valid`, `@NotBlank`, etc.) |
| **Segurança e Criptografia** | passlib (bcrypt) / python-jose | Spring Security 6 (`BCryptPasswordEncoder`) + Nimbus JOSE JWT |
| **Revogação de Tokens (Blacklist)** | Redis 7 (redis-py async) | Spring Data Redis (`StringRedisTemplate` com TTL) |
| **Mensageria (RabbitMQ)** | aio-pika | Spring AMQP (`RabbitTemplate`) |
| **Autenticação OAuth2 Social** | httpx (Google OAuth2 manual) | Spring Security OAuth2 Client (`oauth2Login` / Google Provider) |
| **2FA / MFA (TOTP)** | pyotp | Biblioteca Java TOTP (`com.warrenstrange:googleauth`) |
| **Configuração de Ambiente** | pydantic-settings (`Settings`) | `application.yml` + `@ConfigurationProperties` |
| **Observabilidade e Métricas** | Prometheus FastApi Instrumentator | Spring Boot Actuator + Micrometer Prometheus |

---

## 2. Estrutura de Diretórios Equivalente (Clean Architecture)

A organização segue o padrão em camadas e Clean Architecture no Spring Boot:

### Origem (Python / FastAPI):
```text
backend/iam-service/app/
├── api/
│   ├── routers.py              # Endpoints de login, refresh, usuários
│   └── oauth_router.py         # Endpoints Google OAuth2
├── config.py                   # Configurações e variáveis de ambiente
├── domain/
│   └── models/
│       ├── user.py             # Modelo SQLAlchemy User e Role Enum
│       ├── oauth_account.py    # Modelo OAuthAccount
│       ├── password_reset.py   # Modelo PasswordResetToken
│       └── schemas.py          # Schemas Pydantic (UserCreate, TokenResponse, etc.)
├── infrastructure/
│   ├── database.py             # Sessão SQLAlchemy e engine async
│   ├── events/
│   │   └── publishers.py       # Publicador de eventos RabbitMQ
│   ├── redis_client.py         # Cliente Redis e métodos de Blacklist
│   └── security.py             # Geração de JWT, hash de senha e validação
└── main.py                     # Inicialização da aplicação e rotas
```

### Destino (Java 21 / Spring Boot 3.x):
```text
backend-java/iam-service/
├── pom.xml
└── src/main/java/br/edu/ifrn/promptuario/iam/
    ├── IamServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java         # Spring Security 6, PasswordEncoder, JWT Filter
    │   ├── RedisConfig.java            # Configuração do StringRedisTemplate
    │   ├── RabbitMQConfig.java         # Exchange `promptuario.iam` e Jackson Converter
    │   └── JwtProperties.java          # Propriedades de expiração e segredos JWT
    ├── controller/
    │   ├── AuthController.java         # /api/v1/auth (login, refresh, logout, 2fa)
    │   ├── OAuthController.java        # /api/v1/auth/oauth/google
    │   └── UserController.java         # /api/v1/users (CRUD de usuários)
    ├── model/
    │   ├── User.java                   # Entidade JPA (tabela `users`)
    │   ├── Role.java                   # Enum (ADMIN, DOCTOR, ATTENDANT, PATIENT)
    │   ├── OAuthAccount.java           # Entidade JPA (tabela `oauth_accounts`)
    │   └── PasswordResetToken.java     # Entidade JPA (tabela `password_reset_tokens`)
    ├── dto/
    │   ├── LoginRequestDTO.java        # Record Java para login
    │   ├── TokenResponseDTO.java       # Record Java com access_token, refresh_token
    │   ├── UserCreateDTO.java          # Record Java com validações Jakarta
    │   ├── UserResponseDTO.java        # Record Java com projeção segura de usuário
    │   └── PasswordResetDTO.java       # Record Java para recuperação de senha
    ├── repository/
    │   ├── UserRepository.java         # JpaRepository<User, UUID>
    │   ├── OAuthAccountRepository.java # JpaRepository<OAuthAccount, UUID>
    │   └── PasswordResetRepository.java# JpaRepository<PasswordResetToken, UUID>
    ├── service/
    │   ├── AuthService.java            # Lógica de login, emissão de JWT e refresh
    │   ├── UserService.java            # Criação, busca e desativação de usuários
    │   ├── TokenBlacklistService.java  # Gerenciamento de revogação no Redis
    │   └── TwoFactorService.java       # Geração e validação de código TOTP (Google Auth)
    ├── messaging/
    │   └── IamEventPublisher.java      # Publicador RabbitMQ (UserCreated, UserDeactivated)
    └── exception/
        └── GlobalExceptionHandler.java # @RestControllerAdvice para erros 400, 401, 403, 404, 409
```

---

## 3. Mapeamento de Modelos de Dados (PostgreSQL `iam_db`)

### 1. Entidade `User` (`users`):
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- ADMIN, DOCTOR, ATTENDANT, PATIENT
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(255) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NULL
);
```
- **Mapeamento JPA:** Mapear `id` como `UUID` nativo do Java. `role` mapeado com `@Enumerated(EnumType.STRING)`.
- Senhas salvas com hash seguro via `BCryptPasswordEncoder` (fator de custo 12).

### 2. Entidade `OAuthAccount` (`oauth_accounts`):
```sql
CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL, -- google
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_user UNIQUE (provider, provider_user_id)
);
```

### 3. Entidade `PasswordResetToken` (`password_reset_tokens`):
```sql
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

---

## 4. Endpoints REST Preservados (Contratos Externos)

Todas as URLs, métodos HTTP e estruturas JSON de entrada e saída permanecem **estritamente inalterados**:

| Método | Endpoint | Papel Mínimo | Descrição |
| :--- | :--- | :--- | :--- |
| `GET` | `/healthz` | Público | Health check para o Gateway |
| `POST` | `/api/v1/auth/login` | Público | Autentica com e-mail/senha; valida 2FA se ativo; emite JWT |
| `POST` | `/api/v1/auth/refresh` | Público | Renova access token com base no refresh token |
| `POST` | `/api/v1/auth/logout` | Autenticado | Adiciona o token JWT na blacklist do Redis com TTL restante |
| `POST` | `/api/v1/auth/forgot-password` | Público | Gera token de recuperação de senha |
| `POST` | `/api/v1/auth/reset-password` | Público | Redefine senha a partir do token de recuperação |
| `GET` | `/api/v1/auth/oauth/google` | Público | Inicia autenticação social Google OAuth2 |
| `GET` | `/api/v1/auth/oauth/google/callback` | Público | Recebe código OAuth2, provisiona usuário e retorna tokens |
| `GET` | `/api/v1/users` | `ADMIN`, `ATTENDANT` | Lista usuários paginados (filtros por role e status) |
| `POST` | `/api/v1/users` | `ADMIN`, `ATTENDANT` | Cria novo usuário e publica `UserCreatedEvent` |
| `GET` | `/api/v1/users/me` | Autenticado | Retorna os dados do próprio usuário autenticado |
| `GET` | `/api/v1/users/{id}` | `ADMIN` | Busca dados do usuário por ID |
| `PATCH` | `/api/v1/users/{id}/deactivate` | `ADMIN` | Desativa usuário e publica `UserDeactivatedEvent` |

---

## 5. Eventos RabbitMQ (Event-Driven Integration)

O IAM Service é a fonte da verdade para contas de usuário. Ele publica eventos na exchange de domínio:

- **Exchange:** `promptuario.iam` (tipo `topic` ou `direct`).
- **Eventos Publicados:**
  1. `UserCreatedEvent` (`routing_key: user.created`):
     - Payload: `{"user_id": "UUID", "email": "string", "full_name": "string", "role": "string", "created_at": "ISO-8601"}`
  2. `UserDeactivatedEvent` (`routing_key: user.deactivated`):
     - Payload: `{"user_id": "UUID", "reason": "string", "deactivated_at": "ISO-8601"}`
     - **Consumidores:** `patient-clinical-service` consome este evento para desativar o registro do paciente e cancelar automaticamente consultas futuras em transação relacional única.

---

## 6. Segurança e Revogação no Redis

1. **Estrutura do Token JWT (HS256 ou RS256):**
   - Claims: `sub` (user_id), `email`, `role`, `exp`, `jti` (JWT ID único).
2. **Blacklist no Redis:**
   - No logout, o `jti` ou a hash do token é gravada no Redis com chave `blacklist:token:{jti}`.
   - O tempo de vida (TTL) é calculado exatamente como: `expiracao_do_token - tempo_atual`.
   - O filtro de autenticação (`JwtAuthenticationFilter`) checa `redisTemplate.hasKey(...)`. Se presente, a requisição é rejeitada imediatamente com status `401 Unauthorized`.

---

## 7. Passo a Passo da Refatoração

### Passo 1: Inicialização do Projeto
- Criar o projeto no Spring Initializr com dependências: `Spring Web`, `Spring Data JPA`, `Spring Data Redis`, `PostgreSQL Driver`, `Flyway`, `Validation`, `Spring Security`, `OAuth2 Client`, `Spring for RabbitMQ`, `Spring Boot Actuator`, `Lombok`.

### Passo 2: Script Flyway Inicial
- Criar `src/main/resources/db/migration/V1__init_iam_schema.sql` com as tabelas `users`, `oauth_accounts` e `password_reset_tokens`.
- Inserir a seed do administrador padrão (`admin@promptuario.health`) com hash BCrypt seguro.

### Passo 3: Entidades JPA e Repositórios
- Implementar as classes de modelo em `model/` e os repositórios Spring Data JPA com busca customizada (`findByEmail`, `findByRoleAndIsActive`).

### Passo 4: DTOs e Validação Jakarta
- Criar os Records para requests e responses, substituindo as regras do Pydantic.
- Anotações: `@Email`, `@NotBlank`, `@Size(min = 8)`.

### Passo 5: Mecanismos de Autenticação e Segurança
- Configurar `SecurityFilterChain` permitindo endpoints públicos (`/healthz`, `/api/v1/auth/login`, etc.) e exigindo autenticação nos demais.
- Implementar `JwtTokenProvider` para criação e validação do token JWT.
- Implementar o filtro `JwtAuthenticationFilter` integrado ao `TokenBlacklistService` (Redis).

### Passo 6: Serviços e Publicação de Eventos
- Implementar `AuthService` e `UserService`.
- Implementar `IamEventPublisher` com `RabbitTemplate` para notificar `UserCreated` e `UserDeactivated`.

### Passo 7: Controladores REST e Exceções
- Criar `AuthController`, `UserController`, `OAuthController`.
- Criar `GlobalExceptionHandler` capturando `BadCredentialsException`, `EntityNotFoundException`, `MethodArgumentNotValidException`.

### Passo 8: Dockerfile e Integração com Docker Compose
- Criar `Dockerfile` multi-stage focado em Java 21 (`eclipse-temurin:21-jre-alpine`).
- Configurar porta host `8001:8000` no `docker-compose.yml` e apontar para as variáveis de ambiente existentes.

### Passo 9: Testes Automatizados
- Testes unitários com JUnit 5 e Mockito para as regras de autenticação e expiração de token.
- Testes de integração com Testcontainers (PostgreSQL e Redis) validando o fluxo de login e logout com blacklist.
