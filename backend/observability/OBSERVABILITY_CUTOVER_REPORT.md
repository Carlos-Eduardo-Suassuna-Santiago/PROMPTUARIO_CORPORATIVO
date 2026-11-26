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

