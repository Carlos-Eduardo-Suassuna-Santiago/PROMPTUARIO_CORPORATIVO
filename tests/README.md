# Suíte de testes unitários

`run-unit-tests.ps1` é a entrada única para os testes unitários do backend. Os testes Python ficam em `tests/python/`, separados por serviço. Por padrão, o runner não inicializa banco, RabbitMQ, Redis, MinIO, APIs externas ou Testcontainers.

## Execução

Na raiz do repositório:

```powershell
.\tests\run-unit-tests.ps1 -Suite python -Coverage
.\tests\run-unit-tests.ps1 -Suite java
.\tests\run-unit-tests.ps1
```

Opções úteis:

```powershell
# Um serviço específico
.\tests\run-unit-tests.ps1 -Suite java -Service patient-clinical-service

# Inclui testes marcados como integração e testes Java de integração
.\tests\run-unit-tests.ps1 -IncludeIntegration

# Para no primeiro erro
.\tests\run-unit-tests.ps1 -FailFast
```

O modo Python usa `backend/pytest.ini`, coleta os testes dos serviços auxiliares e exclui o marker `integration` por padrão. O modo Java executa cada `pom.xml` separadamente e seleciona apenas classes unitárias; testes em pacotes `integration` ou terminados em `IntegrationTest` ficam reservados ao modo `-IncludeIntegration`.

Para instalar as ferramentas Python de teste:

```powershell
python -m pip install -r backend/requirements-test.txt
```

## Runner central Java

Para a suíte detalhada de JUnit 5, Mockito e Spring Boot Test, consulte [`README-JAVA.md`](README-JAVA.md).

```powershell
.\tests\run-tests.ps1 -Suite java -Mode unit
.\tests\run-tests.ps1 -Suite java -Mode verify
.\tests\run-tests.ps1 -Suite java -Mode unit -Service patient-clinical-service
.\tests\run-tests.ps1 -Suite java -Mode integration
```

## Runner central Python/FastAPI

Os testes Python ficam centralizados em `tests/python/` e cobrem contratos compartilhados, schemas da IA, schemas/reporting, workers auxiliares e componentes FastAPI isoláveis. PostgreSQL, RabbitMQ, Redis, S3 e APIs externas devem ser substituídos por mocks/fakes nos testes unitários.

```powershell
.\tests\run-tests.ps1 -Suite python
.\tests\run-tests.ps1 -Suite python -Coverage
```

Execução equivalente diretamente no backend:

```powershell
Set-Location backend
pytest -q --cov=. --cov-report=term-missing
```

O runner executa `tests/python` em uma única sessão pytest para consolidar a cobertura.
## Runner central Frontend

O frontend usa React 18, TypeScript, Vite, Vitest, Testing Library, ESLint e Playwright.

```powershell
.\tests\run-tests.ps1 -Suite frontend -Mode unit
.\tests\run-tests.ps1 -Suite frontend -Mode verify
```

O modo `verify` executa `type-check`, `lint`, Vitest e `build`. Para E2E:

```powershell
Set-Location backend-java
docker compose up -d --build
Set-Location ..
docker compose -f frontend/docker-compose.yml up -d --build
.\tests\run-tests.ps1 -Suite frontend -Mode integration
```

Os testes Playwright usam o frontend e a API iniciados pelo Compose. Falhas de backend, autenticação ou rotas ausentes não devem ser tratadas como sucesso.

O roteiro E2E prioritário está documentado em [`README-E2E.md`](README-E2E.md), cobrindo autenticação, perfil, agendamento com RBAC, prontuário SOAP, documentos PDF, análise de IA e auditoria.

Os testes de segurança estão documentados em [`README-SECURITY.md`](README-SECURITY.md) e podem ser executados pela switch com `-Suite security`.

Os testes de carga e resiliência estão documentados em [`README-LOAD.md`](README-LOAD.md) e podem ser executados pela switch com `-Suite load`.

Os testes de mutação e os critérios de aceite estão documentados em [`README-MUTATION.md`](README-MUTATION.md) e podem ser executados pela switch com `-Suite mutation`.

## Smoke tests operacionais

Com o Compose Java iniciado em `backend-java`, execute:

```powershell
.\tests\run-tests.ps1 -Suite smoke
```

O runner valida serviços Compose, `healthz`, `healthz/services`, `actuator/health`, os serviços PostgreSQL, portas de RabbitMQ/Redis/MinIO e a liveness do MinIO. Prometheus, Grafana e Jaeger são reportados separadamente como telemetria; falhas funcionais continuam determinando o resultado da suíte.
