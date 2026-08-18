# PLANO DE TESTES - PROMPTUÁRIO

Este documento detalha todos os testes necessários para garantir a qualidade, resiliência e segurança do sistema PROMPTUÁRIO. Ele descreve a finalidade de cada tipo de teste, o que deve ser coberto e os comandos exatos para executá-los em ambiente local e em pipelines de CI/CD.

---

## 1. Testes Unitários (Unit Tests)

**Objetivo:** Isolar cada parte do código (funções, classes, componentes) e verificar se elas funcionam de forma independente.

### 1.1 Backend (Python / FastAPI)
A meta é atingir uma cobertura (coverage) **superior a 80%**. Os testes devem usar *mocks* para banco de dados e mensageria (RabbitMQ, Redis).

**O que deve existir:**
- Testes para regras de negócio (domínio) de cada serviço (`iam-service`, `patient-service`, etc.).
- Testes para funções utilitárias e middlewares no módulo `shared`.
- Validações de serializers (schemas Pydantic).

**Como executar:**
```bash
# Executar todos os testes unitários de um serviço específico
cd backend/iam-service
pytest tests/unit/

# Executar testes com relatório de cobertura (coverage)
pytest tests/ --cov=app --cov-report=term-missing --cov-report=html
```

### 1.2 Frontend (React / TypeScript)
Utiliza-se `vitest` com `@testing-library/react`.

**O que deve existir:**
- Testes para componentes UI isolados (ex: validação visual condicional de botões, modals).
- Testes para *hooks* customizados (ex: mutações do TanStack Query).
- Testes para funções utilitárias (ex: formatação de datas e validações Zod).
- Testes do *Zustand store* (estado global de autenticação).

**Como executar:**
```bash
# Entrar na pasta do frontend
cd frontend

# Rodar testes em modo watch (desenvolvimento)
npm run test

# Rodar testes com interface gráfica interativa
npm run test:ui

# Rodar testes de cobertura (necessário configurar c8 ou v8 no vitest)
npx vitest run --coverage
```

---

## 2. Testes de Integração (Integration Tests)

**Objetivo:** Verificar se a integração entre os diversos módulos do sistema (banco de dados reais, filas RabbitMQ, Redis) funciona corretamente.

### 2.1 Backend Integrado
**O que deve existir:**
- Fluxo completo de Login, JWT (geração, validação e *refresh*), incluindo blacklist no Redis.
- Criação de paciente propagando evento via RabbitMQ para os demais serviços.
- Geração de prontuário disparando a geração de prescições e análises por IA.
- Cancelamento de consultas e validação de regras de 24h.

**Como executar:**
```bash
# Subir infraestrutura de testes (bancos locais, rabbitmq e redis vazios)
cd backend
make infra-test-up  # (comando hipotético no Makefile)

# Rodar os testes de integração no serviço específico
cd patient-service
pytest tests/integration/

# Encerrar a infra
cd .. && make infra-test-down
```

---

## 3. Testes End-to-End (E2E)

**Objetivo:** Simular o comportamento do usuário real no navegador, navegando de ponta a ponta na aplicação.

### 3.1 Frontend + Backend E2E (Playwright)
**O que deve existir:**
- Login bem-sucedido com direcionamento por Role (médico, paciente, admin).
- Fluxo de agendamento de uma consulta por parte do atendente.
- Criação de prontuário e interação visual com a análise de IA.
- Geração e tentativa de download de relatórios.

**Como executar:**
```bash
# Entrar na pasta do frontend
cd frontend

# Executar testes em modo headless (usado no CI/CD)
npm run test:e2e

# Executar testes abrindo a UI do Playwright (ideal para debug visual)
npm run test:e2e:ui
```

---

## 4. Smoke Tests (Sanity)

**Objetivo:** Testes ultra-rápidos que apenas verificam se a aplicação ligou corretamente e o ambiente está saudável.

**O que deve existir:**
- *Healthchecks* de todos os serviços (Gateway, IAM, Patient, Clinical, AI, Reporting).
- Validação de que a infraestrutura (PostgreSQL, MongoDB, RabbitMQ, Redis, MinIO) aceita conexões.

**Como executar:**
```bash
cd backend
# Subir todos os serviços
make up

# Rodar script de verificação
make health
```
*(Alternativamente via CURL: `curl http://localhost:8000/healthz/services`)*

---

## 5. Testes de Carga e Performance (Load Testing)

**Objetivo:** Determinar os limites da aplicação antes de uma degradação severa, testando a resiliência do *API Gateway* e *Circuit Breakers*.

**O que deve existir:**
- Cenário simulando 100 usuários autenticando simultaneamente (teste de I/O no banco IAM e Redis).
- Cenário de criação em lote de prontuários (estressa banco e RabbitMQ).
- Teste de requisições de alto custo, como *Exportação de Relatórios* (Celery worker limit testing).

**Ferramenta recomendada:** **k6** (da Grafana) ou **Locust**.

**Como executar (Exemplo hipotético com K6):**
```bash
# Na raiz do projeto
k6 run tests/performance/load_test_login.js
k6 run tests/performance/stress_test_appointments.js
```

---

## 6. Testes de Segurança (Security Testing)

**Objetivo:** Garantir a conformidade com a LGPD e fechar brechas comuns da web (OWASP Top 10).

**O que deve existir:**
- Tentativa de contorno de RBAC: um `PATIENT` tentando forçar requisições exclusivas de `ADMIN`.
- Injeção de SQL/NoSQL em rotas de relatórios/buscas.
- Validação de expiração de token JWT e recusa da aplicação para tokens na Blacklist.
- Verificação de expurgo / mascaramento de logs sensíveis (PII) do paciente.

**Como executar:**
- Esses testes geralmente fazem parte da suíte de *Integration Tests* via Pytest, injetando payloads maliciosos intencionalmente.
- Podem-se executar análises estáticas (SAST) em CI/CD:
```bash
# Bandit (para Python)
bandit -r backend/

# npm audit (para Node/React)
cd frontend && npm audit
```
