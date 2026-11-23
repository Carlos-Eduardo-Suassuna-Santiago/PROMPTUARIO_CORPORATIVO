# Testes Python/FastAPI

Os testes Python ficam em `tests/python/`, fora dos serviços, com uma entrada única para a suíte.

## Instalação

```powershell
Set-Location backend
python -m pip install -r requirements-test.txt
python -m pip install -r ai-service/requirements.txt
python -m pip install -r reporting-service/requirements.txt
```

## Execução

```powershell
pytest -q --cov=. --cov-report=term-missing
```

Ou, a partir da raiz:

```powershell
.\tests\run-tests.ps1 -Suite python
.\tests\run-tests.ps1 -Suite python -Coverage
```

Os testes unitários usam mocks, `AsyncMock` e fakes para PostgreSQL, RabbitMQ, Redis, S3, Celery e APIs externas. Testes que exigirem Docker ou serviços reais devem usar a marca `integration` e ficar fora da execução padrão.
