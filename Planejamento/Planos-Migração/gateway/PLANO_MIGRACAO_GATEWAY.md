# Plano de Migração do API Gateway: Python (FastAPI) para Java (Spring Cloud Gateway)

Este documento detalha o passo a passo completo e o mapeamento técnico necessário para migrar o **API Gateway** do ecossistema Promptuário da sua stack atual em Python (FastAPI / httpx / Redis) para a stack de borda corporativa: **Java 21 (LTS) e Spring Cloud Gateway (Spring Boot 3.x / WebFlux Reativo / Netty)**.

---

## 1. Mapeamento de Stack Tecnológica

Equivalência 1-para-1 de cada tecnologia que compõe o API Gateway atualmente:

| Componente | Atual (Python) | Destino (Java 21 / Spring Cloud Gateway) |
| :--- | :--- | :--- |
| **Linguagem Base** | Python 3.12 | Java 21 (LTS) |
| **Framework Base** | FastAPI + Uvicorn (ASGI) | Spring Boot 3.3+ com Spring WebFlux (Project Reactor / Netty) |
| **Engine de Roteamento** | Reverse Proxy customizado (httpx async) | Spring Cloud Gateway (Reactive RouteLocator / Netty) |
| **Gerenciador de Dependências** | pip / requirements.txt | Maven (`pom.xml`) |
| **Validação de JWT** | python-jose / Depends | Spring Security Reactive OAuth2 Resource Server / Nimbus JWT |
| **Blacklist de Tokens (Logout)** | Redis 7 (redis-py async) | Spring Data Reactive Redis (`ReactiveStringRedisTemplate`) |
| **Rate Limiting** | Custom Redis Token Bucket | Spring Cloud Gateway `RedisRateLimiter` |
| **Circuit Breaker & Resiliência** | Custom pybreaker | Spring Cloud CircuitBreaker com **Resilience4j** reativo |
| **Agregação de Saúde (Health Check)** | Endpoint customizado `/healthz/services` | WebFlux Handler Reativo agregando chamadas via `WebClient` |
| **Controle de CORS** | FastAPI `CORSMiddleware` | Spring Cloud Gateway `GlobalCorsProperties` |
| **Observabilidade e Métricas** | Prometheus FastApi Instrumentator | Spring Boot Actuator + Micrometer Gateway Metrics |

---

## 2. Estrutura de Diretórios Equivalente (Arquitetura Reativa)

A arquitetura do Spring Cloud Gateway é puramente **não-bloqueante (Non-blocking I/O)**, operando sobre o Netty:

### Origem (Python / FastAPI):
```text
backend/gateway/app/
├── api/
│   └── routers.py              # Definição dos proxies reversos e endpoints de health
├── config.py                   # URLs dos serviços e chaves de configuração
├── core/
│   ├── circuit_breaker.py      # Lógica de disjuntor de circuito
│   ├── rate_limit.py           # Limitador de requisições por IP/token
│   └── security.py             # Validação do JWT e checagem da blacklist no Redis
└── main.py                     # Inicialização da aplicação e middlewares
```

### Destino (Java 21 / Spring Cloud Gateway):
```text
backend-java/gateway/
├── pom.xml
└── src/main/java/br/edu/ifrn/promptuario/gateway/
    ├── GatewayApplication.java
    ├── config/
    │   ├── GatewayRoutesConfig.java    # Definição programática das rotas ou via YAML
    │   ├── SecurityConfig.java         # SecurityWebFilterChain reativo (OAuth2 JWT)
    │   ├── RateLimiterConfig.java      # Configuração do KeyResolver (por IP ou por Usuário)
    │   └── RedisConfig.java            # ReactiveRedisConnectionFactory
    ├── filter/
    │   ├── JwtBlacklistGlobalFilter.java# Filtro global checando revogação no Redis
    │   └── RequestTracingGlobalFilter.java# Injeção de Correlation-ID para rastreabilidade
    ├── handler/
    │   └── AggregatedHealthHandler.java# Agregador reativo dos microsserviços (/healthz/services)
    ├── fallback/
    │   └── ServiceFallbackController.java# Endpoints de contingência acionados pelo Circuit Breaker
    └── properties/
        └── ServiceEndpointsProperties.java# Mapeamento dinâmico das URLs dos microsserviços
```

---

## 3. Mapeamento das Rotas de Borda (Roteamento Centralizado)

O Gateway é o único ponto de entrada do sistema (porta `8000`). Todas as chamadas dos clientes externos (incluindo o Frontend React) passam por ele:

| Prefixo da Rota Pública | Serviço de Destino Interno | Porta Container | Regras Aplicadas |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/**` | `iam-service` | `http://iam-service:8000` | Rate Limit, Bypass de Auth (login/refresh) |
| `/api/v1/users/**` | `iam-service` | `http://iam-service:8000` | JWT Auth, RBAC |
| `/api/v1/patients/**` | `patient-clinical-service` | `http://patient-clinical-service:8000` | JWT Auth, Circuit Breaker |
| `/api/v1/appointments/**` | `patient-clinical-service` | `http://patient-clinical-service:8000` | JWT Auth, Circuit Breaker |
| `/api/v1/schedules/**` | `patient-clinical-service` | `http://patient-clinical-service:8000` | JWT Auth |
| `/api/v1/records/**` | `patient-clinical-service` | `http://patient-clinical-service:8000` | JWT Auth, Circuit Breaker |
| `/api/v1/ai/**` | `ai-service` | `http://ai-service:8000` | JWT Auth, Timeout estendido (LLM) |
| `/api/v1/reports/**` | `reporting-service` | `http://reporting-service:8000` | JWT Auth |
| `/healthz` | Local | — | Retorna status do próprio Gateway |
| `/healthz/services` | Local Agregado | — | Dispara chamadas paralelas reativas a todos os serviços |

---

## 4. Configuração Declarativa de Rotas (`application.yml`)

O Spring Cloud Gateway permite configurar as rotas com filtros de forma declarativa e concisa:

```yaml
server:
  port: 8000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
      routes:
        # Rotas de Autenticação e Usuários (IAM Service)
        - id: iam-auth-route
          uri: ${IAM_SERVICE_URL:http://iam-service:8000}
          predicates:
            - Path=/api/v1/auth/**, /api/v1/users/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userKeyResolver}"

        # Rotas Unificadas do Patient-Clinical Service
        - id: patient-clinical-route
          uri: ${PATIENT_CLINICAL_SERVICE_URL:http://patient-clinical-service:8000}
          predicates:
            - Path=/api/v1/patients/**, /api/v1/appointments/**, /api/v1/schedules/**, /api/v1/records/**
          filters:
            - name: CircuitBreaker
              args:
                name: patientClinicalCircuitBreaker
                fallbackUri: forward:/fallback/patient-clinical

        # Rotas do AI Service
        - id: ai-route
          uri: ${AI_SERVICE_URL:http://ai-service:8000}
          predicates:
            - Path=/api/v1/ai/**
          filters:
            - name: CircuitBreaker
              args:
                name: aiCircuitBreaker
                fallbackUri: forward:/fallback/ai

        # Rotas do Reporting Service
        - id: reporting-route
          uri: ${REPORTING_SERVICE_URL:http://reporting-service:8000}
          predicates:
            - Path=/api/v1/reports/**, /api/v1/admin/**, /api/v1/audit/**
```

---

## 5. Filtros Globais e Segurança Reativa

### 1. Verificação de Revogação de Token no Redis (`JwtBlacklistGlobalFilter`):
- O filtro intercepta todas as requisições autenticadas.
- Extrai a claim `jti` ou a assinatura do token do header `Authorization: Bearer <token>`.
- Consulta assincronamente o Redis: `reactiveRedisTemplate.hasKey("blacklist:token:" + jti)`.
- Se o token foi revogado no IAM, retorna imediatamente `HttpStatus.UNAUTHORIZED` (401), impedindo o tráfego de alcançar os microsserviços internos.

### 2. Injeção de Rastreamento Distribuído (`RequestTracingGlobalFilter`):
- Gera ou propaga o header `X-Correlation-ID`.
- Repassa o header para todos os serviços a jusante para correlação de logs no Grafana/Loki.

---

## 6. Agregação de Saúde Reativa (`/healthz/services`)

No endpoint `/healthz/services`, o Gateway deve executar chamadas paralelas reativas a todos os serviços com timeout curto (ex: 2 segundos) para validar a integridade geral do ecossistema:

```java
@GetMapping("/healthz/services")
public Mono<HealthStatusResponse> getAggregatedHealth() {
    return Mono.zip(
        checkHealth("iam", iamServiceUrl),
        checkHealth("patient_clinical", patientClinicalServiceUrl),
        checkHealth("ai", aiServiceUrl),
        checkHealth("reporting", reportingServiceUrl)
    ).map(tuple -> new HealthStatusResponse(
        "ok",
        Map.of(
            "iam", tuple.getT1(),
            "patient_clinical", tuple.getT2(),
            "ai", tuple.getT3(),
            "reporting", tuple.getT4()
        )
    ));
}
```

---

## 7. Passo a Passo da Refatoração

### Passo 1: Inicialização do Projeto
- Criar projeto Maven no Spring Initializr com: `Spring Cloud Routing (Gateway)`, `Spring Reactive Web (WebFlux)`, `Spring Data Reactive Redis`, `Resilience4j`, `Spring Security`, `OAuth2 Resource Server`, `Spring Boot Actuator`, `Lombok`.
- Versões compatíveis: Spring Boot 3.3+ e Spring Cloud 2023.x (Leyton).

### Passo 2: Configuração de Variáveis de Ambiente e Rotas
- Mapear propriedades dinâmicas em `application.yml` para ler as variáveis de ambiente do `docker-compose.yml` (`IAM_SERVICE_URL`, `PATIENT_CLINICAL_SERVICE_URL`, `AI_SERVICE_URL`, `REPORTING_SERVICE_URL`).

### Passo 3: Filtro Global de Autenticação e Blacklist
- Configurar o `SecurityWebFilterChain` e implementar o filtro reativo de consulta ao Redis para barrar tokens revogados.

### Passo 4: Rate Limiting com Redis
- Configurar o `KeyResolver` baseado no IP da requisição (`X-Forwarded-For`) ou no identificador do usuário autenticado.

### Passo 5: Circuit Breakers e Endpoints de Fallback
- Configurar tolerância a falhas com Resilience4j para isolar falhas de microsserviços downstream sem derrubar o gateway.
- Criar `ServiceFallbackController` retornando mensagens padronizadas em formato JSON (status `503 Service Unavailable`).

### Passo 6: Agregador de Health Check
- Implementar o controlador reativo para os endpoints `/healthz` e `/healthz/services`.

### Passo 7: Dockerização e Integração
- Criar `Dockerfile` multi-stage com `eclipse-temurin:21-jre-alpine`.
- Atualizar a entrada do serviço `gateway` no `docker-compose.yml` raiz para apontar para a imagem compilada, mantendo o binding de porta `8000:8000`.

### Passo 8: Testes Automatizados Reativos
- Implementar testes de integração com `WebTestClient` validando o roteamento correto, cabeçalhos CORS, rejeição de tokens revogados no Redis e fallbacks de circuit breaker.
