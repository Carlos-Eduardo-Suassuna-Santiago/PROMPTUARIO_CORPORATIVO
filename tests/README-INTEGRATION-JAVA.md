# Integração Java — Promptuário

Este documento define a execução centralizada da camada de integração Java em `tests/`.

## Execução

Na raiz do projeto:

```powershell
# Todos os módulos com testes de integração
.\tests\run-tests.ps1 -Suite java -Mode integration

# Apenas o serviço clínico
.\tests\run-tests.ps1 -Suite java -Mode integration -Service patient-clinical-service
```

O modo `integration` sempre executa `mvn verify`, selecionando somente classes em pacotes `integration` ou terminadas em `IntegrationTest`.

## Infraestrutura descartável

Os testes de integração Java devem usar Testcontainers e nunca os bancos da stack de desenvolvimento:

- PostgreSQL 15 para JPA, constraints, transações e migrações Flyway desde um banco vazio;
- RabbitMQ para publicação, consumo e contratos de eventos AMQP;
- LocalStack S3 ou MinIO para upload/download de objetos e geração de PDF;
- dados criados no ciclo de vida do teste e descartados com os containers.

Não executar `DROP`, seed destrutivo ou limpeza em bancos compartilhados. O teste deve receber URLs, credenciais e portas pelos containers, normalmente usando `@DynamicPropertySource`.

O `patient-clinical-service` já possui integração com PostgreSQL, RabbitMQ e LocalStack S3. O IAM possui integração com PostgreSQL; o Gateway possui testes de integração isolados com servidores simulados.

## Ambiente Docker integrado

Para subir a stack operacional separadamente dos Testcontainers:

```powershell
docker compose -f backend-java/docker-compose.yml up -d --build
docker compose -f backend-java/docker-compose.yml ps
```

Os volumes desse Compose não devem ser usados pelos testes unitários ou de integração. Antes de executar a suíte, confirme que o Docker Desktop está ativo:

```powershell
docker version
```

## Escopo de validação

Os testes devem validar persistência, migrações Flyway, mapeamentos JPA/JSONB, constraints, transações, eventos AMQP, consumidores, publicadores, geração de PDF e armazenamento S3. Relatórios Surefire ficam em `backend-java/<serviço>/target/surefire-reports/`.
