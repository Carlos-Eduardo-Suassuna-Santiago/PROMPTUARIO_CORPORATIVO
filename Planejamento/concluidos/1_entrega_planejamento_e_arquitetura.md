# DOCUMENTO UNIFICADO: 1ª ENTREGA — PLANEJAMENTO E ARQUITETURA

**Informações Gerais da Entrega**
* **Data sugerida:** 29/09/2026
* **Formato:** Apresentação de 15 a 20 minutos e entrega do documento com antecedência.
* **Objetivo principal:** Apresentar o tema, validar o escopo global do projeto e defender as escolhas técnicas metodológicas antes da fase de construção estrutural.

---

## MÓDULO 1: Desenvolvimento de Sistemas Corporativos (DSC)

O objetivo desta seção é fornecer a base arquitetural e as definições técnicas do sistema corporativo **PROMPTUÁRIO**.

---

### 1. Visão Geral

#### 1.1 Problema, Justificativa e Público-Alvo
* **Problema:** A área de saúde enfrenta grave fragmentação de registros clínicos em prontuários físicos ou sistemas legados isolados, ocasionando lentidão no atendimento emergencial, perda de histórico vacinal e medicamentoso, e risco elevado de prescrições incompatíveis (interações medicamentosas adversas).
* **Justificativa:** A criação de uma plataforma distribuída corporativa com suporte à Inteligência Artificial e orientada a eventos permite interoperabilidade em tempo real, integridade imutável de prontuários médicos, auditoria estrita e auxílio imediato à tomada de decisão clínica.
* **Público-Alvo:** 
  - **Médicos e Especialistas:** Acesso seguro a prontuários, emissão de prescrições e laudos assinados, recebimento de alertas de interação medicamentosa via IA.
  - **Atendentes e Recepcionistas:** Gestão de cadastro de pacientes, agendamentos, recepção e triagem ambulatorial.
  - **Pacientes:** Consulta ao histórico de atendimentos, receitas emitidas, carteira de vacinação e controle de consentimento LGPD.
  - **Administradores e Gestores:** Gestão de usuários, controle de permissões (RBAC), auditoria de operações sensíveis e relatórios operacionais.

#### 1.2 Objetivos do Sistema e Declaração de Escopo

##### Escopo Funcional Positivo (O que o sistema entrega)
1. **Ponto Único de Entrada e Resiliência (API Gateway):** Roteamento reverso, validação de tokens JWT, controle de vazão (rate limiting) e circuit breaker.
2. **Identidade e Acesso Corporativo (IAM Service - Java / Spring Boot):** Autenticação stateless, refresh tokens, login social OAuth2 (Google), revogação de tokens em Redis e RBAC multinível.
3. **Gestão Cadastral e Dados Sensíveis (Patient Service - Java / Spring Boot):** Cadastro demográfico, acompanhamento de histórico clínico, alergias, vacinas e direitos do titular LGPD (anonimização e soft delete).
4. **Prontuário e Atendimento Clínico (Clinical Service - Java / Spring Boot):** Agendamento e gestão de consultas, prontuário eletrônico imutável com versionamento histórico auditado, emissão de prescrições médicas digitais em PDF no object storage e solicitação de exames.
5. **Microsserviço de Alto Custo (AI Service - Python / FastAPI):** Processamento assíncrono com modelos de linguagem para análise de interações medicamentosas, detecção de sintomas críticos e sumarização de prontuários.
6. **Relatórios e Processamento em Segundo Plano (Reporting Service - Java / Spring Boot):** Agendadores assíncronos e workers para geração de relatórios gerenciais e epidemiológicos em PDF, CSV e JSON.
7. **Observabilidade Corporativa:** Coleta de métricas no Prometheus via Spring Boot Actuator e Micrometer, dashboards executivos no Grafana, rastreamento distribuído com Jaeger/OpenTelemetry e centralização de logs no Loki.

#### 1.2 Perfis de Usuário e Matriz de Permissões (RBAC)
O sistema implementa 4 perfis corporativos rigorosamente segregados via tokens JWT e políticas no banco de dados:

| Perfil | Descrição e Atribuições | Permissões no Sistema |
| :--- | :--- | :--- |
| **ADMIN** | Gestor do sistema e conformidade | Acesso irrestrito a gestão de usuários, auditoria completa, visualização de métricas e saúde global da plataforma. |
| **DOCTOR** | Corpo clínico e especialistas médicos | Leitura e escrita de prontuários, emissão de prescrições, solicitação de exames, solicitação de análise de IA para pacientes sob atendimento. |
| **ATTENDANT** | Equipe de apoio e recepção | Cadastro e atualização cadastral básica de pacientes, agendamento e cancelamento de consultas, confirmação de presença. Sem acesso a prontuários e diagnósticos. |
| **PATIENT** | Paciente titular dos dados | Acesso exclusivo aos próprios dados de perfil, agendamentos futuros, prescrições recebidas e solicitação de revogação/anonimização (LGPD). |

---

### 2. Modelagem

#### 2.1 Diagrama de Casos de Uso e Histórias de Usuário
* **Diagrama de Casos de Uso**
* **Histórias de Usuário BDD**

#### 2.2 Modelo de Dados Inicial
* **Estratégia de Persistência Poliglota e Database per Service:** Para garantir desacoplamento total, cada microsserviço possui banco de dados próprio:
  - `iam_db` (PostgreSQL 15): Usuários, perfis, credenciais criptografadas (BCryptPasswordEncoder) e tokens de segurança.
  - `patient_db` (PostgreSQL 15): Dados demográficos do paciente, histórico de alergias, vacinas e consentimento LGPD.
  - `clinical_db` (PostgreSQL 15): Consultas, horários de médicos, prontuários eletrônicos com tabela de histórico imutável (`medical_record_histories`) e prescrições.
  - `reporting_db` (PostgreSQL 15): Metadados de relatórios solicitados, status de execução e links no storage.
  - `ai_db` (MongoDB 7): Armazenamento de documentos NoSQL não estruturados gerados pelas análises de IA (embeddings, insights, payloads JSON flexíveis).
---

### 3. Decisões Técnicas

#### 3.1 Stack Tecnológica e Justificativas de Engenharia
A stack corporativa adotada e documentada foi selecionada para garantir aderência corporativa total, robustez transacional e concorrência poliglota especializada:

| Camada | Tecnologia Adotada | Justificativa Técnica |
| :--- | :--- | :--- |
| **Frontend** | React 18 + TypeScript + Vite + Tailwind CSS | Single Page Application rápida, tipagem estática que previne bugs de interface, componentização modular e gerenciamento com Zustand / TanStack Query. |
| **Backend Core** | Java 21 (LTS) + Spring Boot 3.x | Padrão corporativo robusto para os serviços centrais (IAM, Patient, Clinical, Reporting, Gateway); ecossistema com suporte corporativo de longo prazo, Spring Security 6, Spring Data JPA e forte tipagem com Records. |
| **Microsserviço de IA** | Python 3.12 + FastAPI + Uvicorn | Event-loop assíncrono nativo para I/O não bloqueante, integração direta com os ecossistemas líderes de IA generativa, processamento de NLP/LLMs, embeddings e bibliotecas especializadas. |
| **Validação de Dados** | Jakarta Validation (`@Valid`) + Pydantic v2 | Jakarta Bean Validation com Java Records no backend core Java e Pydantic v2 no microsserviço de IA Python, garantindo integridade estrita de schemas em ambas as pontas. |
| **Persistência Relacional** | PostgreSQL 15 + Spring Data JPA / Hibernate 6 + Flyway | Banco relacional corporativo com isolamento de transações ACID estrito, mapeamento ORM de alta produtividade e versionamento automatizado de schemas via migrações Flyway. |
| **Persistência NoSQL (IA)** | MongoDB 7 (via Spring Data MongoDB / Motor) | Armazenamento flexível e sem esquema rígido para outputs analíticos, embeddings vetoriais e logs de execução dos modelos de IA. |
| **Mensageria Corporativa** | RabbitMQ 3.13 (AMQP) | Broker orientado a eventos para comunicação assíncrona entre os microsserviços Java e o microsserviço Python via Spring AMQP (`RabbitTemplate`) e bibliotecas AMQP Python, com Topic Exchanges, Dead Letter Exchanges (DLX) e filas de contingência (DLQ). |
| **Cache e Sessões**| Redis 7 | Operações em memória sub-milissegundo para revogação imediata de JWT (blacklist), rate limiting e cache de dados de leitura frequente. |
| **Storage S3** | MinIO (Object Storage) | Armazenamento seguro de objetos (PDFs de prescrições e laudos) com compatibilidade nativa com a API AWS S3. |
| **Observabilidade** | Prometheus + Grafana + Loki + Jaeger/OTEL | Coleta de telemetria completa (Métricas com Spring Boot Actuator/Micrometer, Logs e Traces) com rastreamento distribuído de ponta a ponta. |

#### 3.2 Alinhamento Curricular com DSC (Java 21 / Spring Boot 3 Definitivo)
A arquitetura do backend atende integralmente e de forma definitiva às diretrizes curriculares da disciplina de Desenvolvimento de Sistemas Corporativos (DSC):
* **Adoção Oficial de Java 21 e Spring Boot 3.x:** Todos os microsserviços do núcleo transacional e de gestão corporativa (`iam-service`, `patient-service`, `clinical-service`, `reporting-service` e roteamento de borda) são construídos sobre Java 21 e Spring Boot 3.x, aplicando os padrões oficiais da disciplina:
  - Inversão de Controle e Injeção de Dependências com o ecossistema Spring Framework.
  - Segurança corporativa e RBAC com **Spring Security 6** e filtros stateless para validação de tokens JWT.
  - Persistência e controle transacional ACID via **Spring Data JPA** e **Hibernate**.
  - Versionamento evolutivo de banco de dados com **Flyway**.
  - Comunicação assíncrona orientada a eventos através de **Spring AMQP (`RabbitTemplate` e `@RabbitListener`)**.
  - Testes corporativos com **JUnit 5**, **Mockito** e **Testcontainers**.
* **Arquitetura Poliglota Justificada (Python para IA):** Mantém-se o microsserviço de alto custo (`ai-service`) em Python 3.12 com FastAPI devido à maturidade dos SDKs de inteligência artificial, conectando-se ao ecossistema corporativo Java via mensagens AMQP padronizadas no RabbitMQ. Dessa forma, atende-se tanto às exigências da disciplina corporativa quanto ao estado da arte no domínio de IA.

#### 3.3 Microsserviço de Alto Custo e Escalabilidade Isolada
* **Serviço Isolado:** O **`ai-service`** e os workers em background do **`reporting-service`** foram desacoplados da aplicação principal.
* **Justificativa de Isolamento:** 
  - A inferência de modelos de IA e a compilação de relatórios analíticos volumosos em PDF consomem intensamente CPU e memória por períodos prolongados (de 2 a 15 segundos por solicitação).
  - O isolamento impede que picos de requisições de IA travem as rotas de atendimento ambulatorial imediato (como busca de pacientes e confirmação de consultas no `clinical-service`).
  - O microsserviço escala horizontalmente de forma independente via `docker compose up --scale ai-service=3` ou workers dedicados, comprovando elasticidade corporativa.

#### 3.4 Mensageria: Desenho do Fluxo e Resiliência a Falhas (DLQ)
* **Desenho do Fluxo:**
  - O `clinical-service` ou `iam-service` publica eventos (ex.: `patient.created`, `medical_record.finalized`, `ai.analysis.requested`) em uma **Topic Exchange** (`promptuario_exchange`).
  - Filas exclusivas de cada microsserviço consumidor recebem cópias das mensagens com base no routing key.
* **Tratamento de Falhas e Dead Letter Queue (DLQ):**
  - Todas as filas possuem configuração de política de Dead Letter Exchange (`dlx_direct`).
  - Se um consumidor falhar ao processar a mensagem após 3 tentativas automáticas (com backoff exponencial), a mensagem é rejeitada (`nack(requeue=False)`) e redirecionada para a fila de descarte e auditoria (`promptuario.dlq`), evitando travamento de filas e perda de dados clínicos.

#### 3.5 Segurança, Autenticação e Conformidade com a LGPD
* **Autenticação e Autorização:**
  - Tokens JWT assinados com algoritmo HMAC-SHA256 ou RSA, contendo claims mínimas (`sub`, `roles`, `exp`).
  - Blacklist em Redis para revogação imediata de tokens no encerramento de sessão (Logout).
  - Controle de acesso granular baseado em papéis (RBAC) validado pelo API Gateway e inspecionado nos serviços.
* **Privacidade e Proteção de Dados (LGPD):**
  - Anonimização irreversível de dados pessoais por rotina segura no `patient-service`.
  - Exclusão lógica (*Soft Delete*) preservando rastreabilidade legal sem expor os dados em consultas ativas.
  - Trilha de auditoria imutável registrando operador, data/hora, tipo de operação e IP de origem em qualquer alteração de prontuário.

---

### 4. Organização e Infraestrutura

#### 4.1 Estrutura do Repositório
O projeto está organizado em monorepo modular poliglota com limites estritos entre domínios:
```text
PROMPTUARIO_CORPORATIVO/
├── backend/
│   ├── gateway/           # API Gateway (Spring Cloud Gateway / Java 21, Redis, Resilience4j)
│   ├── iam-service/       # Microsserviço de Identidade (Java 21, Spring Boot 3, Spring Security 6)
│   ├── patient-service/   # Microsserviço de Pacientes LGPD (Java 21, Spring Boot 3, Spring Data JPA)
│   ├── clinical-service/  # Microsserviço Clínico e Prontuários (Java 21, Spring Boot 3, Hibernate 6)
│   ├── reporting-service/ # Microsserviço de Relatórios (Java 21, Spring Boot 3, Async Jobs)
│   ├── ai-service/        # Microsserviço de IA Assíncrona (Python 3.12, FastAPI, LangChain, Motor)
│   ├── shared/            # Contratos e Eventos compartilhados (Schemas DTO, AMQP Topics)
│   └── docker-compose.yml # Orquestração completa de contêineres do backend poliglota
├── frontend/              # Single Page Application React 18 + Vite + Tailwind
├── .diagramas/            # Diagramas visuais (C4 Níveis 1 a 4, DER, Casos de Uso)
├── DOCUMENTATION/         # Especificações técnicas e manuais por etapa
├── .github/workflows/     # Pipelines automatizados de CI/CD (GitHub Actions)
└── README.md              # Guia consolidado de inicialização rápida
```

#### 4.2 Docker Compose e Orquestração Completa
A infraestrutura sobe com um único comando (`docker compose up -d`) sem dependências locais instaladas na máquina do desenvolvedor ou da banca avaliadora:
- 6 Contêineres de microsserviços de backend.
- 4 Contêineres PostgreSQL isolados + 1 Contêiner MongoDB + 1 Contêiner Redis.
- 1 Contêiner RabbitMQ com Management UI pré-configurada.
- 1 Contêiner MinIO S3 Object Storage + utilitário de criação de buckets.
- 5 Contêineres de Observabilidade (Prometheus, Grafana, Loki, Jaeger, Alertmanager).

#### 4.3 Divisão Clara de Responsabilidades entre os 4 Integrantes da Equipe
Para assegurar governança rigorosa de engenharia e transparência na contribuição de cada estudante, os papéis e responsabilidades foram formalmente distribuídos:

| Integrante / Estudante | Papel Técnico Principal | Responsabilidades no Projeto | Módulos e Serviços de Foco |
| :--- | :--- | :--- | :--- |
| **Carlos Eduardo Suassuna Santiago** | **Líder Técnico & Arquiteto de Software** (Tech Lead) | Arquitetura global distribuída, contratos de API e eventos AMQP, implementação de microsserviços em Spring Boot 3, mensageria RabbitMQ e DLQ, infraestrutura Docker Compose. | API Gateway, `clinical-service`, Shared Events, Infraestrutura Docker |
| **Integrante 2 (Desenvolvedor Backend & IA)** | **Engenheiro de Backend & IA** (Backend Developer) | Implementação e sustentação do microsserviço de alto custo (`ai-service` em Python/FastAPI), persistência NoSQL em MongoDB, workers assíncronos e integração com modelos de IA. | `ai-service` (Python), MongoDB, Redis, mensageria AMQP de IA |
| **Integrante 3 (Desenvolvedor Frontend & UI/UX)** | **Engenheiro de Frontend** (Frontend Specialist) | Desenvolvimento da Single Page Application em React + TypeScript, componentes Tailwind, consumo das APIs REST via Gateway, gestão de rotas e telas de Prontuário e Agendamento. | `frontend/` (React, Vite, TanStack Query, Zustand) |
| **Integrante 4 (Engenheiro de QA, Segurança & DevOps)** | **Engenheiro de Qualidade & DevOps** (QA / SecOps Lead) | Automação de pipelines de CI/CD (GitHub Actions com Maven e Python), suíte de testes unitários e integração (JUnit 5/Mockito/Pytest), observabilidade e compliance LGPD. | `.github/workflows/`, Testes (JUnit/Pytest), Observabilidade, `iam-service`, `patient-service` |

##### Diretrizes de Trabalho no Git (Git Flow e Commits):
- **Branches de Funcionalidade:** Todo trabalho é desenvolvido em branches dedicadas (`feature/`, `fix/`, `test/`).
- **Revisão Obrigatória de Código:** É proibido push direto na branch `main`. Todo código entra via Pull Request (PR) com revisão obrigatória de no mínimo um outro membro da equipe.
- **Convenção de Commits:** Utilização rigorosa do padrão *Conventional Commits* (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`), garantindo rastreabilidade individual das contribuições.

#### 4.4 Pipeline de Integração Contínua (CI)
* **Ferramenta:** GitHub Actions
* **Gatilhos:** Disparado automaticamente a cada push ou abertura de Pull Request para as branches `main` e `develop`.
* **Etapas Automatizadas:**
  1. *Checkout* do repositório e configuração dos ambientes JDK 21 (Temurin) e Python 3.12.
  2. Compilação, validação de estilo e execução de testes dos microsserviços Java/Spring Boot com **Maven** (`mvn clean test`).
  3. Análise estática, linting com **Ruff** e execução de testes automatizados com **Pytest** no microsserviço de IA (`ai-service`).
  4. Geração consolidada de relatórios de cobertura de código (*Code Coverage* via JaCoCo e Coverage.py).
  5. Bloqueio automático de merge em caso de quebra de testes ou violações de cobertura mínima (80%).

---

## MÓDULO 2: Planejamento e Arquitetura de Testes (Teste de Software)

O planejamento de qualidade e testes do **PROMPTUÁRIO** assegura a confiabilidade das regras de negócio clínicas, a segurança de dados sensíveis (LGPD), a integridade da comunicação assíncrona por eventos e a comprovação empírica de escalabilidade do microsserviço isolado.

---

### 1. Escopo e Pirâmide de Testes

#### 1.1 Delimitação do Escopo de Testes

* **Dentro do Escopo de Testes (In-Scope):**
  - Regras de negócio de cada microsserviço isolado (agendamento com validação de conflito de agenda, antecedência mínima de 24h para cancelamento, cálculo de dosagens e versionamento imutável de prontuários em `medical_record_histories`).
  - Segurança e Autenticação: Ciclo completo de vida dos tokens JWT, rotação de refresh tokens, revogação com blacklist no Redis e restrições de acesso RBAC nos 4 papéis (`ADMIN`, `DOCTOR`, `ATTENDANT`, `PATIENT`).
  - Privacidade e LGPD: Anonimização criptográfica irreversível e soft delete de pacientes.
  - Mensageria e Resiliência Event-Driven: Publicação em Topic Exchanges do RabbitMQ, consumo distribuído e desvio automático para Dead Letter Queue (`promptuario.dlq`) em falhas recorrentes.
  - Jornadas Críticas E2E na Interface: Autenticação visual no navegador, agendamento pelo atendente, atendimento médico e geração de prescrição PDF.
  - Teste de Carga Preliminar: Avaliação do comportamento do API Gateway e do microsserviço de IA sob estresse com **k6**.

* **Taxativamente Fora do Escopo de Testes nesta Fase (Out-of-Scope):**
  - Testes de invasão destrutivos (Pentest agressivo / DDoS em massa) na infraestrutura de nuvem.
  - Testes de estresse físico em hardware de servidores (foco estrito em contêineres Docker padronizados).
  - Validação cruzada com operadoras de planos de saúde e bancos (faturamento TISS/TUSS excluído do escopo do produto).
  - Testes clínicos com pacientes reais em ambiente de produção médica.
  - Homologação governamental direta com barramentos federais (RNDS/DATASUS).

#### 1.2 Estratégia e Pirâmide de Testes Proporcional

A distribuição dos esforços de automação de testes segue a **Pirâmide de Testes Ágil**, balanceada para a arquitetura de microsserviços poliglota do sistema:

```
                      / \
                     /   \
                    / E2E \          -> 10% (Playwright)
                   /  UI   \            Testes de jornada ponta a ponta no browser
                  /---------\
                 / Integração\       -> 20% (Testcontainers / Spring Boot Test / RabbitMQ / DLQ)
                / e Mensageria\         Contratos entre serviços Java e IA, broker e bancos
               /---------------\
              / Testes Unitários\    -> 70% (JUnit 5 + Mockito / Pytest / Vitest)
             /   (Base Sólida)   \      Domínio Java/Spring, schemas, services, stores Zustand
            /---------------------\
```

| Nível | Proporção | Escopo e Foco | Defesa Baseada em Microsserviços |
| :--- | :---: | :--- | :--- |
| **Unitários** | **70%** | Classes de domínio Java, entidades JPA, Jakarta Validation, schemas do microsserviço de IA e stores Zustand no frontend. | Execução em milissegundos com mocks (Mockito/unittest.mock), sem dependência de rede, garantindo feedback imediato a cada commit. |
| **Integração & Mensageria** | **20%** | Repositories reais com PostgreSQL via Testcontainers, filas RabbitMQ (`RabbitTemplate` / AMQP), desvio DLQ e persistência MinIO. | Valida se os limites dos microsserviços e seus contratos AMQP permanecem íntegros sem falhas de integração. |
| **End-to-End (E2E / UI)** | **10%** | Simulação do fluxo clínico completo no navegador (Médico e Atendente). | Valida a experiência real do usuário; mantido enxuto devido ao custo de execução e manutenção. |

---

### 2. Ferramentas e Infraestrutura de Teste

#### 2.1 Stack de Ferramentas por Nível

| Nível / Finalidade | Ferramenta Adotada | Justificativa da Escolha |
| :--- | :--- | :--- |
| **Unitários Backend Core (Java)** | **JUnit 5 + Mockito + AssertJ** | Padrão da indústria corporativa para Java 21 / Spring Boot 3, com suporte a injeção de dependências para testes e asserções fluentes. |
| **Unitários Backend IA (Python)** | **Pytest + Pytest-Mock** | Padrão do ecossistema Python, fixtures assíncronas (`pytest-asyncio`) e relatórios de cobertura para o `ai-service`. |
| **Unitários Frontend** | **Vitest + React Testing Library** | Velocidade extrema nativa com Vite, compatibilidade com sintaxe Jest e foco em testes orientados ao comportamento do usuário. |
| **Integração Backend** | **Spring Boot Test + Testcontainers + RabbitMQ** | Inicialização sob demanda de contêineres Docker reais (PostgreSQL, MongoDB, RabbitMQ) para testes de integração 100% fidedignos e isolados. |
| **End-to-End (E2E)** | **Playwright** | Execução rápida em múltiplos navegadores (Chromium, Firefox, WebKit), suporte a testes headless e geração de vídeos/traces de execução. |
| **Carga e Performance** | **k6 (Grafana Labs)** | Scripts em JavaScript versionados no Git, execução leve com baixo consumo de memória e fácil integração aos dashboards do Grafana. |
| **Segurança e Lint** | **Spotless/Checkstyle + Ruff + Bandit** | Padronização e análise estática rigorosa para Java e Python, prevenindo vulnerabilidades de segurança e falhas de formatação. |

#### 2.2 Automação de Testes em CI (GitHub Actions)
* A cada commit ou abertura de Pull Request para `main` e `develop`, o workflow é disparado.
* O pipeline executa `mvn clean test` para os serviços Java e `ruff check .` + `pytest` para o serviço de IA.
* O merge é bloqueado automaticamente caso qualquer teste falhe ou a cobertura de código fique abaixo de 80%.

---

### 3. Critérios de Entrada (Entry) e Saída (Exit)

Para assegurar governança de engenharia e confiabilidade em cada etapa de validação, foram formalizadas as seguintes cláusulas:

#### 3.1 Testes Unitários
* **Critérios de Entrada:**
  - Código commitado na branch de feature/fix do Git.
  - Dependências resolvidas via Maven (`pom.xml`) e Pipenv/Poetry, com análise estática aprovada.
  - Fixtures e classes de teste com *mocks* de banco de dados e mensageria configurados.
* **Critérios de Saída:**
  - **100% dos testes unitários aprovados** (zero falhas e zero erros).
  - Cobertura de código (*Code Coverage*) **mínima de 80%** nas camadas de negócio (`services`, `domain` e `schemas`).
  - Relatório de cobertura gerado e versionado no pipeline.

#### 3.2 Testes de Integração e Mensageria (Filas e Broker)
* **Critérios de Entrada:**
  - Suíte de testes unitários 100% aprovada.
  - Infraestrutura inicializada via Docker Compose (PostgreSQL, MongoDB, RabbitMQ e Redis saudáveis).
  - Contratos de schemas OpenAPI e tópicos RabbitMQ devidamente definidos.
* **Critérios de Saída:**
  - 100% dos testes de integração passando nos endpoints REST e repositories.
  - Ciclo de publicação e consumo assíncrono validado ponta a ponta.
  - **Comprovação de resiliência e DLQ:** mensagens rejeitadas após 3 tentativas automáticas devem ser confirmadamente redirecionadas para a fila `promptuario.dlq` sem interromper os consumidores.

#### 3.3 Testes de Carga e Desempenho
* **Critérios de Entrada:**
  - Endpoints críticos e rotas do microsserviço assíncrono (`ai-service` e `reporting-service`) mapeados e operacionais.
  - Scripts de teste k6 versionados no repositório com rampa de carga (ramp-up) documentada.
  - Monitoramento ativo via Prometheus e Grafana.
* **Critérios de Saída:**
  - Execução estável com carga entre **50 e 100 Usuários Virtuais (VUs)** durante a janela estipulada.
  - **Latência de resposta p95 abaixo de 800 ms** no API Gateway.
  - **Taxa de erro (HTTP 5xx) inferior a 1.0%**.
  - Drenagem completa da fila do RabbitMQ após a cessação da carga em até 5 minutos.

---

### 4. Casos de Teste Iniciais Derivados dos Requisitos BDD

A tabela abaixo formaliza os casos de teste iniciais derivados diretamente do catálogo de histórias de usuários em padrão BDD:

| ID Teste | História Relacionada | Descrição do Cenário | Pré-condições | Passos de Execução | Resultado Esperado | Nível de Teste |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-AUTH-01** | H-AUTH-001 | Autenticação com credenciais válidas | Usuário cadastrado e ativo no banco | 1. Enviar requisição POST `/api/v1/auth/login` com email e senha corretos. | HTTP 200; Retorno de `access_token` e `refresh_token` válidos; claims com role correta. | Unit / Integ |
| **TC-AUTH-02** | H-AUTH-001 | Bloqueio de login com senha incorreta | Usuário cadastrado | 1. Enviar POST `/api/v1/auth/login` com senha inválida. | HTTP 401 Unauthorized; Log de auditoria registrando tentativa inválida. | Unit |
| **TC-AUTH-03** | H-AUTH-002 | Encerramento de sessão e Blacklist JWT | Usuário logado com token ativo | 1. Enviar POST `/api/v1/auth/logout`.<br>2. Reutilizar o mesmo token em rota protegida. | 1. HTTP 200 (Token adicionado à blacklist no Redis).<br>2. HTTP 401 Unauthorized imediato. | Integ |
| **TC-PAT-01** | H-PATIENT-001| Cadastro de paciente e publicação de evento | Operador com perfil ATTENDANT autenticado | 1. Enviar POST `/api/v1/patients` com dados válidos e CPF formatado. | HTTP 201 Created; Paciente persistido; Evento `patient.created` publicado no RabbitMQ. | Integ |
| **TC-PAT-02** | H-PATIENT-003| Anonimização LGPD de dados sensíveis | Paciente cadastrado com histórico | 1. Enviar POST `/api/v1/patients/{id}/anonymize` com justificativa legal. | HTTP 200; Nome, CPF e email substituídos por hashes irreversíveis; integridade clínica preservada. | Integ |
| **TC-CLIN-01**| H-APPOINTMENT-001 | Agendamento de consulta com conflito de horário | Médico com slot já ocupado às 14:00 | 1. Tentar agendar nova consulta para o mesmo médico no mesmo horário. | HTTP 409 Conflict; Mensagem amigável informando indisponibilidade de agenda. | Unit / Integ |
| **TC-CLIN-02**| H-APPOINTMENT-003 | Cancelamento com antecedência mínima de 24h | Consulta marcada para daqui a 4 horas | 1. Paciente solicita cancelamento da consulta via API. | HTTP 400 Bad Request; Exibição de política de cancelamento com menos de 24h. | Unit |
| **TC-CLIN-03**| H-RECORD-001 | Criação de prontuário por médico autorizado | Médico autenticado (role DOCTOR) | 1. Enviar POST `/api/v1/records` com diagnóstico CID-10 e prescrição. | HTTP 201 Created; Registro persistido; Histórico imutável de versão inicial gravado. | Integ |
| **TC-CLIN-04**| H-RECORD-002 | Imutabilidade e versionamento do prontuário | Prontuário já gravado anteriormente | 1. Médico envia atualização de evolução clínica do prontuário. | HTTP 200; Prontuário atualizado; Novo registro inserido na tabela `medical_record_histories` com timestamp e autor. | Integ |
| **TC-SEC-01** | H-USER-001 | Tentativa de contorno de RBAC por perfil não autorizado | Usuário autenticado como PATIENT | 1. Tentar acessar endpoint restrito GET `/api/v1/users` (privilégio de ADMIN). | HTTP 403 Forbidden; Acesso bloqueado pelo Gateway / middleware de segurança. | Unit / Integ |
| **TC-AI-01**  | H-AI-001 | Análise de interação medicamentosa assíncrona | Prontuário com dois fármacos incompatíveis | 1. Disparar evento de solicitação de análise clínica.<br>2. Worker do `ai-service` processa o payload. | Evento consumido; Alerta de interação grave registrado no banco NoSQL e retornado ao médico. | Integ / Mensageria |
| **TC-DLQ-01** | N/A (Resiliência) | Roteamento de mensagem corrompida para DLQ | Broker RabbitMQ ativo com DLX configurada | 1. Publicar mensagem com schema malformado na fila do serviço clínico.<br>2. Consumidor falha 3 vezes consecutivas. | Mensagem não trava a fila principal; Redirecionamento automático confirmado na fila `promptuario.dlq`. | Mensageria |
| **TC-E2E-01** | H-AUTH-001 + H-RECORD-001 | Fluxo completo do Médico (Login → Atendimento) | Frontend e Backend em execução | 1. Médico efetua login na tela web.<br>2. Seleciona paciente da fila.<br>3. Registra consulta e emite prescrição PDF. | Login com redirecionamento para o dashboard médico; PDF da receita gerado e acessível para download. | E2E (Playwright) |

---

### 5. Cenário Preliminar de Teste de Carga e Escalabilidade

Atendendo ao **Item 5 da especificação do Projeto Integrador**, foi estruturado o cenário oficial de teste de carga para comprovar a eficácia da separação do microsserviço assíncrono:

* **Ferramenta:** **k6** (script versionado em `tests/performance/`).
* **Cenário Avaliado:** Submissão massiva de requisições de análise clínica assíncrona para o API Gateway (`POST /api/v1/clinical/ai-analysis`), enfileiramento na fila RabbitMQ e processamento pelos workers do `ai-service`.
* **Critério de Aceitação Preliminar:**
  - Carga constante de 50 Usuários Virtuais (VUs) durante 3 minutos.
  - Tempo de resposta p95 da API de submissão no Gateway inferior a 600 ms (comprovando isolamento do usuário).
  - Taxa de erro HTTP 5xx menor que 1.0%.
  - Drenagem completa da fila do RabbitMQ em até 5 minutos após o encerramento do tráfego.
* **Experimento de Escala Horizontal:**
  - Comparação direta de desempenho entre **1 instância** vs. **3 instâncias** (`docker compose up --scale ai-service=3`).
  - Análise dos tempos de drenagem, vazão (req/s) e consumo de CPU/Memória correlacionados com as métricas do Grafana.

---

## MÓDULO 3: Gerência de Projetos (Alinhado ao Guia Oficial da Disciplina)

A governança do projeto **PROMPTUÁRIO** estrutura os processos de iniciação, planejamento, execução, monitoramento e controle para garantir que as entregas técnicas sejam concluídas com previsibilidade, transparência e rigor metodológico.

---

### 1. Bloco A · Iniciação (Peso: Objetivo e Escopo — 30%)

#### 1.1 Identificação do Projeto, Equipe e Liderança
* **Nome do Projeto:** PROMPTUÁRIO (Ecossistema Corporativo de Prontuário Eletrônico com IA).
* **Equipe de Desenvolvimento:** 4 integrantes (Carlos Eduardo Suassuna Santiago, Integrante 2, Integrante 3 e Integrante 4).
* **Gerência do Projeto:** Coordenação técnica e gerencial liderada por Carlos Eduardo Suassuna Santiago, com distribuição de papéis operacionais e ritos compartilhados.

#### 1.2 Problema, Objetivo e Valor do Produto
* **Problema:** A fragmentação de dados clínicos entre unidades de saúde resulta em diagnósticos lentos, prescrições com interações medicamentosas adversas e ausência de histórico consolidado do paciente.
* **Público-Alvo:** Médicos, atendentes hospitalares, gestores de saúde e pacientes.
* **Mudança Gerada:** Registro clínico centralizado e imutável, suporte automatizado à decisão médica via IA e desburocratização de consultas e agendamentos.
* **Justificativa e Valor:** Redução de até 30% no tempo de atendimento ambulatorial, mitigação de falhas de prescrição e conformidade com a LGPD.

#### 1.3 Projeto, Produto e Operação
* **O Projeto (Esforço Temporário):** Iniciativa de 1 semestre letivo (2026.2) focada no planejamento, modelagem arquitetural, desenvolvimento dos microsserviços, automação de testes e homologação acadêmica.
* **O Produto (Entregável de Software):** Solução corporativa composta por API Gateway, microsserviços Java 21 / Spring Boot 3 para o núcleo corporativo, microsserviço de alto custo em Python 3.12 / FastAPI para IA, broker RabbitMQ com DLQ, bancos dedicados (PostgreSQL e MongoDB), MinIO S3 e frontend React 18.
* **A Operação (Sustentação Contínua):** Rotina hospitalar contínua pós-entrega baseada em observabilidade (Prometheus/Grafana/Loki com Spring Actuator e Micrometer), rotinas de backup automatizado no MinIO, políticas de expurgo/anonimização LGPD e escala elástica de workers.

#### 1.4 Partes Interessadas (Stakeholders)
* **Corpo Clínico (Médicos):** Esperam rapidez no prontuário e precisão nos alertas de IA; ajudam fornecendo feedback clínico; atrapalham com resistência ao uso de prontuários digitais.
* **Atendentes e Recepção:** Esperam busca rápida de pacientes e agenda fluida; ajudam validando usabilidade; atrapalham ao cadastrar dados incompletos.
* **Pacientes:** Esperam privacidade (LGPD) e receitas legíveis; ajudam fornecendo dados corretos; atrapalham com absenteísmo sem cancelamento.
* **TI / Infraestrutura:** Esperam containers Docker padronizados e observabilidade; ajudam na estabilidade da nuvem; atrapalham se impuserem restrições de rede excessivas.
* **Banca Avaliadora:** Espera rigor técnico, prazos cumpridos e arquitetura sólida; ajuda com orientação de engenharia; atrapalha com mudanças bruscas de critérios.

#### 1.5 Restrições, Premissas e Viabilidade
* **Restrições:** Prazo improrrogável até o encerramento do semestre 2026.2; equipe estrita de 4 estudantes; orquestração autossuficiente via `docker compose up -d`.
* **Premissas:** Máquinas com ao menos 16GB de memória RAM para executar a suíte Docker completa; conectividade contínua para o pipeline CI do GitHub Actions; viabilidade de modelos de IA em contêineres dedicados.
* **Viabilidade:** Viabilidade técnica assegurada por monorepo modular, persistência poliglota corporativa e adoção estrita de Java 21 / Spring Boot 3 no núcleo com Python isolado para IA.

---

### 2. Bloco B · Planejamento em Nível Macro (Peso: Ciclo de Vida e Planejamento — 30%)

#### 2.1 Abordagem e Ciclo de Vida do Projeto
A equipe adotou a **Abordagem Híbrida (Preditiva + Ágil)**:
* **Preditiva:** Planejamento arquitetural antecipado, diagramas C4, contratos de API e marcos formais alinhados ao cronograma de avaliações do curso.
* **Ágil:** Execução quinzenal (Sprints de 2 semanas), priorização de histórias de usuário em padrão BDD, testes automatizados e integração contínua (CI).
* **Justificativa:** Garante a estabilidade e a conformidade exigidas em sistemas médicos corporativos sem abdicar da adaptabilidade e velocidade dos ritos ágeis.

#### 2.2 Fases, Marcos, Entregas e Folgas Planejadas
O cronograma incorpora **folgas de segurança de 1 semana** ao final de cada fase macro para absorver eventualidades comentadas em sala de aula:
* **Fase 1 (Semanas 1 a 5): Concepção e Arquitetura.**  
  * *Entregas:* Diagramas C4, DER lógico, planejamento de testes e pipeline de CI.  
  * **Marco 1:** 1ª Entrega Integrada (DSC + TS + GP).  
  * *Folga 1:* 1 semana de reserva para ajustes de documentação pós-banca.
* **Fase 2 (Semanas 6 a 10): Construção do Núcleo Transacional.**  
  * *Entregas:* Gateway, IAM (JWT/OAuth), Patient (LGPD) e Clinical (Prontuários e Agenda) em Java/Spring Boot.  
  * **Marco 2 (Checkpoint 1):** Demonstração do MVP clínico e persistência integrada.  
  * *Folga 2:* 1 semana de reserva para estabilização de banco e testes unitários.
* **Fase 3 (Semanas 11 a 14): Mensageria, IA e Relatórios.**  
  * *Entregas:* Broker RabbitMQ com Dead Letter Queue, `ai-service` (Python/FastAPI) e workers de relatório.  
  * **Marco 3 (Checkpoint 2):** Demonstração da plataforma assíncrona orientada a eventos.  
  * *Folga 3:* 1 semana de reserva para testes de carga preliminares com k6 e resiliência de filas.
* **Fase 4 (Semanas 15 a 17): Homologação, Observabilidade e Defesa Final.**  
  * *Entregas:* Painéis Grafana, testes de carga comparativos (1 vs 3 instâncias), manual e homologação final.  
  * **Marco 4:** Defesa e Demonstração Final perante a Banca Examinadora.

#### 2.3 Matriz RACI e Gestão de Riscos Iniciais

| Integrante | Papel Técnico | Responsabilidades Principais |
| :--- | :--- | :--- |
| **Carlos Eduardo S. Santiago** | **Tech Lead & Arquiteto** | Gateway, `clinical-service` (Spring Boot), RabbitMQ, Docker Compose, Coordenação Geral e Aprovação de PRs. |
| **Everton Gean** | **Engenheiro Backend & IA** | Microsserviço de IA (Python/FastAPI), persistência MongoDB, mensageria de IA e relatórios. |
| **Douglas Lopes** | **Engenheiro Frontend** | Interface SPA React 18, Vite, rotas protegidas e telas clínicas. |
| **Eduardo Pereira** | **Engenheiro de QA & DevOps** | CI/CD GitHub Actions (Maven/Python), suíte de testes (JUnit/Pytest), observabilidade e compliance LGPD. |

##### Tabela dos 4 Riscos Críticos do Projeto:
1. **Risco 1 (Alinhamento de Stack Curricular de DSC):** Exigência acadêmica de Java 21 / Spring Boot 3.  
   * *Resposta:* Totalmente mitigado pela arquitetura oficial adotada: núcleo corporativo construído em **Java 21 e Spring Boot 3.x** e apenas o microsserviço de IA preservado em **Python 3.12** por justificativa técnica especializada de ecossistema.
2. **Risco 2 (Sobrecarga de CPU no Serviço de IA):** Lentidão na inferência de modelos de linguagem.  
   * *Resposta:* Desacoplamento assíncrono por filas RabbitMQ e escalabilidade horizontal isolada (`docker compose up --scale ai-service=3`).
3. **Risco 3 (Quebra de Contratos entre Serviços Poliglotas):** Alterações de schemas quebrando a comunicação entre serviços Java e IA.  
   * *Resposta:* Schemas DTO e eventos padronizados em `backend/shared`, contratos OpenAPI tipados e smoke tests automatizados no CI.
4. **Risco 4 (Desequilíbrio de Commits no Git):** Concentração de trabalho em um único membro.  
   * *Resposta:* Adoção obrigatória de Git Flow, branches individuais e revisão de código por pares via PR.

---

### 3. Bloco C · Execução (Peso: Execução e Condução — 20%)

#### 3.1 Condução do Trabalho: Rotina e Ferramentas
* **Ritos e Rotinas:** Sprints quinzenais, alinhamento semanal presencial/Meet para desbloqueio de dependências e comunicação contínua via Discord e WhatsApp.
* **Ferramentas:** GitHub Projects (quadro Kanban: *Backlog*, *To Do*, *In Progress*, *Code Review*, *Done*), GitHub para versionamento com *Conventional Commits* e Docker Compose com Makefile para automação local.

#### 3.2 Definição de "Pronto" (Definition of Done - DoD)
Uma tarefa só é aprovada quando cumprir rigorosamente 5 critérios:
1. Contrato OpenAPI cumprido e integrado ao API Gateway.
2. Testes automatizados passando (unitários e fumaça) com cobertura mínima de 80%.
3. Pipeline de CI/CD do GitHub Actions verde (compilação Maven, lint e testes aprovados).
4. Execução estável e com health check saudável via Docker Compose.
5. Documentação técnica, schemas e logs de auditoria atualizados.

#### 3.3 Plano de Comunicação com Stakeholders
* **Professores / Orientador:** Reuniões quinzenais presenciais/online com relatório executivo e demonstrações de progresso.
* **Equipe de Desenvolvimento:** Daily assíncrona no Discord, revisões de código via Pull Request e reuniões semanais.
* **Banca Avaliadora:** Documento consolidado executivo de até 4 páginas, repositório Git com README e apresentação final com demonstração ao vivo.

---

### 4. Bloco D · Monitoramento e Controle (Peso: Monitoramento e Controle — 20%)

#### 4.1 Indicadores de Controle Operacional, Métricas e Gatilhos
Foram definidos 3 indicadores objetivos de acompanhamento:
1. **IND-01: Desvio de Cronograma (Schedule Variance):** Mede a razão de Story Points entregues vs. planejados no GitHub Projects semanalmente.  
   * *Gatilho:* Atraso > 3 dias úteis na Sprint ou consumo > 50% da folga planejada da fase.  
   * *Ação Corretiva:* Repriorização imediata do backlog pelo gerente, poda de itens secundários (*nice-to-have*) e mutirão de pair programming.
2. **IND-02: Qualidade do Build e Cobertura (CI Health):** Mede a taxa de aprovação no GitHub Actions e cobertura de código consolidada (JaCoCo para Java e Coverage.py para Python) a cada PR.  
   * *Gatilho:* Cobertura < 80% ou 2 quebras seguidas de build no CI.  
   * *Ação Corretiva:* Bloqueio automático de merge na `main` e prioridade máxima da equipe para correção da suíte de testes.
3. **IND-03: Resiliência de Mensageria (DLQ Health):** Mede a quantidade de eventos não processados e retidos na fila `promptuario.dlq` via Prometheus.  
   * *Gatilho:* Mais de 3 mensagens acumuladas na DLQ durante rotinas de testes.  
   * *Ação Corretiva:* Interrupção dos testes de integração para depuração de payloads e correção imediata de parsing no consumidor.

#### 4.2 Fluxo Oficial de Controle de Mudanças de Escopo
Diante de qualquer solicitação de nova funcionalidade ou alteração de requisito:
1. **Registro:** Abertura formal de *Issue* no GitHub com prefixo `[Mudança de Escopo]`.
2. **Análise Multidimensional:** Avaliação conjunta de impacto em Escopo, Prazo (consumo de folga), Qualidade e Riscos.
3. **Decisão do Comitê:** O Gerente do Projeto e a equipe (em alinhamento com os orientadores) deliberam sobre a aprovação.
4. **Atualização:** Se aprovada, a mudança é adicionada ao GitHub Projects com recálculo de folga e atualização das documentações de arquitetura.