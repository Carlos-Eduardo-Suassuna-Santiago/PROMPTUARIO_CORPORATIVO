# Guia de Prompts de Implementação: Migração e Modernização da Observabilidade (Prometheus, Grafana, Alertmanager, OpenTelemetry e Jaeger)

Este documento contém o conjunto completo de **prompts estruturados e detalhados**, organizados em **5 fases sequenciais (Fases O0 a O4)**, para guiar a implementação assistida por IA da migração, validação e modernização da stack de **Observabilidade** do ecossistema PROMPTUÁRIO (Prometheus, Alertmanager, Grafana, OpenTelemetry Collector e Jaeger), refletindo a topologia unificada pós-Fase 12 (`patient-clinical-service`, `iam-service`, `reporting-service`, `backup-service`, `gateway` e `ai-service`).

O conteúdo foi desenhado com base no documento [PLANO_MIGRACAO_OBSERVABILITY.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/Planos-Migra%C3%A7%C3%A3o/observabilidade/PLANO_MIGRACAO_OBSERVABILITY.md) e nos princípios de resiliência e não-destruição de séries temporais históricas.

---

## Índice das Fases de Implementação

1. [Fase O0: Diagnóstico Inicial, Varredura de Filtros Legados e Baseline (Somente Leitura)](#fase-o0-diagnóstico-inicial-varredura-de-filtros-legados-e-baseline-somente-leitura)
2. [Fase O1: Atualização de Regras de Alerta (`alerts.yml`) e Dashboards Grafana](#fase-o1-atualização-de-regras-de-alerta-alertsyml-e-dashboards-grafana)
3. [Fase O2: Rastreamento Distribuído (OTel/Jaeger), Correlação de Logs e Validação de SLOs](#fase-o2-rastreamento-distribuído-oteljaeger-correlação-de-logs-e-validação-de-slos)
4. [Fase O3: Corte Seguro (Cutover), Limpeza de Filtros Legados e Documentação](#fase-o3-corte-seguro-cutover-limpeza-de-filtros-legados-e-documentação)
5. [Fase O4: Janela de Observação Pós-Corte (7 Dias) e Relatório Final](#fase-o4-janela-de-observação-pós-corte-7-dias-e-relatório-final)

---

## Diretrizes Gerais para o Ciclo de Execução

- **Preservação de Séries Históricas:** Nunca renomeie métricas já coletadas nem execute comandos que expurguem dados do TSDB do Prometheus. Séries legadas emitidas com `service="patient-service"` ou `service="clinical-service"` permanecem no banco para consulta retrospectiva.
- **Transição com Suporte Duplo (OR Matchers):** Durante a fase de transição, alertas e dashboards devem utilizar seletores com regex `service=~"patient-clinical-service|patient-service|clinical-service"` para suportar tanto os dados históricos quanto os novos dados unificados.
- **Validação de Sintaxe Pré-Reload:** Antes de aplicar qualquer alteração de regras ou scrape no Prometheus, valide obrigatoriamente a sintaxe com a ferramenta oficial `promtool`.
- **Rastreabilidade Ponta a Ponta:** Garantir que os identificadores `X-Request-Id` e `X-Correlation-Id` sejam propagados do API Gateway até os workers em segundo plano e correlacionados aos traces do Jaeger.

---

## Fase O0: Diagnóstico Inicial, Varredura de Filtros Legados e Baseline (Somente Leitura)

### Objetivo
Realizar uma auditoria abrangente e somente leitura na infraestrutura de observabilidade atual, verificando a integridade das configurações (`promtool check`), inspecionando os targets ativos do Prometheus, mapeando filtros hardcoded em painéis do Grafana e gerando o documento de baseline `OBSERVABILITY_BASELINE.md`.

### Arquivos Alvo
- `backend/observability/prometheus/prometheus.yml` (leitura)
- `backend/observability/prometheus/alerts.yml` (leitura)
- `backend/observability/grafana/provisioning/dashboards/promptuario.json` (leitura e análise)
- `backend/observability/OBSERVABILITY_BASELINE.md` (novo arquivo)

### Prompt da Fase O0

```text
Atue como Engenheiro de Confiabilidade de Sites (SRE) Especialista em Prometheus, Alertmanager e Grafana.
Nosso objetivo é conduzir a Fase O0 (Diagnóstico Inicial e Baseline Somente Leitura) da stack de observabilidade do PROMPTUÁRIO.

NÃO modifique nenhum arquivo de runtime nesta fase. Execute a análise e gere o relatório de baseline.

Tarefas Obrigatórias:

1. Auditoria de Configuração e Regras:
   - Verifique a sintaxe de "backend/observability/prometheus/prometheus.yml" e "backend/observability/prometheus/alerts.yml" via promtool:
     * promtool check config prometheus.yml
     * promtool check rules alerts.yml
   - Atestar se há erros de parsing, sintaxe PromQL inválida ou duplicação de regras.

2. Varredura dos Dashboards Grafana ("backend/observability/grafana/provisioning/dashboards/promptuario.json"):
   - Inspecione todas as queries dos painéis (targets/expr) e variáveis de template.
   - Identifique e liste todos os painéis que possuem filtros rígidos para os serviços legados:
     * job="patient-service"
     * job="clinical-service"
     * service="patient-service"
     * service="clinical-service"
   - Verifique se os painéis estão preparados para renderizar "service='patient-clinical-service'".

3. Coleta de Métricas de Baseline (via curl na API do Prometheus e nos endpoints /metrics):
   - Inspecione os targets ativos: "http://localhost:9090/api/v1/targets" (garantir que gateway, iam-service, patient-clinical-service, ai-service, reporting-service e rabbitmq estejam UP).
   - Registre os valores correntes das principais métricas de negócio e infraestrutura:
     * up{job="patient-clinical-service"}
     * http_requests_total{service="patient-clinical-service"}
     * patients_registered_total
     * consultations_total
     * prescriptions_total
     * P95 de http_request_duration_seconds_bucket
     * rabbitmq_queue_messages_ready
     * audit_events_failed_total

4. Geração do Documento "backend/observability/OBSERVABILITY_BASELINE.md":
   - Registre:
     * Data e hora da auditoria.
     * Status de cada target do Prometheus.
     * Lista completa dos painéis do Grafana que necessitam de atualização na Fase O1.
     * Valores de referência (baseline) de latência, tráfego e erros.
     * Confirmação de que o pipeline OTel/Jaeger está operacional em "otel-collector:4318" e "jaeger:16686".

Gere o arquivo "backend/observability/OBSERVABILITY_BASELINE.md" estruturado, profissional e com dados comprobatórios.
```

### Critérios de Aceite
- Execução de `promtool check` com 0 erros.
- Mapeamento exato de todos os painéis do Grafana que contenham filtros legados.
- Criação do documento `OBSERVABILITY_BASELINE.md` documentando o estado inicial sem alterações nos arquivos de produção.

---

## Fase O1: Atualização de Regras de Alerta (`alerts.yml`) e Dashboards Grafana

### Objetivo
Atualizar o arquivo `alerts.yml` e o dashboard provisionado `promptuario.json` do Grafana para contemplar o serviço unificado `patient-clinical-service` e o `backup-service`, utilizando seletores em `OR` (`=~"patient-clinical-service|patient-service|clinical-service"`) para garantir continuidade de monitoramento sem quebrar a visualização de dados históricos.

### Arquivos Alvo
- `backend/observability/prometheus/alerts.yml`
- `backend/observability/prometheus/prometheus.yml` (inclusão do scrape do backup-service)
- `backend/observability/grafana/provisioning/dashboards/promptuario.json`

### Prompt da Fase O1

```text
Atue como Especialista em Observabilidade e Dashboards Grafana.
Vamos executar a Fase O1 para atualizar as regras de alerta e os painéis do Grafana com retrocompatibilidade garantida.

Diretriz Fundamental: NUNCA remova as séries legadas. Duplique ou use expressões regulares inclusivas para que o período de transição mostre tanto dados legados quanto do serviço unificado.

1. Atualização do "backend/observability/prometheus/prometheus.yml":
   - Assegure que os jobs de scrape contemplem a topologia completa:
     * gateway (porta 8000)
     * iam-service (porta 8000)
     * patient-clinical-service (porta 8000)
     * ai-service (porta 8000)
     * reporting-service (porta 8000)
     * backup-service (porta 8000) -> adicionar scrape job na porta interna 8000 (métricas Micrometer em /actuator/prometheus).
     * rabbitmq (porta 15692)
     * node-exporter (porta 9100)

2. Atualização do "backend/observability/prometheus/alerts.yml":
   - Atualize os alertas para que monitorem "patient-clinical-service":
     * Alerta "ServiceDown": certifique-se de que dispare para o job "patient-clinical-service".
     * Alertas de Latência e Erros 5xx ("HighErrorRate", "HighLatency"): use "service=~'patient-clinical-service|patient-service|clinical-service'".
     * Alertas específicos de Prescrições e Auditoria: assegure que monitorem as emissões do unificado.
     * Adicione os alertas específicos do Backup Service:
       - "BackupJobFailed": aumento de falhas em 1h.
       - "BackupMissing24h": ausência de backup bem-sucedido nas últimas 24h.
       - "BackupArtifactTooSmall": dump inferior a 100KB.
   - Execute "promtool check rules alerts.yml" e garanta validação 100% limpa.

3. Atualização do Dashboard Grafana ("backend/observability/grafana/provisioning/dashboards/promptuario.json"):
   - Atualize as variáveis de template de serviço para incluir "patient-clinical-service".
   - Nos painéis de:
     * "Taxa de Requisições por Serviço"
     * "Latência P95 por Endpoint"
     * "Taxa de Erros 5xx"
     * "Contadores de Negócio (Consultas, Pacientes, Prescrições)"
     Atualize as queries PromQL para incluir "patient-clinical-service" mantendo suporte em OR às séries históricas.
   - Crie uma linha (Row) colapsável ou painéis dedicados para métricas do "Patient-Clinical Service" (prontuários SOAP, agendamentos e geração de PDFs assíncronos).

Forneça os arquivos modificados e comandos para recarregar as configurações no Prometheus ("curl -X POST http://localhost:9090/-/reload") e validar o Grafana.
```

### Critérios de Aceite
- Validação do `promtool check` aprovada com 0 erros e 0 warnings.
- Recarregamento do Prometheus com `reload` via API bem-sucedido.
- Painéis do Grafana renderizando métricas ativas do `patient-clinical-service` sem erros de "No Data".

---

## Fase O2: Rastreamento Distribuído (OTel/Jaeger), Correlação de Logs e Validação de SLOs

### Objetivo
Validar e enriquecer a rastreabilidade ponta a ponta com OpenTelemetry e Jaeger, confirmando a propagação dos cabeçalhos de contexto (`X-Request-Id`, `X-Correlation-Id`), testando a correlação de logs no Grafana/Loki e formalizando os SLOs e SLAs do serviço unificado.

### Arquivos Alvo
- `backend/observability/otel-collector-config.yml`
- `backend/observability/SLO_DEFINITIONS.md` (novo arquivo)
- `backend/shared/observability.py` (ou interceptores Java equivalentes)

### Prompt da Fase O2

```text
Atue como Engenheiro de Confiabilidade de Sistemas (SRE) Especialista em Tracing Distribuído (OpenTelemetry/Jaeger) e Engenharia de Resiliência.
Vamos conduzir a Fase O2 focada em correlação de traces e formalização de SLOs (Service Level Objectives).

1. Validação do Pipeline OpenTelemetry e Jaeger:
   - Inspecione "backend/observability/otel-collector-config.yml":
     * Garanta que o OTel Collector receba traces OTLP via gRPC (porta 4317) e HTTP (porta 4318) e exporte para o Jaeger ("jaeger:4317").
   - Teste de Propagação de Trace:
     * Simule uma requisição completa: Cliente -> API Gateway -> Patient-Clinical Service -> Publicação RabbitMQ -> Worker de Prescrição.
     * Valide que o mesmo "X-Correlation-Id" esteja presente:
       1. Nos headers HTTP de resposta do Gateway.
       2. Nos logs estruturados dos microsserviços.
       3. Como atributo do Span no Jaeger sob o serviço "patient-clinical-service".

2. Formalização dos SLOs do Sistema ("backend/observability/SLO_DEFINITIONS.md"):
   - Crie o documento formal de SLOs/SLIs cobrindo:
     * **Disponibilidade (Availability SLI):**
       - SLO: 99.5% de requisições com status != 5xx em janela móvel de 30 dias.
       - PromQL: `sum(rate(http_requests_total{status_code!~"5.."}[30d])) / sum(rate(http_requests_total[30d])) * 100`
     * **Latência (Latency SLI):**
       - SLO: 95% das requisições atendidas em menos de 2000ms (P95 < 2s).
       - PromQL: `histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{service="patient-clinical-service"}[5m])) by (le))`
     * **Integridade de Auditoria (Audit SLI):**
       - SLO: 100% de eventos de auditoria persistidos sem falha (`rate(audit_events_failed_total[1h]) == 0`).
     * **Fila de Mensageria (Queue Health SLI):**
       - SLO: Mensagens prontas no RabbitMQ < 1000 por mais de 5 minutos.
     * **Disjuntor de Borda (Circuit Breaker SLI):**
       - SLO: Estado do Circuit Breaker no Gateway == 0 (Closed) para todos os microsserviços saudáveis.

3. Painel de SLO no Grafana:
   - Adicione no dashboard do Grafana um painel de "Error Budget" e "SLO Burn Rate" baseado nas fórmulas acima.

Gere o documento "backend/observability/SLO_DEFINITIONS.md" e o trecho de configuração do Grafana para visualização dos SLOs.
```

### Critérios de Aceite
- Traces do `patient-clinical-service` visíveis no Jaeger (`http://localhost:16686`) com Spans correlacionados pelo `correlation_id`.
- Documento `SLO_DEFINITIONS.md` consolidado com expressões PromQL exatas e metas mensuráveis.

---

## Fase O3: Corte Seguro (Cutover), Limpeza de Filtros Legados e Documentação

### Objetivo
Realizar o cutover definitivo da observabilidade (alinhado à Fase 12 de unificação do sistema): aposentar formalmente as referências aos jobs legados nos dashboards e documentações (sem apagar séries históricas), atualizar os diagramas de arquitetura e validar a stack completa em ambiente integrado.

### Arquivos Alvo
- `backend/observability/grafana/provisioning/dashboards/promptuario.json`
- `backend/observability/prometheus/prometheus.yml` (comentários e limpeza)
- `DOCUMENTATION/ETAPA_11_Observabilidade_Distribuida_Prometheus_Grafana_Loki.md` (atualização)
- `DOCUMENTATION/STATUS_GERAL_DO_SISTEMA.md` (atualização)
- `backend/Makefile` (atualização do comando `make health`)

### Prompt da Fase O3

```text
Atue como Arquiteto de Software e Engenheiro de Observabilidade.
Vamos conduzir a Fase O3 (Corte Seguro e Alinhamento Documental).

Tarefas Obrigatórias:

1. Limpeza dos Dashboards Grafana ("promptuario.json"):
   - Remova variáveis de filtro exclusivas dos serviços legados desativados ("patient-service" e "clinical-service"), consolidando no "patient-clinical-service".
   - Marque consultas históricas legadas como aposentadas ("retired") se necessário, assegurando que o painel padrão abra apontando diretamente para o serviço unificado.
   - Salve o JSON atualizado e valide se o Grafana recarrega sem erros de parsing.

2. Atualização dos Documentos Arquiteturais:
   - "DOCUMENTATION/ETAPA_11_Observabilidade_Distribuida_Prometheus_Grafana_Loki.md":
     * Atualizar a topologia de monitoramento para refletir os microsserviços vigentes: Gateway (:8000), IAM Service (:8001), Patient-Clinical Service (:8002), AI Service (:8004), Reporting Service (:8005), Backup Service (:8006).
     * Atualizar tabela de métricas customizadas e portas de scrape.
   - "DOCUMENTATION/STATUS_GERAL_DO_SISTEMA.md":
     * Registrar o status da migração de observabilidade como "CONCLUÍDO / V2 UNIFICADA".
   - "backend/Makefile":
     * Atualizar comandos de checagem de saúde e métricas ("make health", "make status", "make metrics-check") para consultar a porta 8002 do "patient-clinical-service" e 8006 do "backup-service".

3. Checklist de Validação Pré-Encerramento da Fase O3:
   - [ ] Todos os targets do Prometheus reportando "UP = 1".
   - [ ] Nenhuma regra de alerta disparada indevidamente em "http://localhost:9090/alerts".
   - [ ] Dashboards do Grafana renderizando painéis de latência, vazão, erros e métricas de negócio.
   - [ ] Traces no Jaeger integrados com o OTel Collector.

Forneça os arquivos alterados e um diff explicativo das mudanças.
```

### Critérios de Aceite
- Todos os documentos técnicos alinhados à topologia unificada.
- Comandos do `Makefile` atualizados e funcionando.
- Painéis do Grafana e alertas do Prometheus limpos, estáveis e com dados fluindo.

---

## Fase O4: Janela de Observação Pós-Corte (7 Dias) e Relatório Final

### Objetivo
Monitorar ativamente a stack de observabilidade durante a janela de estabilização pós-cutover (7 dias), acompanhando o disparo de alertas, queima de Error Budget de SLOs e anomalias de cardinalidade de métricas, gerando o relatório final `OBSERVABILITY_CUTOVER_REPORT.md`.

### Arquivos Alvo
- `backend/observability/OBSERVABILITY_CUTOVER_REPORT.md` (novo arquivo)

### Prompt da Fase O4

```text
Atue como Engenheiro de Confiabilidade de Sites (SRE) Líder.
Estamos concluindo o ciclo de migração de observabilidade. Vamos executar a Fase O4 (Janela de Observação e Relatório Final de Encerramento).

Tarefas Obrigatórias:

1. Roteiro de Inspeção dos 7 Dias de Estabilização:
   - Acompanhamento diário dos seguintes alertas prioritários:
     * "ServiceDown" (garantir zero downtime dos microsserviços críticos).
     * "HighErrorRate" (taxa de erro 5xx mantida abaixo de 1%).
     * "HighLatency" (P95 mantido abaixo de 2s sob carga).
     * "QueueDepthIncreasing" (filas RabbitMQ esvaziando sem retenção).
     * "LoginFailuresSpike" (detecção de ataques de força bruta no IAM).
     * "AiErrorsSpike" (taxa de erro no LLM do AI Service).
     * "CircuitBreakerOpen" (resiliência no Gateway).
     * "BackupMissing24h" (garantia de cumprimento do RPO do Backup Service).

2. Auditoria de Cardinalidade e Performance do TSDB do Prometheus:
   - Executar consulta para verificar métricas com excesso de labels/séries:
     `topk(10, count by (__name__) ({__name__=~".+"}))`
   - Garantir que a memória do container do Prometheus permaneça estável sem picos de OOM.

3. Geração do Relatório Final "backend/observability/OBSERVABILITY_CUTOVER_REPORT.md":
   - Estruture o relatório contendo:
     * Resumo Executivo da Migração da Observabilidade.
     * Tabela comparativa do Baseline (Fase O0) vs Estado Pós-Cutover (Fase O4).
     * Cumprimento de SLOs (Disponibilidade real obtida, Latência P95 média, Taxa de Erros).
     * Histórico de incidentes ou alertas disparados durante a janela de observação e ações corretivas adotadas.
     * Declaração formal de aposentadoria dos componentes legados e encerramento com sucesso da migração.

Gere o documento "backend/observability/OBSERVABILITY_CUTOVER_REPORT.md" completo e pronto para aprovação técnica da equipe de liderança.
```

### Critérios de Aceite
- Documento `OBSERVABILITY_CUTOVER_REPORT.md` gerado com métricas reais pós-estabilização.
- Todos os SLOs atingidos dentro das metas estipuladas.
- Encerramento formal do ciclo de migração da observabilidade.

---

## Matriz de Riscos e Planos de Rollback

| Fase | Risco Principal | Impacto | Medida Mitigatória / Rollback |
| :--- | :--- | :--- | :--- |
| **O0** | Consulta pesada ao Prometheus travando o TSDB | Lentidão temporária no scrape | Utilizar apenas consultas pontuais e leitura estática de arquivos de configuração. |
| **O1** | Erro de sintaxe PromQL em `alerts.yml` | Prometheus falha ao recarregar regras | Validação prévia obrigatória com `promtool check rules`. Rollback com `git revert alerts.yml`. |
| **O1** | Painéis do Grafana vazios por filtro quebrado | Perda de visibilidade das operações | Uso de regex inclusiva (`patient-clinical-service|patient-service|clinical-service`). |
| **O2** | Quebra de propagação de `correlation_id` | Perda de rastreabilidade entre serviços | Testes automatizados de cabeçalhos no Gateway e fallback de geração local de ID em cada microsserviço. |
| **O3** | Deleção acidental de séries do Prometheus | Perda de dados históricos para auditoria | Séries antigas **nunca são deletadas** do disco TSDB; apenas filtros de dashboards são ajustados. |

---

## Comandos Rápidos de Validação

```powershell
# 1. Validar sintaxe da configuração do Prometheus:
docker exec backend-prometheus-1 promtool check config /etc/prometheus/prometheus.yml

# 2. Validar sintaxe das regras de alerta do Prometheus:
docker exec backend-prometheus-1 promtool check rules /etc/prometheus/alerts.yml

# 3. Recarregar configurações no Prometheus em tempo de execução:
curl.exe -X POST http://localhost:9090/-/reload

# 4. Inspecionar alvos ativos de monitoramento:
curl.exe -s http://localhost:9090/api/v1/targets | jq .

# 5. Coletar métricas do serviço clínico unificado:
curl.exe -s http://localhost:8002/metrics | Select-String "patients_registered_total|consultations_total"

# 6. Coletar métricas do serviço de backup:
curl.exe -s http://localhost:8006/actuator/prometheus | Select-String "backup_"
```
