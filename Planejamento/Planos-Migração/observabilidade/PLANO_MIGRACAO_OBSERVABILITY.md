# Plano de Migração — OBSERVABILITY (stack unificada pós Patient+Clinical)

> Escopo: Prometheus + Alertmanager rules + Grafana + OTel Collector + Jaeger.
> Princípios: mudanças pequenas e testáveis, sem renomear métricas, sem apagar
> séries históricas, com rollback por `git revert` em cada fase.

## 1. Estado atual auditado

- `backend/observability/prometheus/prometheus.yml:18-21` já coleta
  `patient-clinical-service:8000/metrics`. **Não há jobs legados
  `patient-service`/`clinical-service`** — nada a remover do scrape.
- `backend/shared/observability.py` (`setup_observability` + middlewares
  `request_id`/`correlation_id` + `/metrics`): código compartilhado, sem
  referência a serviço legado. Migração = **reuso**, não reescrita.
- `backend/shared/metrics.py`: contadores Patient (`patients_registered_total`,
  `allergies_registered_total`, `vaccines_registered_total`) e Clinical
  (`consultations_total`, `prescriptions_total`, `medical_records_total`,
  `exam_requests_total`) com label `service`. O unificado emite com
  `service="patient-clinical-service"`; séries legadas permanecem no TSDB.
  **Não renomear métricas**.
- `alerts.yml`: regras usam `service`/`job` genéricos (`HighErrorRate`,
  `HighLatency`, `AuditEventsFailing`, `PrescriptionGenerationFailing`).
  Funcionam para o unificado sem mudança.
- `otel-collector-config.yml`: pipeline `otlp → jaeger + logging` agnóstico a
  serviço (`service.name` via `Resource`). Nada a migrar.
- Referências legadas restantes são **documentação/compose comentado**, não
  runtime de observabilidade.
- Risco: dashboards Grafana provisionados podem ter painéis filtrando
  `job="patient-service"|"clinical-service"`. Verificar na Fase O0.

## 2. Plano em fases (pequenas, testáveis, com rollback)

### Fase O0 — Baseline (somente leitura)

1. `promtool check config prometheus.yml` + `promtool check rules alerts.yml`.
2. `curl` targets: `prometheus:9090/api/v1/targets`, `/metrics` do unificado,
   `otel-collector:4318`, `jaeger:16686`.
3. Capturar baseline: `up{job="patient-clinical-service"}`,
   `http_requests_total{service="patient-clinical-service"}`,
   `patients_registered_total`, `consultations_total`, `prescriptions_total`,
   P95 `http_request_duration_seconds_bucket`,
   `rabbitmq_queue_messages_ready`, `audit_events_failed_total`.
4. Ler JSONs dos dashboards Grafana e listar painéis com filtro legado.
5. Registrar `OBSERVABILITY_BASELINE.md`. Sem edição.

### Fase O1 — Regras e dashboards (sem tocar em código)

1. Em `alerts.yml`: **adicionar** (não substituir); se algum alerta filtrar
   `job=~"patient-service|clinical-service"`, duplicar a expressão com
   `patient-clinical-service` em OR, mantendo a legada até o corte.
2. Nos dashboards: duplicar variáveis/painéis para `patient-clinical-service`,
   mantendo os antigos ocultos até a observação.
3. Validar: `promtool check`, `targets UP`, painéis renderizando.
4. Rollback: `git revert` dos arquivos alterados.

### Fase O2 — Correlação e SLOs do unificado (leitura + config)

1. Confirmar `X-Request-Id`/`X-Correlation-Id` Gateway → unificado → worker
   (logs JSON + traces Jaeger `service.name=patient-clinical-service`).
2. SLOs iniciais espelhando os legados: erro 5xx < 5%/5m, P95 < 2s,
   `audit_events_failed_total == 0`, `rabbitmq_queue_messages_ready < 1000`,
   `gateway_circuit_state == 0`.
3. Validar que `report_type="PRESCRIPTIONS"` continua emitido pelo worker.
4. Rollback: remover os SLOs adicionados.

### Fase O3 — Corte (junto à Fase 12, após observação)

1. Após a janela de observação: remover painéis/alertas duplicados legados e
   marcar jobs antigos como `historic (retired)` (não apagar séries do TSDB).
2. Atualizar `DOCUMENTATION/ETAPA_11_*`, `STATUS_GERAL`,
   `ARQUITETURA_DE_SOFTWARE`, `README` e diagramas.
3. Atualizar `Makefile health`, comentários do `prometheus.yml` e CI.
4. Rollback: backups `.phase12-backups/` + `GATEWAY_ROUTING_MODE=legacy`.

### Fase O4 — Pós-corte

1. Monitorar 7 dias: `ServiceDown`, `HighErrorRate`, `HighLatency`,
   `QueueDepthIncreasing`, `LoginFailuresSpike`, `AiErrorsSpike`,
   `CircuitBreakerOpen`.
2. Congelar baseline final em `OBSERVABILITY_CUTOVER_REPORT.md`.

## 3. Comandos de validação

```powershell
docker exec backend-prometheus-1 promtool check config /etc/prometheus/prometheus.yml
docker exec backend-prometheus-1 promtool check rules /etc/prometheus/alerts.yml
curl.exe -s http://localhost:9090/api/v1/targets
curl.exe -s http://localhost:8002/metrics
```

## 4. Testes

- Aprovados (Fase 11): matriz unitária + smoke Gateway → unificado → worker.
- Novos: `promtool check`, `targets UP`, painéis renderizando.
- Falhos conhecidos: `gobject-2.0-0` local (irrelevante em Docker).

## 5. Riscos

- Painéis com filtro hardcoded legado ficam vazios no corte (mitigado em O1).
- Renomear métrica quebraria histórico — **não renomear**.
- Cardinalidade `endpoint` alta — já existente, sem novos labels aqui.

## 6. Decisão

Plano pronto. Iniciar pela **Fase O0 (baseline somente leitura)** quando autorizado.

