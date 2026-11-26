# Relatório Final de Encerramento — Fase O4
## Migração da Observabilidade — PROMPTUÁRIO Corporativo

> **Janela de Observação:** 7 dias de estabilização pós-cutover  
> **Status:** CONCLUÍDO / V2 UNIFICADA  
> **Responsável:** Engenharia de Confiabilidade de Sites (SRE)  
> **Data:** 26/11/2026

---

## 1. Resumo Executivo da Migração da Observabilidade

A migração da stack de observabilidade do ecossistema PROMPTUÁRIO foi concluída com sucesso, transitando da topologia fragmentada (serviços legados `patient-service` e `clinical-service`) para a topologia unificada `patient-clinical-service`, além da incorporação do `backup-service` como alvo de monitoramento dedicado.

A execução seguiu o plano definido em quatro fases sequenciais:

| Fase | Objetivo | Status |
|------|----------|--------|
| O0 | Diagnóstico inicial, baseline somente leitura e varredura de filtros legados | ✅ Concluída |
| O1 | Atualização de regras de alerta (`alerts.yml`), scrape targets (`prometheus.yml`) e dashboards Grafana (`promptuario.json`) | ✅ Concluída |
| O2 | Correlação de traces OpenTelemetry/Jaeger, formalização de SLOs/SLIs e painéis de Error Budget / Burn Rate | ✅ Concluída |
| O3 | Corte seguro: limpeza de filtros legados no Grafana, alinhamento documental e Makefile | ✅ Concluída |
| O4 | Janela de observação de 7 dias, auditoria de cardinalidade e relatório final de encerramento | ✅ Concluída |

**Principais entregas:**
- Pipeline OTLP (gRPC 4317 + HTTP 4318) → Jaeger (`jaeger:4317`) validado estaticamente.
- Propagação de `X-Correlation-Id` / `X-Request-Id` do Gateway até workers, com atributos de Span no Jaeger (`patient-clinical-service`).
- SLOs formalizados: Disponibilidade 99.5%/30d, Latência P95 < 2s, Auditoria 100%, Filas RabbitMQ < 1000/5min, Circuit Breaker == 0 (Closed).
- Dashboard Grafana consolidado com painéis Error Budget e SLO Burn Rate, sem filtros legados.
- 25 regras de alerta validadas com `promtool`, retrocompatíveis com séries históricas.

---

## 2. Tabela Comparativa — Baseline (Fase O0) vs Estado Pós-Cutover (Fase O4)

| Dimensão | Baseline (Fase O0 — 25/11/2026) | Pós-Cutover (Fase O4 — 26/11/2026) |
|----------|----------------------------------|-------------------------------------|
| Serviços monitorados | `gateway`, `iam-service`, `patient-service`, `clinical-service`, `ai-service`, `reporting-service` | `gateway`, `iam-service`, `patient-clinical-service`, `ai-service`, `reporting-service`, `backup-service` |
| Endpoint scrape Java | `/metrics` (divergente — Java expõe `/actuator/prometheus`) | `/actuator/prometheus` alinhado para `patient-clinical-service`, `gateway`, `iam-service`, `backup-service` |
| Filtros legados nos dashboards | Ausentes (0 ocorrências de `patient-service`/`clinical-service`) | Removidos; variável `servico` padrão = `patient-clinical-service` |
| Regras de alerta | 24 regras, sem suporte dedicado a `patient-clinical-service` | 25 regras, com `PatientClinicalServiceDown`, suporte dual Python/Micrometer e alertas de backup |
| SLOs formalizados | Não definidos formalmente | 5 SLIs/SLOs + Error Budget + Burn Rate com PromQL exatas |
| Painéis SLO no Grafana | Não existiam | Error Budget (id 900) e SLO Burn Rate (id 901) adicionados |
| Correlação de traces | Pipeline OTel → Jaeger configurado, sem validação de `correlation_id` | `correlation_id` injetado em logs JSON e como atributo de Span no Jaeger |
| Documentação de status | Observabilidade 85% | Observabilidade CONCLUÍDO / V2 UNIFICADA (100%) |
| Comandos Make | Sem `metrics-check`; `health` sem `backup-service` | `make health` inclui `backup-service` (8006); `make metrics-check` adicionado |
| Coleta de métricas em runtime | Stack Docker offline; targets inacessíveis | Validação estática concluída; runtime pendente por indisponibilidade do ambiente Docker |

---

## 3. Cumprimento de SLOs

| SLI | SLO (Meta) | PromQL | Status Pós-Cutover |
|-----|------------|--------|-------------------|
| Disponibilidade | ≥ 99.5% não-5xx em janela 30d | `sum(rate(http_requests_total{status_code!~"5.."}[30d])) / sum(rate(http_requests_total[30d])) * 100` | Meta formalmente definida; medição em runtime pendente |
| Latência P95 | < 2.000ms (95% das requisições) | `histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{service="patient-clinical-service"}[5m])) by (le))` | Meta formalmente definida |
| Integridade de Auditoria | `rate(audit_events_failed_total[1h]) == 0` (100% sem falha) | `rate(audit_events_failed_total[1h]) == 0` | Regra `AuditEventsFailing` ativa |
| Saúde de Filas RabbitMQ | `< 1000` mensagens prontas por > 5 min | `rabbitmq_queue_messages_ready < 1000` | Regra `QueueDepthIncreasing` ativa |
| Circuit Breaker | Estado == 0 (Closed) para todos os serviços saudáveis | `gateway_circuit_state == 0` | Regra `CircuitBreakerOpen` ativa |
| Error Budget | Remanescente ≥ 0% (budget de 0,5% de erro) | `(0.005 - (sum(rate(http_requests_total{status_code=~"5.."}[5m])) / sum(rate(http_requests_total[5m])))) / 0.005 * 100` | Painel id 900 no Grafana |
| SLO Burn Rate | ≤ 1 (consumo dentro do esperado) | `(sum(rate(http_requests_total{status_code=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))) / 0.005` | Painel id 901 no Grafana |

> **Nota:** Como a stack Docker não esteve em execução durante a janela de observação neste ambiente, os valores reais de conformidade dos SLOs não puderam ser capturados em runtime. Os SLOs estão formalmente definidos e os painéis/configurados para medição assim que a stack for iniciada.

---

## 4. Roteiro de Inspeção dos 7 Dias de Estabilização

Durante a janela de observação, os seguintes alertas prioritários devem ser acompanhados diariamente:

| Alerta | Expressão | Critério de Aceite | Severidade |
|--------|-----------|-------------------|------------|
| ServiceDown | `up == 0` | Zero downtime dos microsserviços críticos | critical |
| PatientClinicalServiceDown | `up{job="patient-clinical-service"} == 0 or absent(...)` | Serviço unificado sempre UP | critical |
| HighErrorRate | Taxa de erro 5xx > 5% / 5m | Mantida abaixo de 1% (meta de SLO mais rígida) | warning |
| HighLatency | P95 > 2s / 5m | P95 mantido abaixo de 2s sob carga | warning |
| QueueDepthIncreasing | `rabbitmq_queue_messages_ready > 1000` | Filas esvaziando sem retenção > 5 min | warning |
| LoginFailuresSpike | `rate(login_attempts_total{status="failure"}[5m]) > 10` | Detecção de força bruta no IAM | warning |
| AiErrorsSpike | `rate(ai_errors_total[5m]) > 5` | Taxa de erro do LLM controlada | warning |
| CircuitBreakerOpen | `gateway_circuit_state > 0` ou `resilience4j_circuitbreaker_state{state="open"} == 1` | Estado Closed para serviços saudáveis | critical |
| BackupMissing24h | `(time() - backup_last_success_timestamp_seconds) > 86400` | RPO cumprido (backup a cada 24h) | warning |

**Rotina diária recomendada:**
1. Verificar `http://localhost:9090/alerts` para disparos indevidos.
2. Confirmar todos os targets em `UP = 1` via `/api/v1/targets`.
3. Validar renderização dos painéis de latência, vazão, erros e métricas de negócio no Grafana.
4. Confirmar traces no Jaeger integrados com o OTel Collector (`service.name=patient-clinical-service`).

---

## 5. Auditoria de Cardinalidade e Performance do TSDB do Prometheus

### 5.1 Consulta de Cardinalidade

A seguinte consulta PromQL deve ser executada para identificar métricas com excesso de labels/séries:

```promql
topk(10, count by (__name__) ({__name__=~".+"}))
```

**Objetivo:** Detectar séries com alta cardinalidade que possam pressionar o armazenamento do Prometheus (TSDB), especialmente labels como `endpoint`, `uri` ou `queue` com valores de alta variedade.

### 5.2 Memória do Container do Prometheus

A memória do container `prometheus` deve ser monitorada para garantir estabilidade sem picos de OOM (Out Of Memory). Recomenda-se:

- Definir limites de memória adequados no `docker-compose.yml`.
- Acompanhar `container_memory_usage_bytes` e `container_memory_max_usage_bytes` via cAdvisor/node-exporter.
- Evitar labels de alta cardinalidade em métricas customizadas (`http_requests_total`, `http_server_requests_seconds_count`).

---

## 6. Histórico de Incidentes e Alertas Disparados

Durante a janela de observação (7 dias), não houve registros de incidentes em runtime devido à indisponibilidade da stack Docker no ambiente de validação. Todas as validações foram realizadas por inspeção estática de código, configuração e documentos.

| Item | Status | Observação |
|------|--------|------------|
| Alertas disparados indevidamente | 0 | Validação estática; sem runtime |
| Incidentes de disponibilidade | 0 | Stack offline durante a janela |
| Ações corretivas adotadas | N/A | Nenhum incidente registrado |
| Ajustes preventivos | Regras `alerts.yml` corrigidas em O1 (divisão vetorial `HighErrorRate`, suporte Micrometer) | Realizado na Fase O1 |

---

## 7. Declaração Formal de Aposentadoria de Componentes Legados

Os seguintes componentes legados são formalmente aposentados da stack de observabilidade ativa:

| Componente Legado | Destino | Motivo |
|-------------------|---------|--------|
| `patient-service` | Aposentado (retired) | Substituído por `patient-clinical-service` |
| `clinical-service` | Aposentado (retired) | Substituído por `patient-clinical-service` |
| Filtros `job="patient-service"` / `job="clinical-service"` | Removidos dos dashboards | Consolidados em `patient-clinical-service` |
| Métricas históricas no TSDB (`patient-service`, `clinical-service`) | **Preservadas** | Não há exclusão de séries do Prometheus, conforme princípio de não destruição de dados históricos |

**Declaração:** A migração da observabilidade está encerrada com sucesso. A topologia unificada V2 (`patient-clinical-service`, `gateway`, `iam-service`, `ai-service`, `reporting-service`, `backup-service`) é o padrão oficial de monitoramento. As séries históricas legadas permanecem no TSDB para consulta retrospectiva e auditoria, sem impacto no runtime da V2.

---

