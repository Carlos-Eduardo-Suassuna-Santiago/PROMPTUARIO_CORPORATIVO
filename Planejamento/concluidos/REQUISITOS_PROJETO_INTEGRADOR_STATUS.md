# Análise de Requisitos: Projeto Integrador vs. Implementação Atual

Este documento apresenta uma análise detalhada comparando os requisitos do documento **Projeto_Integrador_2026_2_Formatado.md** com o status atual do sistema PROMPTUARIO, baseado nas documentações do projeto.

> [!WARNING]
> **Divergência Crítica de Stack Tecnológica:** O requisito central do Projeto Integrador estipula o uso de **Java 21 e Spring Boot 3.x**. No entanto, o sistema atual foi quase inteiramente desenvolvido em **Python (FastAPI)**. Para a entrega final nas disciplinas (em especial DSC), o núcleo do backend precisará ser reescrito ou o professor precisará aprovar formalmente a stack atual.

---

## 🟢 1. Requisitos Totalmente Implementados

Apesar da divergência de linguagem de programação, o sistema atual cobre de forma excelente a maioria dos requisitos arquiteturais e funcionais exigidos:

### 1.1 Arquitetura e Comunicação
- **Microsserviço de Alto Custo:** O `ai-service` e os Celery workers do `reporting-service` cumprem perfeitamente a exigência de ter um serviço apartado para tarefas pesadas.
- **Mensageria Assíncrona:** Implementado com sucesso via RabbitMQ (Topic Exchanges para os serviços).
- **Frontend:** Frontend desenvolvido em React + TypeScript + Tailwind (atendendo à exigência de consumir a API).
- **Contêineres Docker:** 100% implementado. Todos os componentes sobem via `docker-compose`.

### 1.2 Requisitos Funcionais Mínimos
- **Múltiplos Perfis (Min. 3):** Superado (Existem 4 perfis: ADMIN, DOCTOR, ATTENDANT, PATIENT).
- **Fluxo de Negócio Complexo:** Atendido pelo fluxo clínico de `Paciente -> Agendamento -> Prontuário -> Prescrição/Exame`.
- **Upload/Download de Arquivos:** Atendido com a integração MinIO (S3) para PDFs de prescrições e exportações.
- **Relatório Gerencial Exportável:** Atendido pelo `reporting-service` (exportação em PDF, CSV, JSON).
- **Trilha de Auditoria:** Atendido (histórico imutável de prontuários em `MedicalRecordHistory`).
- **Rotina Agendada:** Atendido (uso de Celery Beat no `reporting-service`).

### 1.3 Requisitos Não Funcionais (Conceituais)
- **Conformidade LGPD:** Implementado através de soft deletes e processo de anonimização no `patient-service`.
- **Resiliência:** API Gateway implementa Circuit Breaker, Rate Limiting; RabbitMQ possui Dead Letter Queue (DLX).
- **Observabilidade:** Totalmente atendido com Prometheus, Grafana, Loki e Jaeger integrados via OpenTelemetry.
- **CI/CD & Versionamento:** GitHub Actions implementado (lint, testes, build de imagens Docker).

---

## 🟡 2. Requisitos Implementados com Desvio de Tecnologia

Estes são os requisitos que foram implementados funcionalmente, mas utilizando tecnologias diferentes das exigidas de forma obrigatória no documento.

| Requisito do PI | Implementação Atual | Status da Adequação |
| --- | --- | --- |
| **Linguagem Java 21 / Spring Boot 3** | Python 3.12 / FastAPI | ❌ **Necessita Refatoração/Reescrita** |
| **Monólito Modular + 1 Microsserviço**| Arquitetura 100% Microsserviços | 🟡 O modelo atual é mais complexo. Deve ser validado com o professor. |
| **Keycloak (Identidade)** | IAM Service customizado + OAuth Google | ❌ **Precisa ser trocado para Keycloak** |
| **Spring Data JPA / Hibernate** | SQLAlchemy (Python) | ❌ **Adequar para Java/JPA** |
| **Flyway** | Alembic (ou sync SQLAlchemy) | ❌ **Adequar para Flyway** |
| **JUnit 5, Mockito, Testcontainers**| Pytest | ❌ **Adequar para testes Java** |

---

## 🔴 3. Requisitos Pendentes / Não Implementados

Independentemente da stack, estas são as atividades que ainda precisam ser feitas de acordo com o documento base:

### 3.1 Testes e Validação
- [ ] **Teste de Carga (Item 5 do PI):** Ainda não foi executado. É obrigatório criar scripts em **k6** ou **Apache JMeter**, versioná-los no repositório, e realizar o teste de carga no microsserviço (ex: no envio de requisições de IA ou Relatórios).
- [ ] **Comparativo de Escalabilidade:** Documentar e comparar a execução do teste de carga com 1 instância vs. 2 ou 3 instâncias do microsserviço (via `docker compose up --scale`).
- [ ] **Cobertura de Testes Unitários:** A cobertura atual é de ~50%. Precisa aumentar e gerar relatório (ex: JaCoCo no mundo Java).

### 3.2 Implantação Real (Nuvem)
- [ ] **Publicação em Ambiente de Nuvem (Item 6.4):** O sistema precisa estar publicado e acessível via URL (AWS, Oracle Cloud, Render, etc.). Atualmente os deploys estão documentados via SSH, mas é preciso garantir o ambiente no ar para a banca.

### 3.3 Documentações Específicas do Projeto
- [ ] **Architecture Decision Records (ADRs):** Necessário registrar pelo menos 5 decisões no formato ADR.
- [ ] **Diário de Bordo:** Criar ou atualizar o diário de bordo com registros de atividades semanais, dificuldades e decisões por todos os integrantes.
- [ ] **Entregáveis de Gerência e Testes:** Gerar Plano de Testes detalhado, EAP, Cronograma, Termo de Abertura, e Matriz RACI para alinhamento com as outras disciplinas da banca.

---

## 🎯 Conclusão e Plano de Ação

O sistema PROMPTUARIO está extremamente maduro do ponto de vista de arquitetura de software, englobando padrões avançados de resiliência, observabilidade e event-driven architecture. 

**O maior risco atual para o projeto acadêmico é a linguagem de programação.**
A equipe tem duas opções:
1. **Opção A:** Negociar com o professor de *Desenvolvimento de Sistemas Corporativos* a aceitação do ecossistema Python (FastAPI/Celery/SQLAlchemy) dada a altíssima complexidade e qualidade já entregues.
2. **Opção B:** Iniciar imediatamente a reescrita dos serviços principais (`gateway`, `patient`, `clinical`, `iam`) para **Spring Boot/Java**, mantendo a integração com o banco PostgreSQL, o front-end em React e a infraestrutura Docker já pronta. O Keycloak precisará substituir o IAM atual.
