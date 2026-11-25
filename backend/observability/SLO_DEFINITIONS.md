# SLO Definitions — PROMPTUÁRIO Corporativo

> Documento formal de Service Level Objectives (SLOs) e Service Level Indicators (SLIs)
> do ecossistema PROMPTUÁRIO unificado (Fase O2 da migração de observabilidade).
> Janela de avaliação padrão: 30 dias (rolling) salvo indicação em contrário.

## 1. Visão Geral

Os SLOs abaixo traduzem os requisitos de confiabilidade do sistema em metas mensuráveis,
baseadas nas métricas expostas via Prometheus (`http_requests_total`,
`http_request_duration_seconds_bucket`, `audit_events_failed_total`,
`rabbitmq_queue_messages_ready`, `gateway_circuit_state`). Cada SLI é acompanhado de
expressão PromQL para uso em dashboards e alertas do Grafana.

---

## 2. Disponibilidade (Availability SLI)

- **SLI:** Proporção de requisições HTTP que não resultam em erro 5xx.
- **SLO:** 99.5% de requisições com status != 5xx em janela móvel de 30 dias.
- **Error Budget:** 0.5% (1 - 0.995).
- **PromQL:**

```promql
sum(rate(http_requests_total{status_code!~"5.."}[30d])) / sum(rate(http_requests_total[30d])) * 100
```

---

## 3. Latência (Latency SLI)

- **SLI:** Percentil 95 do tempo de resposta das requisições do patient-clinical-service.
- **SLO:** 95% das requisições atendidas em menos de 2000ms (P95 < 2s).
- **PromQL:**

```promql
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{service="patient-clinical-service"}[5m])) by (le))
```

---

## 4. Integridade de Auditoria (Audit SLI)

- **SLI:** Taxa de eventos de auditoria persistidos sem falha.
- **SLO:** 100% de eventos de auditoria persistidos sem falha.
- **PromQL:**

```promql
rate(audit_events_failed_total[1h]) == 0
```

---

## 5. Fila de Mensageria (Queue Health SLI)

- **SLI:** Número de mensagens prontas (ready) nas filas do RabbitMQ.
- **SLO:** Mensagens prontas no RabbitMQ < 1000 por mais de 5 minutos.
- **PromQL:**

```promql
rabbitmq_queue_messages_ready < 1000
```

---

## 6. Disjuntor de Borda (Circuit Breaker SLI)

- **SLI:** Estado do circuit breaker no Gateway para microsserviços saudáveis.
- **SLO:** Estado do Circuit Breaker no Gateway == 0 (Closed) para todos os microsserviços saudáveis.
- **PromQL:**

```promql
gateway_circuit_state == 0
```

---

## 7. Error Budget e Burn Rate

### 7.1 Error Budget (Disponibilidade)

Error Budget remanescente (fração), considerando SLO de 99.5% (allowed error ratio = 0.005):

```promql
(0.005 - (sum(rate(http_requests_total{status_code=~"5.."}[5m])) / sum(rate(http_requests_total[5m])))) / 0.005 * 100
```

### 7.2 SLO Burn Rate

Burn rate = taxa de erro observada / taxa de erro permitida. Valor > 1 indica consumo acelerado do budget.

```promql
(sum(rate(http_requests_total{status_code=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))) / 0.005
```

---

## 8. Rastreamento Distribuído (OpenTelemetry / Jaeger)

- Pipeline: OTLP gRPC `0.0.0.0:4317`, OTLP HTTP `0.0.0.0:4318` -> Jaeger `jaeger:4317`.
- Correlação: `X-Correlation-Id` / `X-Request-Id` propagados Gateway -> serviços -> workers.
- O mesmo `X-Correlation-Id` deve estar presente:
  1. Nos headers HTTP de resposta do Gateway.
  2. Nos logs estruturados dos microsserviços (campo `correlation_id`).
  3. Como atributo do Span no Jaeger sob o serviço `patient-clinical-service`.

---

## 9. Trecho de Configuração do Grafana (Painéis de SLO)

Adicionar ao dashboard `backend/observability/grafana/provisioning/dashboards/promptuario.json`
na linha (row) `💡 SLO / SLI Tracking` os painéis abaixo.

### 9.1 Painel "Error Budget" (stat)

```json
{
  "datasource": "Prometheus",
  "fieldConfig": {
    "defaults": {
      "color": { "mode": "thresholds" },
      "mappings": [],
      "thresholds": {
        "mode": "absolute",
        "steps": [
          { "color": "red", "value": null },
          { "color": "yellow", "value": 25 },
          { "color": "green", "value": 50 }
        ]
      },
      "unit": "percent"
    },
    "overrides": []
  },
  "gridPos": { "h": 6, "w": 6, "x": 12, "y": 49 },
  "id": 900,
  "options": {
    "colorMode": "background",
    "graphMode": "area",
    "justifyMode": "auto",
    "orientation": "auto",
    "reduceOptions": { "calcs": ["mean"], "fields": "", "values": false },
    "showPercentChange": false,
    "textMode": "auto",
    "wideLayout": true
  },
  "pluginVersion": "11.2.2",
  "targets": [
    {
      "expr": "(0.005 - (sum(rate(http_requests_total{status_code=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])))) / 0.005 * 100",
      "legendFormat": "Error Budget",
      "refId": "A"
    }
  ],
  "title": "SLO \u2014 Error Budget (Disponibilidade)",
  "type": "stat"
}
```

### 9.2 Painel "SLO Burn Rate" (stat)

```json
{
  "datasource": "Prometheus",
  "fieldConfig": {
    "defaults": {
      "color": { "mode": "thresholds" },
      "mappings": [],
      "thresholds": {
        "mode": "absolute",
        "steps": [
          { "color": "green", "value": null },
          { "color": "yellow", "value": 1 },
          { "color": "red", "value": 2 }
        ]
      },
      "unit": "short"
    },
    "overrides": []
  },
  "gridPos": { "h": 6, "w": 6, "x": 18, "y": 49 },
  "id": 901,
  "options": {
    "colorMode": "background",
    "graphMode": "area",
    "justifyMode": "auto",
    "orientation": "auto",
    "reduceOptions": { "calcs": ["mean"], "fields": "", "values": false },
    "showPercentChange": false,
    "textMode": "auto",
    "wideLayout": true
  },
  "pluginVersion": "11.2.2",
  "targets": [
    {
      "expr": "(sum(rate(http_requests_total{status_code=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m]))) / 0.005",
      "legendFormat": "Burn Rate",
      "refId": "A"
    }
  ],
  "title": "SLO \u2014 Burn Rate (Disponibilidade)",
  "type": "stat"
}
```

---

## 10. Critérios de Aceite (Fase O2)

- Traces do `patient-clinical-service` visíveis no Jaeger (`http://localhost:16686`) com Spans correlacionados pelo `correlation_id`.
- Documento `SLO_DEFINITIONS.md` consolidado com expressões PromQL exatas e metas mensuráveis.
- Painéis "Error Budget" e "SLO Burn Rate" adicionados ao dashboard Grafana.
