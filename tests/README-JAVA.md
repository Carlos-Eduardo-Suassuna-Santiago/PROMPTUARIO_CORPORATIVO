# Suíte central de testes Java — Promptuário

Esta pasta é a entrada única para executar os testes Java do sistema. O runner coordena `iam-service`, `patient-clinical-service`, `gateway`, `reporting-service` e `backup-service` sem exigir que o desenvolvedor conheça a estrutura interna de cada módulo.

## Pré-requisitos

- JDK 21 configurado em `JAVA_HOME`;
- Maven 3.9+ no `PATH`;
- PowerShell 5.1+ ou PowerShell 7+;
- Docker somente para os testes de integração que usam Testcontainers.

Os módulos já declaram `spring-boot-starter-test`, que fornece JUnit 5, Mockito, AssertJ e Spring Boot Test.

## Execução centralizada

Na raiz do repositório:

```powershell
# Testes unitários: não usa banco compartilhado, RabbitMQ, Redis ou APIs externas
.\tests\run-tests.ps1 -Suite java -Mode unit

# Mesmo recorte unitário até a fase Maven verify
.\tests\run-tests.ps1 -Suite java -Mode verify

# Um módulo específico
.\tests\run-tests.ps1 -Suite java -Mode unit -Service patient-clinical-service

# Testes de integração; requer Docker quando o teste usar Testcontainers
.\tests\run-tests.ps1 -Suite java -Mode integration
```

O runner seleciona as classes Java que possuem `@Test`. No modo unitário, exclui classes em pacotes `integration` e classes terminadas em `IntegrationTest`. No modo integração, executa somente esse recorte. `-FailFast` interrompe o Surefire após a primeira falha.

## Execução direta por módulo

Para depuração local, o comando Maven continua disponível:

```powershell
Set-Location backend-java/patient-clinical-service
mvn test
mvn verify
```

Para respeitar a regra de unidade sem infraestrutura compartilhada, prefira o runner central. A execução direta do Maven segue o conjunto configurado pelo próprio `pom.xml` e pode incluir testes de contexto ou integração do módulo.

## Critérios de implementação

- `@WebMvcTest` para controllers e validação HTTP;
- Mockito para serviços, clientes externos, Redis, S3 e dependências AMQP;
- `@DataJpaTest` para repositórios e mapeamentos JPA;
- testes de entidades, DTOs e Jakarta Validation;
- testes de CORS, JWT/OAuth2 e regras de segurança;
- testes de consumidores/publicadores AMQP por mocks ou contratos;
- banco, RabbitMQ, Redis e Testcontainers somente em testes de integração explícitos.

## Diagnóstico

Relatórios Surefire ficam em `backend-java/<serviço>/target/surefire-reports/`. Para isolar uma falha, use `-Service`. Para integração, confirme Docker e portas livres. Nunca faça testes unitários apontarem para um banco compartilhado.

O arquivo [`README.md`](README.md) mantém as instruções existentes para Python e Newman.
