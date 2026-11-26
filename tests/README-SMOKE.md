# Smoke tests operacionais

Suba o ambiente Java a partir de `backend-java`:

```powershell
Set-Location backend-java
docker compose up -d --build
Set-Location ..
```

Execute a validação centralizada:

```powershell
.\tests\run-tests.ps1 -Suite smoke
```

O smoke test verifica os endpoints funcionais do gateway, IAM, patient-clinical, IA e reporting, o estado dos serviços essenciais do Compose, PostgreSQL, RabbitMQ, Redis e MinIO. Também consulta Prometheus, Grafana e Jaeger, mas falhas exclusivamente de telemetria são exibidas separadamente e não mascaram falhas funcionais.
