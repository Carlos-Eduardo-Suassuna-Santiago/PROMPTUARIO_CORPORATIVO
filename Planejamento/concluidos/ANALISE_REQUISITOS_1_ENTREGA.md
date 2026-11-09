# Análise de Requisitos: 1ª Entrega — Planejamento e Arquitetura

> **Documentos de Referência:** `1_entrega_planejamento_e_arquitetura.md` e `Projeto-I_tegrador_gerencia_de_projeto.md`  
> **Sistema Avaliado:** PROMPTUÁRIO (Ecossistema Corporativo de Prontuário Eletrônico com IA)  
> **Data de Elaboração:** 21 de Setembro de 2026 (Atualizado com Guia Oficial de Gerência de Projetos)  
> **Status Geral da 1ª Entrega:** 🟡 **75% CONCLUÍDO** (Núcleo técnico de arquitetura superado; planejamento de gerência alinhado à ementa oficial de 4 páginas e pendente de compilação final).

---

## 📊 1. Resumo Executivo de Cumprimento

A tabela abaixo resume o status de atendimento para cada um dos três módulos que compõem a **1ª Entrega**, incorporando a estrutura e os pesos oficiais da disciplina de **Gerência de Projetos**:

| Módulo | Itens Avaliados | Cumpridos (🟢) | Parciais (🟡) | Pendentes (🔴) | % Aderência |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Módulo 1: Desenvolvimento de Sistemas Corporativos (DSC)** | 14 | 14 | 0 | 0 | **100.0%** |
| **Módulo 2: Planejamento e Arquitetura de Testes (TS)** | 8 | 8 | 0 | 0 | **100.0%** |
| **Módulo 3: Gerência de Projetos (GP — Guia Oficial de 4 Páginas)** | 14 | 14 | 0 | 0 | **100.0%** |
| **TOTAL GERAL** | **36** | **36** | **0** | **0** | **100.0%** |

### Destaques do Diagnóstico:
* **Conclusão Integral da 1ª Entrega (🟢 100%):** Todos os 3 módulos da 1ª Entrega do Projeto Integrador estão plenamente formalizados e documentados em conformidade com as diretrizes oficiais dos professores de DSC, Teste de Software e Gerência de Projetos.
* **Módulo 1 (DSC):** Arquitetura distribuída com C4 (níveis 1 a 4), monorepo com Docker Compose (20+ contêineres), delimitação positiva e taxativa do que está FORA DO ESCOPO, stack justificada com matriz de equivalência para Java 21 / Spring Boot 3 e plano de migração executivo (`PLANO_MIGRACAO_CLINICAL_SERVICE.md`).
* **Módulo 2 (Testes):** Escopo de testes formalizado, pirâmide proporcional ágil (70% unitários, 20% integração/mensageria e 10% E2E), critérios objetivos de entrada e saída (Entry/Exit) para cada nível, catálogo com 13 Casos de Teste BDD detalhados e protocolo de teste de carga com k6.
* **Módulo 3 (Gerência de Projetos):** Documento executivo oficial de 4 páginas compilado (`DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md`) cobrindo os 4 blocos avaliativos: Iniciação (30%), Planejamento Macro com linha do tempo e folgas de contingência (30%), Execução com Definition of Done e comunicação (20%), e Monitoramento com 3 indicadores operacionais e fluxo formal de controle de mudanças (20%).




---

## 🔍 2. Análise Detalhada por Módulo

---

### MÓDULO 1: Desenvolvimento de Sistemas Corporativos (DSC)

#### 1. Visão Geral
* **1.1 Problema, justificativa e público-alvo do projeto:**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está implementado/documentado:** Documentado em `README.md`, `DOCUMENTATION/Documento de Visao.pdf`, `DOCUMENTATION/DIAGRAMA_DE_CASOS_DE_USO.md` e `DOCUMENTATION/REQUISITOS_HISTORIAS_DE_USUARIOS.md`.  
  * **Evidência:** O problema da fragmentação de prontuários, falta de interoperabilidade e lentidão em diagnósticos é justificado; o público-alvo compreende Médicos, Atendentes, Pacientes e Administradores Hospitalares.

* **1.2 Objetivos do sistema e declaração de escopo (com itens fora do escopo):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `1_entrega_planejamento_e_arquitetura.md`, `DOCUMENTATION/DIAGRAMA_DE_CASOS_DE_USO.md` e `DOCUMENTATION/ARQUITETURA_DE_SOFTWARE.md`.  
  * **Evidência:** Formalizada lista taxativa e consolidada de **FORA DO ESCOPO** cobrindo: Faturamento e Glosas Médicas TISS/TUSS, Gestão Física de Hotelaria/Leitos de UTI, Dispositivos Médicos Embarcados IoT, Farmácia Hospitalar com Almoxarifado Físico, Prontuários Odontológicos/Veterinários, Servidor PACS/DICOM in-house e integração direta com RNDS/DATASUS na 1ª versão.

* **1.3 Definição dos perfis de usuário e suas respectivas permissões:**  
  * **Status:** 🟢 **Cumprido (Superado)**  
  * **Onde está implementado/documentado:** `DOCUMENTATION/ETAPA_3_Iam_Service_Fastapi_Production.md`, `DOCUMENTATION/DIAGRAMA_DE_CASOS_DE_USO.md` e implementado no código-fonte com 4 papéis RBAC (`ADMIN`, `DOCTOR`, `ATTENDANT`, `PATIENT`).

#### 2. Modelagem
* **2.1 Diagrama de casos de uso ou histórias de usuário priorizadas:**  
  * **Status:** 🟢 **Cumprido (Superado)**  
  * **Onde está implementado/documentado:** Diagrama visual presente em `.diagramas/Diagramas Sistema Promptuario-Caso de Uso.drawio.png` e levantamento detalhado de histórias com BDD, story points e prioridades em `DOCUMENTATION/REQUISITOS_HISTORIAS_DE_USUARIOS.md`.

* **2.2 Modelo de dados inicial (DER):**  
  * **Status:** 🟢 **Cumprido (Superado)**  
  * **Onde está implementado/documentado:** Arquivo visual `.diagramas/Diagramas Sistema Promptuario-Modelo Lógico.drawio.png` e documentações por microsserviço em `DOCUMENTATION/ETAPA_14_Modelo_Logico_*.md`.

* **2.3 Diagrama de arquitetura do sistema (Modelo C4, níveis 1 e 2):**  
  * **Status:** 🟢 **Cumprido (Superado)**  
  * **Onde está implementado/documentado:** Na pasta `.diagramas/` existem diagramas C4 completos cobrindo:
    * `Diagramas Sistema Promptuario-C4 nivel 1.drawio.png` (Contexto)
    * `Diagramas Sistema Promptuario-C4 nivel 2.drawio.png` (Contêineres)
    * `Diagramas Sistema Promptuario-C4 nivel 3.drawio.png` (Componentes)
    * `Diagramas Sistema Promptuario-C4 nível 4.drawio.png` (Código)

#### 3. Decisões Técnicas
* **3.1 Stack Tecnológica detalhada e justificada:**  
  * **Status:** 🟢 **Cumprido (100% com Alinhamento Acadêmico)**  
  * **Onde está documentado:** `1_entrega_planejamento_e_arquitetura.md`, `DOCUMENTATION/TECNOLOGIAS_UTILIZADAS.md` e `DOCUMENTATION/ARQUITETURA_DE_SOFTWARE.md`.  
  * **Evidência:** Stack detalhada e justificada (FastAPI, React, PostgreSQL, RabbitMQ, MongoDB, Redis, MinIO), complementada por uma **Matriz de Equivalência 1-para-1 com Spring Boot 3** e pelo plano executivo de migração/coexistência do núcleo clínico (`PLANO_MIGRACAO_CLINICAL_SERVICE.md` e `PROMPTS_EXECUCAO_MIGRACAO.md`), sanando qualquer divergência acadêmica perante a banca de DSC.

* **3.2 Arquitetura do microsserviço isolado e justificativa de escalabilidade:**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está documentado:** `DOCUMENTATION/ARQUITETURA_DE_SOFTWARE.md` e `DOCUMENTATION/ETAPA_6_Ai_Service_Fastapi_Async_Clean_Architecture.md`.  
  * **Evidência:** O `ai-service` e os workers Celery do `reporting-service` foram isolados para tarefas pesadas (processamento de linguagem natural e geração de relatórios), com justificativa clara de consumo de recursos computacionais e isolamento de falhas.

* **3.3 Mensageria: Desenho do fluxo e tratamento de falhas (DLQ):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está implementado/documentado:** `DOCUMENTATION/ETAPA_2_Eventos_Rabbitmq _Arquitetura_Event_Driven.md`, diagramas de sequência em `.diagramas/` e código em `backend/shared/events` com Dead Letter Exchanges (`dlx_direct`) e Dead Letter Queues (`dlq`).

* **3.4 Segurança e Privacidade (Autenticação/Autorização e LGPD):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está implementado/documentado:** `DOCUMENTATION/ETAPA_3_Iam_Service_Fastapi_Production.md` (JWT, Redis blacklist, RBAC) e `DOCUMENTATION/ETAPA_4_Patient_Service_Fastapi_Clean_Architecture.md` (anonimização, soft delete, consentimento LGPD).

#### 4. Organização e Infraestrutura
* **4.1 Repositório estruturado:**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está:** Estrutura organizada com pastas para backend, frontend, diagramas, ci e documentação técnica.

* **4.2 Docker Compose com infraestrutura básica:**  
  * **Status:** 🟢 **Cumprido (Superado)**  
  * **Onde está:** `backend/docker-compose.yml` e `frontend/docker-compose.yml` sobem bancos PostgreSQL, MongoDB, Redis, RabbitMQ, MinIO, e serviços de observabilidade.

* **4.3 Divisão clara de responsabilidades entre os 4 integrantes:**  
  * **Status:** 🟢 **Cumprido em Documentação e Planejamento**  
  * **Onde está:** Formalizado em `1_entrega_planejamento_e_arquitetura.md` e `DOCUMENTATION/ETAPA_13_Definicao_Do_Processo_De_Software.md`.  
  * **Evidência:** Matriz de responsabilidades com os 4 papéis formais da equipe (Tech Lead & Arquiteto, Engenheiro Backend & IA, Engenheiro Frontend & UI/UX, Engenheiro de QA & DevOps/Segurança), distribuição de microsserviços e diretrizes obrigatórias de Git Flow (branches de feature, PR com code review e Conventional Commits).  
  * *Observação Operacional:* Manter a prática diária de commits pelos 4 estudantes nas respectivas branches ao longo de todo o semestre.

* **4.4 Pipeline de Integração Contínua (CI):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está:** `.github/workflows/backend-ci.yml` configurado e executando lint com Ruff e testes com Pytest + coverage para todos os serviços a cada push/PR.

---

### MÓDULO 2: Planejamento e Arquitetura de Testes (Teste de Software)

#### 1. Escopo e Pirâmide de Testes
* **1.1 Delimitação do Escopo de Testes (o que será testado vs. fora de escopo):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/PLANO_DE_TESTES.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Formalizada fronteira clara com itens dentro do escopo (regras clínicas, contratos OpenAPI, JWT/RBAC, RabbitMQ com DLQ, testes E2E e carga com k6) e taxativamente fora do escopo (pentest invasivo, estresse em hardware físico real, validação TISS/TUSS, testes clínicos com pacientes reais e RNDS).

* **1.2 Estratégia e Pirâmide de Testes:**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/PLANO_DE_TESTES.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Pirâmide de testes formalizada com representação visual e proporções oficiais: 70% Unitários (Pytest/Vitest), 20% Integração e Mensageria (Pytest + Docker + RabbitMQ + DLQ) e 10% E2E / UI (Playwright), acompanhada de defesa técnica baseada no desacoplamento de microsserviços.

#### 2. Ferramentas e Infraestrutura de Teste
* **2.1 Stack de Testes por nível:**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está:** Pytest + Pytest-Mock (Unitários backend), Vitest + React Testing Library (Unitários frontend), Pytest + AsyncPG + Aio-pika (Integração), Playwright (E2E/UI), K6 (Carga), Ruff + Bandit + npm audit (Lint e SAST).

* **2.2 Automação (CI) para testes:**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está:** GitHub Actions integrado (`.github/workflows/backend-ci.yml`) executando suíte de testes unitários com matriz multi-serviço paralela e bloqueio de merge em falhas ou cobertura inferior a 80%.

#### 3. Critérios de Entrada (Entry) e Saída (Exit)
* **3.1 Critérios para Testes Unitários:**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/PLANO_DE_TESTES.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Cláusulas formais de Entrada (branch de trabalho ativa, lint com Ruff sem erros, fixtures e mocks configurados) e Saída (100% dos testes passando com zero falhas, cobertura mínima de 80% em services, domain e schemas).

* **3.2 Critérios para Testes de Integração e Mensageria:**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/PLANO_DE_TESTES.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Cláusulas de Entrada (testes unitários aprovados, infraestrutura Docker Compose ativa e contratos AMQP definidos) e Saída (100% de sucesso em endpoints e repositories, validação de publicação/consumo e evidência de desvio seguro para DLQ `promptuario.dlq` após 3 falhas).

* **3.3 Critérios para Testes de Carga e Desempenho:**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/PLANO_DE_TESTES.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Cláusulas de Entrada (endpoints mapeados, scripts k6 versionados e observabilidade Prometheus/Grafana ativa) e Saída (execução estável com 50-100 VUs, tempo de resposta p95 < 800ms, taxa de erro < 1% e fila RabbitMQ drenada em até 5 minutos).

#### 4. Casos de Teste Iniciais
* **4.1 Tabela de Testes Derivados das Histórias de Usuário:**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/PLANO_DE_TESTES.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Tabela com 13 Casos de Teste BDD contendo: `ID`, `História Relacionada`, `Descrição do Cenário`, `Pré-condições`, `Passos de Execução`, `Resultado Esperado` e `Nível de Teste` mapeados a partir de `DOCUMENTATION/REQUISITOS_HISTORIAS_DE_USUARIOS.md`.

---

### MÓDULO 3: Gerência de Projetos (Alinhado ao Guia Oficial da Disciplina)

> **Documento Base da Disciplina:** `Projeto-I_tegrador_gerencia_de_projeto.md`  
> **Diretrizes e Regras de Submissão:**
> * **Formato:** Equipe, documento único de **até 4 páginas (PDF ou DOCX)**, aplicado estritamente ao produto real do projeto integrador (`PROMPTUÁRIO`).
> * **Orientação de Escopo e Prazos:** Planejamento em nível macro e cronograma parcial com **possíveis folgas** (para eventualidades e contingências comentadas em sala de aula).
> * **Pesos e Critérios Oficiais de Avaliação:**
>   * **Objetivo e Escopo (30%)** — Bloco A (Iniciação)
>   * **Ciclo de Vida e Planejamento (30%)** — Bloco B (Planejamento em Nível Macro)
>   * **Execução (20%)** — Bloco C (Execução e Condução do Trabalho)
>   * **Monitoramento e Controle (20%)** — Bloco D (Indicadores, Gatilhos e Mudanças)

---

#### A · Iniciação (Peso: Objetivo e Escopo — 30%)

* **A.1 Identificação (Nome do projeto e equipe, gerente do projeto):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está implementado/documentado:** `README.md`, `DOCUMENTATION/Documento de Visao.pdf` e `DOCUMENTATION/ETAPA_13_Definicao_Do_Processo_De_Software.md`.  
  * **Evidência e Resposta:** Projeto *PROMPTUÁRIO* (Ecossistema Corporativo de Prontuário Eletrônico com IA). Equipe de 4 integrantes com gerência compartilhada/coordenada pelo líder técnico.

* **A.2 Problema, objetivo e valor (Qual situação real o produto resolve, para quem, que mudança gera e por que vale a pena):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está documentado:** `DOCUMENTATION/Documento de Visao.pdf`, `README.md` e `DOCUMENTATION/DIAGRAMA_DE_CASOS_DE_USO.md`.  
  * **Evidência e Resposta:** Resolve a fragmentação de prontuários médicos, a lentidão em triagens e a ausência de suporte computacional à decisão clínica. Atende Médicos, Atendentes, Gestores e Pacientes. Gera desburocratização no atendimento, segurança clínica e interoperabilidade orientada a eventos. O investimento se justifica pela redução de tempo de consulta em até 30% e mitigação de erros de prescrição.

* **A.3 Projeto, produto e operação (O que é o projeto, o que é o produto entregue e qual operação ele sustentará depois):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Formalizada distinção clara entre os três conceitos: Projeto (esforço temporário no semestre 2026.2 para concepção e entrega acadêmica), Produto (plataforma distribuída com API Gateway, 5 microsserviços, mensageria RabbitMQ e frontend React) e Operação (sustentação hospitalar contínua pós-projeto com observabilidade Prometheus/Grafana, backups no MinIO e compliance LGPD).

* **A.4 Escopo e fora do escopo (O que será entregue e o que não será):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md`, `DOCUMENTATION/DIAGRAMA_DE_CASOS_DE_USO.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Escopo funcional positivo detalhado por serviço e lista taxativa de 7 itens FORA DO ESCOPO (Faturamento TISS/TUSS, Leitos de UTI/Hotelaria, IoT embarcado, Farmácia física com estoque, Odonto/Veterinária, Servidor PACS/DICOM próprio e barramentos governamentais diretos no MVP).

* **A.5 Partes interessadas / Stakeholders (Quem são, o que esperam e como podem ajudar ou atrapalhar):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `DOCUMENTATION/Documento de Visao.pdf`.  
  * **Evidência e Resposta:** Matriz formalizada cobrindo Corpo Clínico (Médicos), Atendentes/Recepção, Pacientes, TI/Infraestrutura e Banca Acadêmica Examinadora.

* **A.6 Restrições, premissas e viabilidade (Prazo, equipe, tecnologia e por que vale começar):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Formalizadas restrições de prazo rígido no semestre 2026.2, equipe de 4 membros e deploy Docker autossuficiente; premissas de hardware (16GB RAM) e modelos open-source; e viabilidade técnica/econômica demonstrada pelo protótipo funcional.

---

#### B · Planejamento em Nível Macro (Peso: Ciclo de Vida e Planejamento — 30%)

* **B.1 Abordagem e ciclo de vida (Preditiva, ágil ou híbrida, justificada pelas características do produto):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `DOCUMENTATION/ETAPA_13_Definicao_Do_Processo_De_Software.md`.  
  * **Evidência e Resposta:** Abordagem Híbrida formalizada e justificada: modelo preditivo para arquitetura, modelo de dados e marcos institucionais fixos; modelo ágil (Scrum/Kanban) para iterações em sprints de 2 semanas, CI/CD e revisão de Pull Requests.

* **B.2 Fases, marcos e entregas (Linha do tempo do semestre: fases, marcos principais, entregas e folgas planejadas):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Linha do tempo estruturada em 4 fases e 4 marcos institucionais (Marco 1: 1ª Entrega Integrada; Marco 2: MVP Clínico/IAM; Marco 3: Plataforma Assíncrona com IA; Marco 4: Defesa Final), incorporando **folgas de segurança de 1 semana** ao final de cada fase para absorver eventualidades e contingências comentadas em sala de aula.

* **B.3 Papéis e riscos iniciais (Quem faz o quê; 3 a 5 principais riscos com causa provável e resposta prevista):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Matriz RACI formalizada com atribuições dos 4 estudantes e tabela estruturada com os 4 riscos prioritários (Divergência de stack Python vs Java com plano de migração, Sobrecarga de IA com isolamento assíncrono, Quebra de contratos com schemas compartilhados e Desequilíbrio de commits com Git Flow obrigatório).

---

#### C · Execução (Peso: Execução — 20%)

* **C.1 Condução do trabalho — Rotina e ferramentas:**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `DOCUMENTATION/ETAPA_13_Definicao_Do_Processo_De_Software.md`.  
  * **Evidência e Resposta:** Rotina com Sprints quinzenais, alinhamentos semanais presenciais/Meet, comunicação via Discord/WhatsApp, gestão de fluxo Kanban no GitHub Projects e automação de desenvolvimento com Docker Compose e Makefile.

* **C.2 Condução do trabalho — Definição de "Pronto" (Definition of Done - DoD com 3 a 5 critérios):**  
  * **Status:** 🟢 **Cumprido**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `DOCUMENTATION/ETAPA_13_Definicao_Do_Processo_De_Software.md`.  
  * **Evidência e Resposta:** 5 critérios de aceitação objetivos: contrato de API validado, testes unitários passando com cobertura >= 80%, pipeline de CI verde, execução estável em contêiner Docker e documentação/auditoria atualizadas.

* **C.3 Condução do trabalho — Comunicação com stakeholders (Quem, frequência e formato):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Matriz de comunicação formalizada especificando canais, periodicidade e formatos para Professores/Orientador (quinzenal presencial/relatório), Equipe Interna (diária assíncrona Discord/PRs), Banca Avaliadora (documento executivo de até 4 págs e apresentação ao vivo) e Usuários/Comunidade (README e documentação técnica).

---

#### D · Monitoramento e Controle (Peso: Monitoramento e Controle — 20%)

* **D.1 Indicadores e gatilhos (Mínimo de 3 indicadores: o que medir, como, frequência, indicador de desvio e ação correspondente):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** 3 indicadores objetivos formalizados com métricas, frequências, gatilhos de desvio e ações corretivas:
    1. *IND-01: Desvio de Cronograma (Schedule Variance):* Medição semanal no GitHub Projects. Gatilho: atraso > 3 dias ou consumo > 50% da folga. Ação: repriorização e pareamento.
    2. *IND-02: Qualidade do Build e Cobertura (CI Health):* Medição a cada PR via GitHub Actions e Pytest Coverage. Gatilho: cobertura < 80% ou 2 falhas consecutivas. Ação: bloqueio de merge na `main` e correção prioritária.
    3. *IND-03: Resiliência de Mensageria (DLQ Health):* Medição diária na fila `promptuario.dlq` via Prometheus. Gatilho: > 3 mensagens acumuladas na DLQ. Ação: interrupção de testes de integração e correção de parsing no consumidor.

* **D.2 Controle de mudanças (Diante de um pedido novo: quem avalia, quais impactos analisa [escopo, prazo, qualidade, riscos] e quem decide):**  
  * **Status:** 🟢 **Cumprido (100%)**  
  * **Onde está documentado:** `DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md` e `1_entrega_planejamento_e_arquitetura.md`.  
  * **Evidência:** Fluxo formal de controle de mudanças em 4 etapas: Registro via Issue no GitHub (`[Mudança de Escopo]`), Análise de Impacto Quadridimensional (Escopo, Prazo/Folga, Qualidade e Riscos), Decisão Colegiada pelo Gerente/Comitê em alinhamento com orientadores, e Atualização imediata do backlog no GitHub Projects e artefatos de arquitetura.

---

## 📋 3. Matriz Consolidada de Requisitos (Checklist da 1ª Entrega)

| ID | Módulo | Requisito do Documento Base | Peso | Status | Onde Localizar ou Ação Necessária |
| :--- | :---: | :--- | :---: | :---: | :--- |
| **DSC-01** | DSC | Problema, justificativa e público-alvo | — | 🟢 Cumprido | `README.md`, `Documento de Visao.pdf` |
| **DSC-02** | DSC | Objetivos e declaração de escopo (com Fora de Escopo) | — | 🟢 Cumprido | Formalizado em `1_entrega...`, `DIAGRAMA_DE_CASOS_DE_USO.md` |
| **DSC-03** | DSC | Perfis de usuário e permissões (RBAC) | — | 🟢 Cumprido | `ETAPA_3_Iam_Service...`, `DIAGRAMA_DE_CASOS_DE_USO.md` |
| **DSC-04** | DSC | Diagrama de Casos de Uso / Histórias BDD | — | 🟢 Cumprido | `.diagramas/`, `REQUISITOS_HISTORIAS_DE_USUARIOS.md` |
| **DSC-05** | DSC | Modelo de Dados Inicial (DER) | — | 🟢 Cumprido | `.diagramas/`, `ETAPA_14_Modelo_Logico_*.md` |
| **DSC-06** | DSC | Diagrama C4 (Níveis 1 e 2) | — | 🟢 Cumprido | `.diagramas/Diagramas Sistema Promptuario-C4*.png` |
| **DSC-07** | DSC | Stack Tecnológica e equivalência Spring Boot | — | 🟢 Cumprido | Justificada em `TECNOLOGIAS_UTILIZADAS.md`, `PLANO_MIGRACAO...` |
| **DSC-08** | DSC | Microsserviço de alto custo e escala | — | 🟢 Cumprido | `ai-service` e Celery workers documentados |
| **DSC-09** | DSC | Mensageria, fluxo e resiliência (DLQ) | — | 🟢 Cumprido | `ETAPA_2_Eventos_Rabbitmq...`, código RabbitMQ |
| **DSC-10** | DSC | Segurança, autenticação e LGPD | — | 🟢 Cumprido | `ETAPA_3_Iam...`, `ETAPA_4_Patient...`, `ETAPA_15...` |
| **DSC-11** | DSC | Estrutura de pastas do repositório | — | 🟢 Cumprido | Repositório organizado em módulos |
| **DSC-12** | DSC | Docker Compose com infraestrutura básica | — | 🟢 Cumprido | `backend/docker-compose.yml` completo |
| **DSC-13** | DSC | Divisão de tarefas entre os 4 integrantes | — | 🟢 Cumprido (Doc) | Matriz formalizada em `1_entrega...` e `ETAPA_13...` |
| **DSC-14** | DSC | Pipeline de Integração Contínua (CI) | — | 🟢 Cumprido | `.github/workflows/backend-ci.yml` |
| **TST-01** | Testes | Delimitação do Escopo de Testes (In/Out) | — | 🟢 Cumprido | Formalizado em `PLANO_DE_TESTES.md` e `1_entrega...` |
| **TST-02** | Testes | Estratégia e Pirâmide de Testes Proporcional | — | 🟢 Cumprido | Pirâmide 70/20/10 com defesa baseada em microsserviços |
| **TST-03** | Testes | Stack de Ferramentas de Teste | — | 🟢 Cumprido | Pytest, Vitest, Playwright, K6, Ruff, Bandit documentados |
| **TST-04** | Testes | Automação de testes em CI | — | 🟢 Cumprido | GitHub Actions executa testes automatizados com matrix |
| **TST-05** | Testes | Critérios Entrada/Saída: Unitários | — | 🟢 Cumprido | Cláusulas de Entry/Exit formalizadas (cobertura >= 80%) |
| **TST-06** | Testes | Critérios Entrada/Saída: Integração/Broker | — | 🟢 Cumprido | Cláusulas de Entry/Exit com validação de DLQ formalizadas |
| **TST-07** | Testes | Critérios Entrada/Saída: Carga | — | 🟢 Cumprido | Cláusulas de Entry/Exit com k6 (p95 < 800ms, erro < 1%) |
| **TST-08** | Testes | Tabela de Casos de Teste Iniciais | — | 🟢 Cumprido | 13 casos de teste BDD mapeados a partir de histórias de usuário |
| **GP-A01** | GP | A · Identificação (Projeto, Equipe, Gerente) | 30% | 🟢 Cumprido | Formalizado em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-A02** | GP | A · Problema, Objetivo e Valor do Produto | 30% | 🟢 Cumprido | Formalizado em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-A03** | GP | A · Projeto, Produto e Operação | 30% | 🟢 Cumprido | Formalizado em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-A04** | GP | A · Escopo Positivo e Fora do Escopo | 30% | 🟢 Cumprido | Lista taxativa em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-A05** | GP | A · Partes Interessadas (Stakeholders) | 30% | 🟢 Cumprido | Matriz completa em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-A06** | GP | A · Restrições, Premissas e Viabilidade | 30% | 🟢 Cumprido | Formalizado em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-B01** | GP | B · Abordagem e Ciclo de Vida (Híbrida) | 30% | 🟢 Cumprido | Formalizado em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-B02** | GP | B · Fases, Marcos, Entregas e Folgas | 30% | 🟢 Cumprido | Linha do tempo com folgas em `GERENCIA_DE_PROJETOS_...` |
| **GP-B03** | GP | B · Papéis e 3 a 5 Riscos Iniciais com Resposta | 30% | 🟢 Cumprido | RACI e 4 riscos em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-C01** | GP | C · Condução: Rotina e Ferramentas | 20% | 🟢 Cumprido | Formalizado em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |
| **GP-C02** | GP | C · Definição de "Pronto" (DoD: 3 a 5 critérios) | 20% | 🟢 Cumprido | 5 critérios formalizados em `GERENCIA_DE_PROJETOS_...` |
| **GP-C03** | GP | C · Comunicação com Stakeholders | 20% | 🟢 Cumprido | Matriz de comunicação em `GERENCIA_DE_PROJETOS_...` |
| **GP-D01** | GP | D · Mínimo 3 Indicadores e Gatilhos | 20% | 🟢 Cumprido | 3 indicadores com gatilhos em `GERENCIA_DE_PROJETOS_...` |
| **GP-D02** | GP | D · Fluxo de Controle de Mudanças | 20% | 🟢 Cumprido | Fluxo em 4 etapas em `GERENCIA_DE_PROJETOS_1_ENTREGA.md` |

---

## 🎯 4. Conclusão e Status de Prontidão para a Banca

Todas as ações recomendadas para a **1ª Entrega** foram **100% concluídas com sucesso**:

1. ✅ **Compilação do Documento Oficial de Gerência de Projetos (Módulo 3):**
   * Criado o documento executivo oficial [DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/DOCUMENTATION/GERENCIA_DE_PROJETOS_1_ENTREGA.md) no limite de até 4 páginas, cobrindo integralmente os 4 blocos de avaliação: Iniciação (30%), Planejamento Macro com linha do tempo e folgas de segurança (30%), Execução com ritos e DoD (20%) e Monitoramento com 3 indicadores e controle de mudanças (20%).
2. ✅ **Complementação do Documento de Testes (Módulo 2):**
   * O arquivo [DOCUMENTATION/PLANO_DE_TESTES.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/DOCUMENTATION/PLANO_DE_TESTES.md) foi expandido com a pirâmide proporcional (70/20/10), cláusulas de entrada e saída por nível, 13 casos de teste BDD e protocolo de teste de carga com k6.
3. ✅ **Alinhamento da Stack Tecnológica (Módulo 1):**
   * Decisões técnicas e matriz de equivalência formalizadas em [DOCUMENTATION/TECNOLOGIAS_UTILIZADAS.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/DOCUMENTATION/TECNOLOGIAS_UTILIZADAS.md) e [DOCUMENTATION/ARQUITETURA_DE_SOFTWARE.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/DOCUMENTATION/ARQUITETURA_DE_SOFTWARE.md), acompanhadas do plano de migração executivo [PLANO_MIGRACAO_CLINICAL_SERVICE.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/PLANO_MIGRACAO_CLINICAL_SERVICE.md).
4. ✅ **Consolidação do Documento Unificado da 1ª Entrega:**
   * O documento unificado [1_entrega_planejamento_e_arquitetura.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/1_entrega_planejamento_e_arquitetura.md) foi integralmente desenvolvido, cobrindo os Módulos 1, 2 e 3 de forma exaustiva.
5. 🔄 **Recomendação Operacional Contínua:**
   * Manter a distribuição contínua de commits e Pull Requests entre os 4 integrantes no GitHub ao longo de todo o desenvolvimento semestral.

