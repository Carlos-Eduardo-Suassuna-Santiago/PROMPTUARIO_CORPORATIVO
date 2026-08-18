# 🛠️ Ferramentas e Tecnologias Utilizadas

Este documento consolida o *Stack Tecnológico* oficial do sistema **PROMPTUÁRIO**. O sistema foi projetado sob uma arquitetura baseada em microsserviços, orientada a eventos e focada em escalabilidade, resiliência e boas práticas modernas de desenvolvimento.

---

## 1. Frontend (Interface do Usuário)
A aplicação web foi desenvolvida como uma Single Page Application (SPA) reativa e de alta performance.

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **React 18** | Biblioteca base para a construção das interfaces de usuário. |
| **TypeScript** | Adiciona tipagem estática ao JavaScript, garantindo segurança e melhor refatoração do código. |
| **Vite** | Ferramenta de build incrivelmente rápida usada no lugar do Create React App/Webpack. |
| **Tailwind CSS** | Framework CSS utilitário para estilização rápida e design responsivo. |
| **Zustand** | Gerenciamento de estado global leve e sem boilerplate (usado principalmente para gerir a Autenticação e Sessões). |
| **TanStack Query (React Query)** | Gerenciamento de estado assíncrono (fetching, cacheamento, sincronização de requisições de API). |
| **React Router v6** | Roteamento do *client-side*, permitindo navegação sem recarregar a página e *AuthGuards* (rotas protegidas). |
| **Lucide React** | Biblioteca moderna de ícones SVG com design consistente. |
| **Vitest / React Testing Library** | Framework de testes unitários para a interface. |
| **Playwright** | Framework para testes *End-to-End* (E2E), simulando ações reais de usuário no navegador. |

---

## 2. Backend (Microsserviços e API)
O backend é composto por 6 microsserviços independentes e orientados a eventos, todos desenvolvidos em Python moderno.

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **Python 3.12** | Linguagem principal do backend. |
| **FastAPI** | Framework web assíncrono e de altíssima performance para construir as APIs REST. Utilizado em todos os serviços. |
| **Pydantic** | Validação de dados rigorosa e serialização. |
| **SQLAlchemy (Async)** | ORM assíncrono padrão para comunicação com bancos SQL. |
| **AsyncPG** | Driver de banco de dados ultrarrápido para PostgreSQL. |
| **Celery** | Gerenciador de tarefas em segundo plano (background workers). Usado para geração de relatórios e IA pesada. |
| **Pytest** | Framework de testes unitários e de integração do backend. |
| **Uvicorn** | Servidor ASGI padrão para rodar as aplicações FastAPI em produção e desenvolvimento. |

---

## 3. Bancos de Dados e Armazenamento (Storage)
Estratégia de persistência poligota, usando o melhor banco para a necessidade específica de cada microsserviço.

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **PostgreSQL 15** | Banco de Dados relacional. Serviços como IAM, Patient, Clinical e Reporting possuem instâncias ou *schemas* logicamente isolados. |
| **MongoDB 7** | Banco NoSQL orientado a documentos. Usado exclusivamente no `ai-service` para flexibilidade no armazenamento dos dados não-estruturados gerados pela LLM (Language Model). |
| **Redis 7** | Armazenamento em memória ultra-rápido. Usado para: Blacklist de JWT (Logout), Rate Limiting, e como *Broker* de mensagens do Celery. |
| **MinIO** | Servidor de *Object Storage* compatível com a API do **Amazon S3**. Usado para armazenar PDFs, laudos, prescrições e backups. |

---

## 4. Mensageria e Orientação a Eventos
A espinha dorsal para a comunicação assíncrona que mantém os microsserviços desacoplados.

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **RabbitMQ 3.13** | *Message Broker* (mensageria). Quando um paciente é criado no `iam-service`, o evento é publicado no RabbitMQ e lido pelo `patient-service`, evitando dependência síncrona. |

---

## 5. Observabilidade e Monitoramento
Conjunto de ferramentas para garantir que a saúde da infraestrutura e dos fluxos do sistema estejam sempre visíveis.

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **Prometheus** | Banco de dados Time-Series focado no *scraping* (coleta) de métricas de todos os microsserviços, RabbitMQ e Node (host). |
| **Grafana** | Plataforma de visualização visual com *Dashboards* que lêem os dados do Prometheus para exibir consumo de CPU, erros 500, e fluxos de mensagens. |
| **Jaeger** | Ferramenta de Tracing (Rastreamento Distribuído). Permite enxergar a jornada de uma requisição desde que entra no API Gateway até o banco de dados de um serviço no fundo da rede. |
| **OpenTelemetry (OTEL)** | Agente coletor de telemetria padronizado pelo qual as aplicações Python enviam seus rastreios (logs/traces). |
| **Mailpit** | Servidor SMTP local (Fake SMTP) focado no ambiente de desenvolvimento para interceptar e validar envios de e-mail sem precisar enviar e-mails de verdade. |

---

## 6. Infraestrutura, CI/CD e Deploy

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **Docker & Docker Compose** | Containerização completa. Todos os serviços são executados em contêineres isolados, garantindo que "se funciona localmente, funciona em produção". |
| **GitHub Actions** | Pipeline CI/CD. Executa automaticamente verificação de código (Lint), Testes e o *Deploy* contínuo via SSH. |
| **AWS EC2 (Ubuntu)** | Instância de nuvem virtual na qual o backend e todos os contêineres de dados/mensageria são hospedados. |
| **Vercel** | Provedor de hospedagem serverless ultrarrápida onde a interface do usuário (Frontend React) está hospedada. |
| **DuckDNS** | Provedor de DNS dinâmico gratuito que roteia a comunicação da AWS caso o IP dinâmico sofra alterações. |
| **Caddy** | Servidor Web e Reverse Proxy no host da AWS que provê renovação automática de certificados SSL (HTTPS Let's Encrypt) e roteamento de tráfego nativo. |

---

## 7. Segurança

| Tecnologia | Descrição / Uso no Projeto |
| :--- | :--- |
| **JWT (JSON Web Tokens)** | Sistema *stateless* principal para Autenticação. |
| **OAuth 2.0 / OpenID Connect** | Implementado para fluxo de Login Social via **Google**. |
| **RBAC** | *Role Based Access Control*, controle rígido de autorização separando regras para `ADMIN`, `DOCTOR` e `PATIENT` (incluindo permissões em nível de campos de banco de dados). |
