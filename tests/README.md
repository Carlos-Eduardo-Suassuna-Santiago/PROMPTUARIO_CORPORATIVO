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
