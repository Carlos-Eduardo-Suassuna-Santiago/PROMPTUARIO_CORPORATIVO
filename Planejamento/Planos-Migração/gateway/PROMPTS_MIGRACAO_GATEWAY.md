# Guia de Prompts de Implementação: Migração do API Gateway (FastAPI -> Spring Cloud Gateway / Java 21)

Este documento contém o conjunto completo de **prompts estruturados e detalhados**, organizados em **8 fases sequenciais**, para guiar a implementação assistida por IA da migração do **API Gateway** de Python (FastAPI / `httpx` / Redis) para a stack corporativa reativa: **Java 21 (LTS), Spring Boot 3.3+, Spring Cloud Gateway (WebFlux / Project Reactor / Netty), Spring Data Reactive Redis e Resilience4j**.

O conteúdo foi desenhado com base nas especificações de [PLANO_MIGRACAO_GATEWAY.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/Planos-Migra%C3%A7%C3%A3o/gateway/PLANO_MIGRACAO_GATEWAY.md) e na topologia unificada dos microsserviços do PROMPTUÁRIO.

---

## Índice das Fases de Implementação

1. [Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base](#fase-1-bootstrap-do-projeto-dependências-maven-e-configurações-base)
2. [Fase 2: Configuração Declarativa de Rotas, Predicados e Propriedades](#fase-2-configuração-declarativa-de-rotas-predicados-e-propriedades)
3. [Fase 3: Autenticação Reativa, Filtro Global de Blacklist Redis e Headers de Confiança](#fase-3-autenticação-reativa-filtro-global-de-blacklist-redis-e-headers-de-confiança)
4. [Fase 4: Rate Limiting Reativo com Redis (Anon vs Authenticated)](#fase-4-rate-limiting-reativo-com-redis-anon-vs-authenticated)
5. [Fase 5: Circuit Breakers Reativos (Resilience4j) e Fallbacks Padronizados](#fase-5-circuit-breakers-reativos-resilience4j-e-fallbacks-padronizados)
6. [Fase 6: Agregador de Saúde Reativo (/healthz/services) e Proxy RabbitMQ](#fase-6-agregador-de-saúde-reativo-healthzservices-e-proxy-rabbitmq)
7. [Fase 7: Dockerfile Multi-Stage e Integração no Docker Compose](#fase-7-dockerfile-multi-stage-e-integração-no-docker-compose)
8. [Fase 8: Testes Automatizados Reativos com WebTestClient e Homologação](#fase-8-testes-automatizados-reativos-com-webtestclient-e-homologação)

---

## Diretrizes Gerais para o Ciclo de Execução

- **Stack 100% Não-Bloqueante (Reactive / Netty):** O Spring Cloud Gateway opera exclusivamente sobre **Spring WebFlux** e **Project Reactor**. Nunca adicione a dependência `spring-boot-starter-web` (Tomcat bloqueante), pois gerará conflito de runtime no Netty.
- **Injeção de Cabeçalhos de Confiança Downstream:** O Gateway deve validar o token JWT e injetar para os microsserviços internos os headers:
  * `X-User-Id`
  * `X-User-Role`
  * `X-User-Email`
  * `X-Request-Id`
  * `X-Correlation-Id`
  * `X-Forwarded-For`
  * `X-Gateway: promptuario-gateway/1.0`
- **Validação de Revogação Imediata:** Tokens invalidados por logout residem no Redis sob a chave `blacklist:<token>`. O Gateway deve consultar o Redis de forma não-bloqueante (`ReactiveStringRedisTemplate`) antes de repassar a requisição.
- **Porta Canônica:** O Gateway é o único ponto de entrada público e atende na **porta 8000** (mapeada no host como `8000:8000`).

---

## Fase 1: Bootstrap do Projeto, Dependências Maven e Configurações Base

### Objetivo
Inicializar a estrutura do projeto `backend-java/gateway` com Java 21 e Spring Boot 3.3+, integrando as dependências do Spring Cloud 2023.x (Leyton), WebFlux reativo, Spring Data Reactive Redis, Resilience4j, Actuator e micrômetro para Prometheus.

### Arquivos Alvo
- `backend-java/gateway/pom.xml`
- `backend-java/gateway/src/main/resources/application.yml`
- `backend-java/gateway/src/main/java/br/edu/ifrn/promptuario/gateway/GatewayApplication.java`
- `backend-java/gateway/src/main/java/br/edu/ifrn/promptuario/gateway/config/RedisConfig.java`

### Prompt da Fase 1

```text
Atue como Engenheiro de Software Sênior especialista em Java 21, Spring Boot 3.3+ e Spring Cloud Gateway.
Nosso objetivo é criar a base do novo API Gateway reativo corporativo em "backend-java/gateway/".

Requisito Crítico: O Spring Cloud Gateway exige arquitetura reativa não-bloqueante (Netty). NUNCA inclua "spring-boot-starter-web" (Servlet/Tomcat).

Gere os arquivos estruturais iniciais do projeto:

1. "pom.xml":
   - Java 21, Spring Boot 3.3.4 (ou versão 3.3+ estável).
   - Spring Cloud Release Train 2023.0.3 (Leyton) gerenciado em <dependencyManagement>.
   - GroupId: "br.edu.ifrn.promptuario"
   - ArtifactId: "gateway"
   - Dependências obrigatórias:
     * org.springframework.cloud:spring-cloud-starter-gateway
     * org.springframework.boot:spring-boot-starter-data-redis-reactive (Project Reactor Redis)
     * org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j
     * org.springframework.boot:spring-boot-starter-security
     * org.springframework.boot:spring-boot-starter-oauth2-resource-server
     * org.springframework.boot:spring-boot-starter-actuator
     * io.micrometer:micrometer-registry-prometheus
     * org.projectlombok:lombok (scope provided)
     * org.springframework.boot:spring-boot-starter-test e io.projectreactor:reactor-test (scope test).

2. "src/main/resources/application.yml":
   - Configuração do servidor HTTP na porta 8000.
   - Redis reativo: "spring.data.redis.host: ${REDIS_HOST:localhost}", "spring.data.redis.port: 6379", "spring.data.redis.database: 0".
   - Actuator: expondo endpoints "health,info,prometheus,gateway" em "/actuator".
   - Variáveis de ambiente dos serviços a jusante mapeadas sob "app.services":
     * iam: "${IAM_SERVICE_URL:http://iam-service:8000}"
     * patient-clinical: "${PATIENT_CLINICAL_SERVICE_URL:http://patient-clinical-service:8000}"
     * ai: "${AI_SERVICE_URL:http://ai-service:8000}"
     * reporting: "${REPORTING_SERVICE_URL:http://reporting-service:8000}"
     * rabbitmq-management: "${RABBITMQ_MANAGEMENT_URL:http://guest:guest@rabbitmq:15672}"
   - Segredo JWT para decodificação simétrica HMAC-SHA256:
     * "app.security.jwt.secret: ${JWT_SECRET_KEY:change-me-in-production-super-secret-key-min-32-chars}"

3. "GatewayApplication.java":
   - Classe principal com anotação @SpringBootApplication.

4. "RedisConfig.java" (pacote "br.edu.ifrn.promptuario.gateway.config"):
   - Bean "ReactiveStringRedisTemplate" configurado com ReactiveRedisConnectionFactory para operações atômicas de chave/valor no Redis sem bloqueio de thread.

Forneça o código completo e verifique se a compilação "mvn clean compile" é executada com sucesso.
```

### Critérios de Aceite
- `mvn clean compile` sem erros e sem dependências Servlet/Tomcat no classpath.
- Carregamento limpo das propriedades reativas no `application.yml`.

---

## Fase 2: Configuração Declarativa de Rotas, Predicados e Propriedades

### Objetivo
Configurar a tabela completa de rotas de borda no `application.yml` e/ou via classe de configuração `RouteLocator`, direcionando o tráfego externo para os microsserviços correspondentes (`iam-service`, `patient-clinical-service`, `ai-service`, `reporting-service`), aplicando filtros de CORS global e reescrita de caminhos.

### Arquivos Alvo
- `backend-java/gateway/src/main/resources/application.yml` (atualização das rotas)
- `config/GatewayRoutesConfig.java`
- `properties/ServiceEndpointsProperties.java`

### Prompt da Fase 2

```text
Atue como Arquiteto de Software especialista em Spring Cloud Gateway.
Vamos implementar o roteamento centralizado do API Gateway mantendo 100% de paridade com as rotas originais do FastAPI.

1. "ServiceEndpointsProperties.java" em "br.edu.ifrn.promptuario.gateway.properties":
   - Classe anotada com @ConfigurationProperties(prefix = "app.services") para encapsular as URIs dos serviços:
     * iam, patientClinical, ai, reporting, rabbitmqManagement.

2. Configuração de CORS Global no "application.yml":
   - Permitir origens configuráveis via env var "${FRONTEND_URL:*}".
   - Métodos permitidos: GET, POST, PUT, PATCH, DELETE, OPTIONS.
   - Cabeçalhos permitidos: "*".
   - AllowCredentials: true.

3. Mapeamento das Rotas no "application.yml" ou via "GatewayRoutesConfig.java":
   Implemente a tabela de rotas exata do ecossistema:
   - Rota "iam-public":
     * Predicados: Path=/api/v1/auth/login, /api/v1/auth/refresh, /api/v1/auth/forgot-password, /api/v1/auth/reset-password, /api/v1/auth/register-patient, /api/v1/auth/oauth/**
     * Destino: ${app.services.iam}
     * Não exige validação de token prévia.
   - Rota "iam-protected":
     * Predicados: Path=/api/v1/auth/**, /api/v1/users/**
     * Destino: ${app.services.iam}
   - Rota "patient-clinical":
     * Predicados: Path=/api/v1/patients/**, /api/v1/appointments/**, /api/v1/schedules/**, /api/v1/records/**
     * Destino: ${app.services.patient-clinical}
     * Observação: cobre tanto os endpoints de cadastro de pacientes quanto os registros clínicos unificados.
   - Rota "ai-debug-public":
     * Predicados: Path=/api/v1/ai/debug
     * Destino: ${app.services.ai}
   - Rota "ai-protected":
     * Predicados: Path=/api/v1/ai/**
     * Destino: ${app.services.ai}
   - Rota "reporting":
     * Predicados: Path=/api/v1/reports/**, /api/v1/admin/**, /api/v1/audit/**
     * Destino: ${app.services.reporting}
   - Rota "rabbitmq-ui":
     * Predicado: Path=/rabbitmq/**
     * Filtro: RewritePath=/rabbitmq/(?<segment>.*), /${segment}
     * Destino: ${app.services.rabbitmq-management}
     * Remove o header de Authorization para evitar conflito com a autenticação básica do RabbitMQ.

4. Headers de Segurança HTTP (adicionados globalmente em todas as respostas):
   - X-Content-Type-Options: nosniff
   - X-Frame-Options: DENY
   - Strict-Transport-Security: max-age=31536000; includeSubDomains

Gere os arquivos com sintaxe YAML e Java limpa e com logs de depuração ativáveis para roteamento.
```

### Critérios de Aceite
- Todas as rotas públicas e protegidas mapeadas sem omissão de prefixos.
- Roteamento transparente de `/api/v1/patients/**` e `/api/v1/records/**` para o mesmo microsserviço unificado (`patient-clinical-service`).

---

## Fase 3: Autenticação Reativa, Filtro Global de Blacklist Redis e Headers de Confiança

### Objetivo
Configurar o Spring Security reativo com decodificação de JWT HMAC-SHA256, implementar o filtro global não-bloqueante `JwtBlacklistGlobalFilter` para checagem de tokens revogados no Redis (`blacklist:<token>`), e injetar os headers downstream de confiança (`X-User-Id`, `X-User-Role`, `X-User-Email`, `X-Correlation-Id`).

### Arquivos Alvo
- `config/SecurityConfig.java`
- `filter/JwtBlacklistGlobalFilter.java`
- `filter/RequestTracingGlobalFilter.java`

### Prompt da Fase 3

```text
Atue como Especialista em Segurança Reativa com Spring Security 6 e Spring Cloud Gateway.
Vamos implementar a camada de validação de identidade e controle de tokens revogados na borda.

1. "SecurityConfig.java" em "br.edu.ifrn.promptuario.gateway.config":
   - Configurar o "SecurityWebFilterChain" reativo:
     * Desabilitar CSRF (arquitetura stateless REST).
     * Configurar ServerHttpSecurity com modo reativo.
     * Endpoints liberados (permit all):
       - "/healthz", "/healthz/services"
       - "/actuator/**"
       - "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/register-patient", "/api/v1/auth/oauth/**"
       - "/api/v1/ai/debug"
       - "/api/v1/schedules/*/available-slots"
       - "/rabbitmq/**"
     * Todas as demais rotas exigem autenticação.
     * Configurar "oauth2ResourceServer" com validador de JWT reativo ("ReactiveJwtDecoder").
   - Bean "ReactiveJwtDecoder":
     * Decodifica tokens HMAC-SHA256 utilizando a chave simétrica configurada em "app.security.jwt.secret".

2. "JwtBlacklistGlobalFilter.java" (implementa GlobalFilter, Ordered com alta precedência):
   - Intercepta a requisição e verifica se existe o cabeçalho "Authorization: Bearer <token>".
   - Se houver token:
     * Extrai a string pura do token.
     * Consulta o Redis reativo de forma assíncrona: "reactiveStringRedisTemplate.hasKey("blacklist:" + token)".
     * Se retornar TRUE (token revogado no logout):
       - Aborta imediatamente o pipeline com HTTP 401 Unauthorized.
       - Retorna corpo JSON: { "detail": "Token revogado" }.
       - Não repassa a requisição para nenhum microsserviço downstream.
     * Se o token for válido e ativo:
       - Extrai as claims: "sub" (user_id), "role" (user_role) e "email" (user_email).
       - Modifica a requisição ("ServerHttpRequest.mutate()") injetando os headers:
         * "X-User-Id": valor do sub
         * "X-User-Role": valor da role
         * "X-User-Email": valor do email
         * "X-Gateway": "promptuario-gateway/1.0"
       - Permite que o pipeline continue.

3. "RequestTracingGlobalFilter.java" (implementa GlobalFilter, Ordered com a mais alta precedência):
   - Verifica se o cliente enviou "X-Correlation-Id" ou "X-Request-Id".
   - Se não existir, gera um UUID v4 (ou hash hexadecimal curto) e injeta na requisição ("X-Correlation-Id" e "X-Request-Id") e no cabeçalho da resposta.
   - Injeta também o cabeçalho "X-Forwarded-For" com o IP remoto real do cliente.

Garanta que os filtros utilizem Mono e operadores reativos sem chamadas bloqueantes (.block() ou .toFuture().get() são expressamente proibidos).
```

### Critérios de Aceite
- Requisições com tokens presentes na blacklist do Redis recebem HTTP 401 instantâneo.
- Microsserviços internos recebem os cabeçalhos de identidade `X-User-Id` e `X-User-Role` sem necessidade de revalidar a assinatura criptográfica se confiarem no Gateway.

---

## Fase 4: Rate Limiting Reativo com Redis (Anon vs Authenticated)

### Objetivo
Configurar a limitação de taxa de requisições (`RequestRateLimiter`) com Redis Token Bucket do Spring Cloud Gateway, implementando limites distintos para usuários anônimos (30 req/min por IP) e usuários autenticados (300 req/min por `user_id`), retornando HTTP 429 Too Many Requests com cabeçalho `Retry-After`.

### Arquivos Alvo
- `config/RateLimiterConfig.java`
- `filter/CustomRateLimiterResponseFilter.java`
- `backend-java/gateway/src/main/resources/application.yml` (ajustes nos filtros das rotas)

### Prompt da Fase 4

```text
Atue como Engenheiro de Backend especialista em Resiliência e Rate Limiting no Spring Cloud Gateway.
Vamos implementar o controle de vazão de requisições baseado em Redis.

Requisitos de Rate Limit do PROMPTUÁRIO:
- Usuários Anônimos (não autenticados): 30 requisições por minuto, agrupados por endereço IP do cliente.
- Usuários Autenticados: 300 requisições por minuto, agrupados pelo ID do usuário extraído do token ("sub").
- Clientes com Chave de API ("X-Api-Key"): 120 requisições por minuto, agrupados pelo hash SHA-256 da chave.

Implemente os seguintes componentes em "br.edu.ifrn.promptuario.gateway.config":

1. "RateLimiterConfig.java":
   - Bean "KeyResolver" primário reativo ("userOrIpKeyResolver"):
     * Se a requisição contiver o header "X-Api-Key", retorna "apikey:" + sha256(apiKey).
     * Senão, inspeciona a autenticação no contexto reativo ("ReactiveSecurityContextHolder"):
       - Se autenticado, retorna "user:" + principal.getName() (user_id).
       - Se anônimo, obtém o IP remoto (considerando cabeçalho "X-Forwarded-For") e retorna "ip:" + clientIp.
   - Declaração de Beans "RedisRateLimiter":
     * "anonRateLimiter": replenishRate = 30, burstCapacity = 60 (para anônimos).
     * "authRateLimiter": replenishRate = 300, burstCapacity = 400 (para autenticados).
     * "apiKeyRateLimiter": replenishRate = 120, burstCapacity = 150.

2. Customização do Erro HTTP 429:
   - Quando o limite de taxa for excedido pelo Spring Cloud Gateway, o Gateway deve emitir:
     * Status HTTP 429 Too Many Requests.
     * Header "Retry-After" com os segundos restantes para reabastecimento do bucket.
     * Payload JSON: { "detail": "Taxa de requisições excedida. Tente novamente em breve." } idêntico ao FastAPI.

3. Associação das Rotas no "application.yml":
   - Aplicar o filtro "RequestRateLimiter" nas rotas públicas (/api/v1/auth/**) e rotas de dados (/api/v1/patients/**, etc.).

Forneça a configuração completa e garanta que o rate limiter funcione de forma distribuída utilizando as estruturas de dados nativas do Redis do Spring Cloud Gateway.
```

### Critérios de Aceite
- Disparo de HTTP 429 com mensagem JSON e cabeçalho `Retry-After` ao ultrapassar os limites configurados.
- Isolamento dos contadores no Redis por usuário e por IP.

---

## Fase 5: Circuit Breakers Reativos (Resilience4j) e Fallbacks Padronizados

### Objetivo
Configurar disjuntores de circuito com Resilience4j reativo para isolar falhas nos microsserviços downstream (`patient-clinical-service`, `ai-service`, `reporting-service`), definindo limites de tolerância a falhas e endpoints de fallback que retornem HTTP 503 com o cabeçalho `X-Circuit-Breaker: open`.

### Arquivos Alvo
- `config/ResilienceConfig.java`
- `fallback/ServiceFallbackController.java`
- `backend-java/gateway/src/main/resources/application.yml` (configuração do CircuitBreaker)

### Prompt da Fase 5

```text
Atue como Engenheiro de Confiabilidade de Sistemas (SRE) especialista em Resilience4j e Circuit Breakers Reativos.
Vamos configurar a tolerância a falhas no Spring Cloud Gateway para evitar efeito cascata em caso de indisponibilidade de microsserviços internos.

Requisitos de Circuit Breaker:
- Limite de falhas: 3 falhas consecutivas ou taxa de erro >= 50% abrem o circuito.
- Timeout de reset (tempo aberto): 30 segundos.
- Estado meio-aberto (half-open): permite 2 requisições de teste.
- Timeout de requisição downstream: 30 segundos para serviços gerais; 60 segundos para o "ai-service" (processamento LLM).

1. "ResilienceConfig.java" em "br.edu.ifrn.promptuario.gateway.config":
   - Configurar o "Customizer<ReactiveResilience4JCircuitBreakerFactory>":
     * Criar configurações customizadas com TimeLimiter e CircuitBreakerConfig:
       - Configuração padrão para "patientClinicalCircuitBreaker", "reportingCircuitBreaker", "iamCircuitBreaker".
       - Configuração estendida de timeout para "aiCircuitBreaker" (TimeLimiter com 60s).

2. "ServiceFallbackController.java" em "br.edu.ifrn.promptuario.gateway.fallback":
   - Endpoints reativos (@RestController) que recebem o forward quando o circuito estiver aberto ou quando houver timeout:
     * "/fallback/patient-clinical"
     * "/fallback/ai"
     * "/fallback/reporting"
     * "/fallback/iam"
   - Comportamento do Fallback:
     * Retorna HTTP 503 Service Unavailable.
     * Adiciona o cabeçalho: "X-Circuit-Breaker: open".
     * Corpo da resposta em JSON:
       {
         "detail": "Serviço temporariamente indisponível",
         "service": "<nome_do_servico>",
         "timestamp": "<instant_iso8601>"
       }

3. Atualização no "application.yml":
   - Declarar os filtros "CircuitBreaker" nas rotas do Gateway com os respectivos "fallbackUri: forward:/fallback/...".

Forneça o código reativo com captura de exceções originais (CallNotPermittedException, TimeoutException) e log estruturado.
```

### Critérios de Aceite
- Quando um serviço downstream é derrubado, após 3 falhas as requisições subsequentes são rejeitadas imediatamente com HTTP 503 e `X-Circuit-Breaker: open` sem prender threads do Gateway.
- Após 30 segundos, o circuito transiciona para half-open e recupera automaticamente com a restauração do serviço.

---

## Fase 6: Agregador de Saúde Reativo (/healthz/services) e Proxy RabbitMQ

### Objetivo
Construir o controlador reativo para os endpoints de monitoramento `/healthz` (saúde local do Gateway) e `/healthz/services` (agregação concorrente e paralela da saúde de todos os microsserviços downstream com timeout de 3s), mantendo aliases de compatibilidade para ferramentas legadas, além de configurar o proxy reverso para a interface web do RabbitMQ.

### Arquivos Alvo
- `handler/AggregatedHealthHandler.java`
- `model/HealthStatusResponse.java`
- `config/WebClientConfig.java`

### Prompt da Fase 6

```text
Atue como Desenvolvedor Fullstack/Backend especialista em WebClient reativo e Project Reactor.
Vamos implementar os endpoints de saúde e diagnóstico do Gateway.

1. "WebClientConfig.java" em "br.edu.ifrn.promptuario.gateway.config":
   - Configurar o Bean "WebClient" reativo com timeouts curtos de conexão (Netty HttpClient com ConnectTimeout de 2 segundos e ReadTimeout de 3 segundos).

2. "model/HealthStatusResponse.java" (Record imutável):
   - Campos: String status ("ok" ou "degraded"), Map<String, String> services.

3. "AggregatedHealthHandler.java" (@RestController):
   - Endpoint GET "/healthz":
     * Retorna imediatamente: { "status": "ok", "service": "gateway" } com HTTP 200.
   - Endpoint GET "/healthz/services":
     * Executa chamadas paralelas não-bloqueantes usando "Mono.zip" para os endpoints "/healthz" dos seguintes serviços:
       1. "iam": ${app.services.iam}/healthz
       2. "patient_clinical": ${app.services.patient-clinical}/healthz
       3. "ai": ${app.services.ai}/healthz
       4. "reporting": ${app.services.reporting}/healthz
     * Cada chamada deve ter timeout estrito de 3 segundos ("timeout(Duration.ofSeconds(3))") e fallback com onErrorReturn("unreachable").
     * Regra de status do serviço individual:
       - Se HTTP 200 -> "ok"
       - Se outro status HTTP -> "degraded"
       - Se exceção/timeout -> "unreachable"
     * Aliases de compatibilidade histórica:
       - Adicionar no mapa de saída: "patient" = status de "patient_clinical" e "clinical" = status de "patient_clinical" (garantindo que dashboards de observabilidade e testes legados continuem funcionando).
     * Status geral:
       - Se todos os serviços estiverem "ok", retorna status: "ok" e HTTP 200.
       - Se algum serviço estiver degradado ou inacessível, retorna status: "degraded" e HTTP 200 (ou HTTP 503 dependendo do contrato do agregador).

4. Validação da rota "/rabbitmq/**":
   - Garantir que requisições para "/rabbitmq/api/**" ou interface web sejam roteadas com sucesso para a porta 15672 do RabbitMQ, tratando eventuais falhas de conexão com JSON de erro 502 Bad Gateway amigável.

Forneça o código reativo limpo com Reactor operators ("Mono.zip", "timeout", "map").
```

### Critérios de Aceite
- Execução de `GET /healthz/services` responde em menos de 100ms em condições normais e no máximo 3s em caso de timeout de um serviço.
- Retorno JSON idêntico ao modelo consumido pelo frontend e pelos testes de homologação do Gateway.

---

## Fase 7: Dockerfile Multi-Stage e Integração no Docker Compose

### Objetivo
Criar o `Dockerfile` multi-stage otimizado com `eclipse-temurin:21-jre-alpine` para o serviço Java do Gateway e atualizar a definição do serviço `gateway` no `backend/docker-compose.yml`, preservando as variáveis de ambiente, portas e dependências de rede.

### Arquivos Alvo
- `backend-java/gateway/Dockerfile`
- `backend/docker-compose.yml` (atualização do serviço gateway)

### Prompt da Fase 7

```text
Atue como Especialista em DevOps e Contêineres Docker.
Vamos containerizar o novo API Gateway em Java 21 e atualizar o Docker Compose raiz do projeto.

1. "backend-java/gateway/Dockerfile":
   - Multi-stage build otimizado:
     * Stage 1 (Builder): imagem "maven:3.9-eclipse-temurin-21-alpine".
       - Copiar pom.xml e baixar dependências offline ("mvn dependency:go-offline").
       - Copiar código-fonte ("src") e compilar o JAR executável ("mvn clean package -DskipTests").
     * Stage 2 (Runtime): imagem "eclipse-temurin:21-jre-alpine".
       - Instalar utilitários essenciais: "apk add --no-cache curl tzdata".
       - Criar usuário e grupo não-root "gatewayuser" para conformidade de segurança.
       - Copiar o JAR gerado ("app.jar") do Stage 1.
       - Definir variáveis de JVM otimizadas para Netty reativo:
         "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dio.netty.allocator.type=pooled"
       - Expor a porta 8000.
       - Healthcheck interno no container:
         "HEALTHCHECK --interval=15s --timeout=3s --retries=3 CMD curl -f http://localhost:8000/healthz || exit 1"
       - ENTRYPOINT ["java", "-jar", "app.jar"]

2. Atualização no "backend/docker-compose.yml":
   - Substituir a definição do serviço "gateway" existente:
     ```yaml
     gateway:
       build:
         context: ../backend-java/gateway
         dockerfile: Dockerfile
       container_name: api-gateway
       restart: unless-stopped
       ports:
         - "8000:8000"
       environment:
         - SERVER_PORT=8000
         - IAM_SERVICE_URL=http://iam-service:8000
         - PATIENT_CLINICAL_SERVICE_URL=http://patient-clinical-service:8000
         - AI_SERVICE_URL=http://ai-service:8000
         - REPORTING_SERVICE_URL=http://reporting-service:8000
         - RABBITMQ_MANAGEMENT_URL=http://guest:guest@rabbitmq:15672
         - REDIS_HOST=redis
         - REDIS_PORT=6379
         - JWT_SECRET_KEY=${JWT_SECRET_KEY}
         - FRONTEND_URL=${FRONTEND_URL:-*}
         - SPRING_PROFILES_ACTIVE=prod
       depends_on:
         redis:
           condition: service_healthy
         iam-service:
           condition: service_healthy
         patient-clinical-service:
           condition: service_healthy
       networks:
         - backend
     ```

Forneça os arquivos prontos e comandos para rebuild e subida controlada.
```

### Critérios de Aceite
- Imagem Docker final com tamanho inferior a 250MB.
- Subida do container com status `healthy` verificado pelo Docker Compose.

---

## Fase 8: Testes Automatizados Reativos com WebTestClient e Homologação

### Objetivo
Implementar a suíte de testes de integração com `WebTestClient` cobrindo roteamento reativo, bypass de rotas públicas, bloqueio de tokens na blacklist do Redis, rate limiting por IP/usuário, fallbacks de circuit breaker e testes de regressão dos endpoints do Gateway.

### Arquivos Alvo
- `backend-java/gateway/src/test/java/br/edu/ifrn/promptuario/gateway/GatewayRoutingIntegrationTest.java`
- `backend-java/gateway/src/test/java/br/edu/ifrn/promptuario/gateway/GatewaySecurityAndResilienceTest.java`

### Prompt da Fase 8

```text
Atue como Engenheiro de Qualidade de Software (QA/SDET) especializado em Testes Reativos com Spring Cloud Gateway e WebTestClient.
Vamos criar a suíte de testes automatizados para validar que a nova implementação em Java mantém paridade total com o Gateway anterior.

1. "GatewayRoutingIntegrationTest.java" em "src/test/java/...":
   - Utilizar @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT).
   - Injetar WebTestClient.
   - Mockar serviços downstream usando WireMock ou MockWebServer reativo:
     * Teste 1: GET /healthz deve responder HTTP 200 com status "ok".
     * Teste 2: GET /healthz/services deve disparar requisições para os 4 serviços e retornar status agregado correto e aliases legados.
     * Teste 3: Rota pública POST /api/v1/auth/login deve ser encaminhada para o IAM sem exigir cabeçalho Authorization.
     * Teste 4: Rota protegida GET /api/v1/patients sem token deve retornar HTTP 401 Unauthorized.
     * Teste 5: Rota protegida com token válido deve injetar os headers "X-User-Id" e "X-User-Role" na requisição repassada ao microsserviço.

2. "GatewaySecurityAndResilienceTest.java":
   - Teste de Token Blacklist:
     * Inserir token no Redis de teste: "blacklist:<token>".
     * Enviar requisição com esse token.
     * Validar que o Gateway responde HTTP 401 com mensagem "Token revogado" sem repassar a chamada ao backend.
   - Teste de Rate Limiting:
     * Executar requisições em loop ultrapassando o limite configurado (ex: 35 requisições rápidas para anônimo).
     * Validar que as requisições excedentes recebem HTTP 429 com header "Retry-After".
   - Teste de Circuit Breaker e Fallback:
     * Simular falhas consecutivas ou timeout no serviço downstream.
     * Validar que o Gateway aciona o fallback retornando HTTP 503 com cabeçalho "X-Circuit-Breaker: open".

3. Executar a suíte de testes de integração do repositório em "backend/gateway/tests" (se houver scripts pytest externos) e validar que todas as asserções passam sem alterações no frontend.

Forneça os arquivos de teste completos com asserções detalhadas e comandos de execução Maven.
```

### Critérios de Aceite
- 100% dos testes com `WebTestClient` passando com sucesso no `mvn clean test`.
- Chamadas originadas do Frontend React continuam funcionando sem necessidade de alteração de endpoints ou configurações de cliente.

---

## Matriz de Rastreabilidade e Riscos Técnicos

| Risco Identificado | Severidade | Impacto | Medida Mitigatória nos Prompts |
| :--- | :--- | :--- | :--- |
| **Conflito de runtime Netty vs Tomcat** | Crítica | A aplicação não inicia | A Fase 1 proíbe explicitamente a inclusão de `spring-boot-starter-web`. |
| **Bloqueio de threads de I/O** | Alta | Queda brusca de throughput e travamento do Gateway | Uso estrito de operadores reativos (`Mono`/`Flux`) e `ReactiveStringRedisTemplate` sem chamadas `.block()`. |
| **Bypass indevido de rotas protegidas** | Crítica | Acesso não autenticado a dados sensíveis de pacientes | A Fase 2 e 3 definem lista explícita de exceções (allow-list) e fecham todas as demais rotas por padrão. |
| **Incompatibilidade do segredo JWT** | Alta | Rejeição de todos os tokens emitidos pelo IAM | A Fase 1 e 3 forçam a decodificação HMAC-SHA256 utilizando a variável de ambiente compartilhada `JWT_SECRET_KEY`. |
| **Perda de cabeçalhos de rastreabilidade** | Média | Dificuldade de correlação de logs no Loki/Grafana | A Fase 3 implementa o `RequestTracingGlobalFilter` que propaga ou cria o `X-Correlation-Id`. |

---

## Comandos Rápidos de Validação

```bash
# 1. Compilar o Gateway em Java:
cd backend-java/gateway
mvn clean package -DskipTests

# 2. Executar a suíte de testes reativos:
mvn test

# 3. Subir o Gateway no Docker Compose:
cd ../../backend
docker compose up -d --build gateway

# 4. Validar o health check do Gateway:
curl -i http://localhost:8000/healthz

# 5. Validar o health check agregado de todos os microsserviços:
curl -s http://localhost:8000/healthz/services | jq .

# 6. Testar rejeição de rota protegida sem token:
curl -i http://localhost:8000/api/v1/patients
```
