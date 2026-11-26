# Carga e resiliência

A suíte usa k6 e deve ser executada somente com dados e credenciais descartáveis. O cenário padrão é somente leitura: login, listagens paginadas de pacientes e consultas. A solicitação de IA é ativada quando `K6_PATIENT_ID` é informado e cria jobs assíncronos de teste.

## Carga

Suba o Compose Java e defina credenciais de teste:

```powershell
Set-Location backend-java
docker compose up -d --build
Set-Location ..

$env:K6_EMAIL = 'load-test@example.invalid'
$env:K6_PASSWORD = '<senha-descartável>'
$env:K6_PATIENT_ID = '<id-do-paciente-de-teste>'
.\tests\run-tests.ps1 -Suite load
```

Parâmetros do runner específico:

```powershell
.\tests\load\run-load.ps1 -VUs 25 -Duration 5m
```

O k6 mede p95 de todas as requisições, p95 de login/listagens, taxa de erro HTTP e quantidade de solicitações de IA. Os thresholds padrão são erro inferior a 5%, p95 geral inferior a 1,5 s e p95 de login/listagens inferior a 1,2 s.

## Resiliência

Os seguintes cenários devem ser executados em janela controlada, com dados descartáveis e observação de logs/métricas:

- interromper PostgreSQL, RabbitMQ, Redis e MinIO individualmente e validar timeout, erro controlado, retry e circuit breaker;
- reiniciar containers durante login, listagem, publicação e consumo de eventos;
- reenviar o mesmo evento com o mesmo identificador e confirmar idempotência, sem registros duplicados;
- validar recuperação após `docker compose restart` e retorno dos health checks;
- medir p95, taxa de erro, uso de CPU/memória, filas RabbitMQ, conexões Redis e backlog de jobs de IA.

Falhas induzidas não devem ser executadas automaticamente pelo runner padrão e nunca devem usar banco compartilhado ou dados reais.
