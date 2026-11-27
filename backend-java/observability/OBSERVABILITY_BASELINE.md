# Baseline de Observabilidade — Fase O0 (Diagnóstico Somente Leitura)

- **Data e hora da auditoria:** 2026-11-25 09:40:15 -03:00
- **Escopo:** Auditoria somente leitura da stack de observabilidade (`Prometheus`, `Alertmanager rules`, `Grafana`, `OTel Collector` e `Jaeger`) pré-Fase O1.
- **Arquivos de runtime alterados:** Nenhum (somente leitura conforme diretriz da Fase O0).

---

## 1. Resumo Executivo

A validação sintática das configurações do Prometheus (`prometheus.yml`) e das regras de alerta (`alerts.yml`) via `promtool` (imagem `prom/prometheus:v2.54.1`) foi aprovada com **0 erros** e **24 regras válidas sem duplicidade**. Nos dashboards Grafana provisionados (`promptuario.json`, `backup_dashboard.json`, `node-exporter.json`), **não há filtros rígidos legados** (`job="patient-service"`, `job="clinical-service"`, `service="patient-service"` ou `service="clinical-service"`).

Entretanto, durante esta auditoria foram identificadas **duas condições críticas de ambiente e arquitetura** que impedem a coleta de telemetria em runtime neste momento e exigem ação na **Fase O1**:

1. **Stack Docker do PROMPTUÁRIO offline no host:** nenhum container da aplicação ou observabilidade (`prometheus`, `grafana`, `otel-collector`, `jaeger`, `gateway`, `iam-service`, `patient-clinical-service`, `ai-service`, `reporting-service`, `backup-service`, `rabbitmq`) estava em execução durante a auditoria (`http://localhost:9090`, `4318` e `16686` inacessíveis). As métricas de baseline em runtime não puderam ser coletadas.
2. **Desalinhamento de endpoints e nomes de métricas:** os serviços Java 21 / Spring Boot 3.3.4 expõem métricas Micrometer em `/actuator/prometheus` (padrão `http_server_requests_seconds_*`), enquanto alguns painéis e regras ainda preservam séries históricas da antiga implementação Python.

---

## 2. Auditoria de Configuração e Regras (`promtool` e YAML)

### 2.1 Resultados do `promtool` (`prom/prometheus:v2.54.1`)

Comandos executados (montando `backend-java/observability/prometheus` em `/etc/prometheus:ro`):

```bash
docker run --rm --entrypoint promtool \
  -v /home/douglas/Documentos/PI-2026/PROMPTUARIO_CORPORATIVO/backend-java/observability/prometheus:/etc/prometheus:ro \
  prom/prometheus:v2.54.1 check config /etc/prometheus/prometheus.yml

docker run --rm --entrypoint promtool \
  -v /home/douglas/Documentos/PI-2026/PROMPTUARIO_CORPORATIVO/backend-java/observability/prometheus:/etc/prometheus:ro \
  prom/prometheus:v2.54.1 check rules /etc/prometheus/alerts.yml
```

Saída observada:

- **`prometheus.yml`**: `SUCCESS: 1 rule files found` / `SUCCESS: /etc/prometheus/prometheus.yml is valid prometheus config file syntax`.
- **`alerts.yml`**: `SUCCESS: 24 rules found` (0 erros de parsing, 0 erros de sintaxe PromQL, 0 regras com nomes duplicados).
- **`otel-collector-config.yml`**: validado via parser YAML (`yaml.safe_load`) sem erros.

### 2.2 Inventário de Scrape Jobs em `backend-java/observability/prometheus/prometheus.yml`

| Job | Target | `metrics_path` configurado | Tipo de Serviço no Compose | Diagnóstico Estático |
| :--- | :--- | :--- | :--- | :--- |
| `gateway` | `gateway:8000` | `/metrics` | Java (Spring Boot Actuator) | **Divergente:** Java expõe `/actuator/prometheus`; `/metrics` no Gateway faz proxy para `iam-service:8000/metrics`. |
| `iam-service` | `iam-service:8000` | `/metrics` | Java (Spring Boot Actuator) | **Divergente:** `application.yml` expõe `/actuator/prometheus` (não `/metrics`). |
| `patient-clinical-service` | `patient-clinical-service:8000` | `/metrics` | Java (Spring Boot Actuator) | **Divergente:** `application.yml` expõe `/actuator/prometheus` (não `/metrics`). Jobs legados já ausentes. |
| `ai-service` | `ai-service:8000` | `/metrics` | Python (FastAPI) | **Alinhado:** `shared/observability.py` monta `/metrics`. |
| `reporting-service` | `reporting-service:8000` | `/metrics` | Python (FastAPI) | **Alinhado:** `shared/observability.py` monta `/metrics`. |
| `backup-service` | `backup-service:8000` | `/actuator/prometheus` | Java (Spring Boot Actuator) | **Alinhado:** `SecurityConfig` e `application.yml` expõem `/actuator/prometheus`. |
| `rabbitmq` | `rabbitmq:15692` | `/metrics` | RabbitMQ 3.13 Management | **Alinhado.** |
| `postgres` | `postgres-exporter:9187` | `/metrics` (padrão) | `postgres-exporter:v0.15.0` | **Alinhado.** |
| `mongodb` | `mongodb-exporter:9216` | `/metrics` | `mongodb_exporter:0.44` | **Alinhado.** |
| `node-exporter` | `node-exporter:9100` | `/metrics` (padrão) | `node-exporter:v1.8.2` | **Alinhado.** |

### 2.3 Achados Semânticos em `alerts.yml` (24 regras)

- Nenhuma regra contém filtro rígido para `patient-service` ou `clinical-service`.
- **Achado PromQL em `HighErrorRate` (`alerts.yml:24-27`):**
  `rate(http_requests_total{status_code=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.05` — o numerador mantém o label `status_code="5xx"` enquanto o denominador possui séries com múltiplos `status_code` (`200`, `404`, `500`, etc.); sem agregação (`sum by (service)`), a divisão direta falha no matching vetorial do PromQL.
- **Achado de compatibilidade Java/Micrometer:** regras baseadas em `http_requests_total`, `http_requests_4xx_total`, `http_request_duration_seconds_bucket`, `login_attempts_total`, `audit_events_failed_total`, `http_requests_rejected_total` e `gateway_circuit_state` dependem de instrumentação customizada ainda não presente nos módulos Java (`gateway`, `iam-service`, `patient-clinical-service`), os quais emitem métricas nativas do Micrometer (`http_server_requests_seconds_*`, `resilience4j_circuitbreaker_state`).

---

## 3. Varredura dos Dashboards Grafana

Foram inspecionados todos os dashboards provisionados em `backend-java/observability/grafana/provisioning/dashboards/`:

- `promptuario.json` (`uid: promptuario-obs`, 24 painéis, 7 rows, 2 variáveis de template)
- `backup_dashboard.json` (`uid: backup-dr`, 9 painéis, 4 rows, 0 variáveis)
- `node-exporter.json` (`uid: rYdddlPWk`, infraestrutura de host)

### 3.1 Filtros Rígidos Legados

Varredura textual e estrutural em `promptuario.json`, `backup_dashboard.json` e `node-exporter.json`:

- `job="patient-service"`: **0 ocorrências**
- `job="clinical-service"`: **0 ocorrências**
- `service="patient-service"`: **0 ocorrências**
- `service="clinical-service"`: **0 ocorrências**

### 3.2 Diagnóstico de Prontidão de `promptuario.json` para `patient-clinical-service`

#### Variáveis de Template

| Variável | Definição Atual | Status para `patient-clinical-service` | Observação |
| :--- | :--- | :--- | :--- |
| `servico` | `label_values(up, job)` | Parcialmente preparada | Descobrirá `patient-clinical-service` dinamicamente, porém **nenhum dos 24 painéis usa `$servico` nas queries**. |
| `endpoint` | `label_values(http_requests_total, endpoint)` | Incompatível com Java puro | Depende da métrica Python `http_requests_total` (Micrometer usa `http_server_requests_seconds_count` com label `uri`) e **não é referenciada por nenhum painel**. |

#### Inventário Completo dos Painéis de `promptuario.json` e Ações para a Fase O1

| ID | Título | Query Atual (`expr`) | Pronto para `patient-clinical-service`? | Ação Recomendada para Fase O1 |
| :--- | :--- | :--- | :--- | :--- |
| 4 | Serviços UP/DOWN | `up` | **Sim** (por `job`) | Nenhuma alteração obrigatória (após corrigir `metrics_path` no `prometheus.yml`). |
| 1 | Requests por segundo (por serviço) | `rate(http_requests_total[1m])` | **Não** (métrica Python) | Incluir suporte OR/fallback para `http_server_requests_seconds_count` (Micrometer) ou registrar `http_requests_total` no Java; aplicar filtro `$servico`. |
| 2 | Taxa de erros 4xx (por serviço) | `rate(http_requests_4xx_total[1m])` | **Não** (métrica Python) | Adaptar para incluir `http_server_requests_seconds_count{status=~"4.."}` em OR com a série histórica Python. |
| 3 | Latência P95 (segundos) | `histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))` | **Não** (métrica Python) | Adaptar para incluir `http_server_requests_seconds_bucket` (ou habilitar histograma no Spring Boot) e agregar por `le, job/service`. |
| 5 | Cadastros (últimas 24h) | `rate(patients_registered_total[24h])`, `rate(users_registered_total[24h])`, `rate(consultations_total[24h])` | **Não** (métrica não emitida pelo Java) | Registrar contadores equivalentes via `MeterRegistry` no `patient-clinical-service` e `iam-service` (mantendo o nome sem sufixo duplicado se possível) ou adaptar query. |
| 6 | Atividades Clínicas (por hora) | `rate(prescriptions_total[1h])`, `rate(medical_records_total[1h])`, `rate(exam_requests_total[1h])` | **Não** (métrica não emitida pelo Java) | Registrar contadores no `patient-clinical-service` via Micrometer e adicionar painéis dedicados de Prontuários SOAP / PDFs assíncronos. |
| 7 | Status das Consultas | `rate(consultations_total{status="completed"\|"cancelled"\|"scheduled"}[1h])` | **Não** (métrica não emitida pelo Java) | Registrar `consultations_total` com tag `status` no `patient-clinical-service`. |
| 8 | Requisições à IA (por tipo) | `rate(ai_requests_total[1h])` | N/A (`ai-service` Python) | Manter (serviço permanece em Python). |
| 9 | Latência Média da IA (segundos) | `rate(ai_request_duration_seconds_sum[5m]) / rate(ai_request_duration_seconds_count[5m])` | N/A (`ai-service` Python) | Manter (serviço permanece em Python). |
| 10 | Tentativas de Login | `rate(login_attempts_total{status="failure"\|"success"}[5m])` | N/A (`iam-service` Java) | Registrar `login_attempts_total` no `iam-service` Java para evitar `No Data`. |
| 11 | Mensagens Prontas (por fila) | `rabbitmq_queue_messages_ready` | **Sim** (métrica do broker) | Manter; na Fase O1/O2 verificar filas `prescription.pdf.generation` e DLQs. |
| 12 | Publicação de Mensagens | `rate(rabbitmq_queue_messages_published_total[5m])` | **Sim** (métrica do broker) | Manter. |
| 13 | Mensagens Não Processadas | `rabbitmq_queue_messages_unacked` | **Sim** (métrica do broker) | Manter. |
| 14 | Conexões PostgreSQL Ativas (por banco) | `pg_stat_activity_count` | **Sim** (exporter DB) | Manter. |
| 15 | Transações PostgreSQL (por banco) | `rate(pg_stat_database_xact_commit[5m])`, `rate(pg_stat_database_xact_rollback[5m])` | **Sim** (exporter DB) | Manter. |
| 16 | MongoDB — Conexões e Operações | `mongodb_connections`, `mongodb_operations_*` | **Sim** (exporter DB) | Manter. |
| 17 | Top 5 Endpoints + Requisitados | `topk(5, rate(http_requests_total[5m]))` | **Não** (métrica Python) | Adicionar suporte OR para `http_server_requests_seconds_count` (`uri`). |
| 18 | Top 5 Endpoints + Lentos (P95) | `topk(5, histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])))` | **Não** (métrica Python) | Adicionar suporte OR para `http_server_requests_seconds_bucket`. |
| 19 | Tamanho Médio das Respostas HTTP | `rate(http_response_size_bytes_sum[5m]) / rate(http_response_size_bytes_count[5m])` | **Não** (métrica Python) | Avaliar remoção/substituição para serviços Java ou registro equivalente. |
| 20 | Requisições por Serviço | `sum by (service) (rate(http_requests_total[5m]))` | **Não** (métrica Python) | Incluir suporte a séries Micrometer (`job` / `application`) em OR com `service`. |
| 21 | Requisições por Status Code | `sum by (status_code) (rate(http_requests_total[5m]))` | **Não** (métrica Python) | Incluir suporte OR para label `status` do Micrometer (`http_server_requests_seconds_count`). |
| 22 | Requisições por Endpoint | `sum by (endpoint) (rate(http_requests_total{endpoint=~"/api/v1/.*"}[5m]))` | **Não** (métrica Python) | Incluir suporte OR para label `uri=~"/api/v1/.*"` do Micrometer. |
| 23 | SLO — Taxa de Sucesso (24h) | `sum(rate(http_requests_total{status_code!~"5.."}[24h])) / sum(rate(http_requests_total[24h])) * 100` | **Não** (métrica Python) | Atualizar fórmula na Fase O1/O2 para consolidar Python + Micrometer (`status!~"5.."`). |
| 24 | SLO — P99 Latência (24h) | `histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket[24h])) by (le))` | **Não** (métrica Python) | Atualizar na Fase O1/O2 para consolidar buckets Python + Micrometer. |

---

## 4. Coleta de Targets e Métricas de Baseline (Estado do Ambiente)

### 4.1 Targets do Prometheus (`http://localhost:9090/api/v1/targets`)

No momento da auditoria (`2026-11-25 09:38 -03:00`), o endpoint `http://localhost:9090/api/v1/targets` encontrava-se **inacessível (`PROM_UNREACHABLE`)** e `docker ps` não listava nenhum container da stack PROMPTUÁRIO em execução (apenas `pgadmin_ui` e `postgres_db` externos ao compose do projeto estavam presentes).

| Target Esperado (Job) | Endpoint Configurado | Status na Auditoria | Observação |
| :--- | :--- | :--- | :--- |
| `gateway` | `http://gateway:8000/metrics` | **Offline / Não coletado** | Container não iniciado no host; porta local `8000` sem listener. |
| `iam-service` | `http://iam-service:8000/metrics` | **Offline / Não coletado** | Container não iniciado no host; porta local `8001` sem listener. |
| `patient-clinical-service` | `http://patient-clinical-service:8000/metrics` | **Offline / Não coletado** | Container não iniciado no host; porta local `8002` sem listener. |
| `ai-service` | `http://ai-service:8000/metrics` | **Offline / Não coletado** | Container não iniciado no host; porta local `8004` sem listener. |
| `reporting-service` | `http://reporting-service:8000/metrics` | **Offline / Não coletado** | Container não iniciado no host. |
| `backup-service` | `http://backup-service:8000/actuator/prometheus` | **Offline / Não coletado** | Container não iniciado no host; porta local `8006` sem listener. |
| `rabbitmq` | `http://rabbitmq:15692/metrics` | **Offline / Não coletado** | Container `thirsty_jang` parado (`Exited` há 10 dias). |
| `postgres` | `http://postgres-exporter:9187/metrics` | **Offline / Não coletado** | Container não iniciado no host. |
| `mongodb` | `http://mongodb-exporter:9216/metrics` | **Offline / Não coletado** | Container não iniciado no host. |
| `node-exporter` | `http://node-exporter:9100/metrics` | **Offline / Não coletado** | Container não iniciado no host. |

### 4.2 Valores Correntes das Métricas de Referência (Baseline)

Como Prometheus e os serviços da aplicação não estavam em execução durante a auditoria somente leitura, os valores numéricos correntes não puderam ser consultados em runtime:

| Métrica / Expressão | Valor Coletado | Status |
| :--- | :--- | :--- |
| `up{job="patient-clinical-service"}` | N/A | Prometheus offline (`localhost:9090` inacessível) |
| `http_requests_total{service="patient-clinical-service"}` | N/A | Prometheus e serviço offline; além disso, serviço Java atual não registra essa métrica sem adaptação |
| `patients_registered_total` | N/A | Prometheus e serviço offline |
| `consultations_total` | N/A | Prometheus e serviço offline |
| `prescriptions_total` | N/A | Prometheus e serviço offline |
| P95 de `http_request_duration_seconds_bucket` | N/A | Prometheus e serviço offline |
| `rabbitmq_queue_messages_ready` | N/A | Prometheus e RabbitMQ offline |
| `audit_events_failed_total` | N/A | Prometheus e serviços offline |

---

## 5. Pipeline OpenTelemetry e Jaeger

### 5.1 Inspeção Estática da Configuração

- **`backend-java/observability/otel-collector-config.yml`** (válido):
  - `receivers.otlp`: gRPC em `0.0.0.0:4317` e HTTP em `0.0.0.0:4318`.
  - `processors`: `memory_limiter` (`256 MiB`) e `batch` (`timeout: 1s`, `send_batch_size: 1024`).
  - `exporters`: `otlp/jaeger` apontando para `jaeger:4317` (`tls.insecure: true`) e `logging` (`loglevel: warn`).
  - `service.pipelines.traces`: `receivers: [otlp]` → `processors: [memory_limiter, batch]` → `exporters: [otlp/jaeger, logging]`.
- **`backend-java/docker-compose.yml`**:
  - `jaeger` (`jaegertracing/all-in-one:1.58`) com `COLLECTOR_OTLP_ENABLED: "true"`, portas `16686:16686` e `14250:14250`.
  - `otel-collector` (`otel/opentelemetry-collector-contrib:0.105.0`), portas `4317:4317` e `4318:4318`, dependendo de `jaeger`.
  - `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317` injetado nos serviços da stack Java.
  - **Lacuna identificada nos serviços Java (`pom.xml`):** `patient-clinical-service`, `iam-service` e `gateway` não possuem dependência OpenTelemetry/Micrometer Tracing OTLP nos respectivos `pom.xml` no estado atual (o AI Python da própria stack usa `backend-java/shared/observability.py`).

### 5.2 Status Operacional em Runtime (`otel-collector:4318` e `jaeger:16686`)

  - `http://localhost:4318/`: **Inacessível (`OTEL_UNREACHABLE`)** — container `otel-collector` não estava em execução no momento da auditoria.
- `http://localhost:16686/`: **Inacessível (`JAEGER_UNREACHABLE`)** — container `jaeger` não estava em execução no momento da auditoria.
- **Conclusão:** A configuração estática do pipeline OTel → Jaeger está válida e consistente, porém **não foi possível confirmar operacionalidade em runtime** nesta sessão por ausência da stack em execução.

---

## 6. Próximos Passos Recomendados antes/durante a Fase O1

1. Subir a stack via `docker compose` (`backend-java/docker-compose.yml`) para capturar os valores numéricos de baseline reais em `localhost:9090`, `8002/actuator/prometheus`, `4318` e `16686`.
2. Corrigir `metrics_path: /actuator/prometheus` em `prometheus.yml` para os jobs Java (`gateway`, `iam-service`, `patient-clinical-service`).
3. Corrigir a divisão vetorial da regra `HighErrorRate` em `alerts.yml` (agregar por `service`/`job`) e adicionar suporte às métricas Micrometer (`http_server_requests_seconds_*`) em `alerts.yml` e `promptuario.json`.
4. Definir na transição O1/O2 o registro das métricas de negócio (`patients_registered_total`, `consultations_total`, `prescriptions_total`, etc.) e exportação OTLP no `patient-clinical-service` Java.
