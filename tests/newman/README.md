# Testes Newman

Smoke/integration tests da integração Gateway → IAM → Patient/Clinical → Reporting → AI.

A coleção contém 30 requisições e 31 asserções e valida health checks, autenticação, usuários,
pacientes (lista, busca, detalhe, resumo e subrecursos), consultas, prontuários,
reporting, auditoria, diagnóstico de IA, contratos inválidos e autorização de
rotas protegidas. A coleção valida 401 para acesso anônimo e rejeita 404 no diagnóstico de IA; respostas 403 devem ser exercitadas quando um token válido sem a role exigida estiver disponível no ambiente. O 404 de slots públicos é esperado apenas porque a agenda usada é deliberadamente inválida, e sua resposta precisa conter `detail`. Os testes de escrita destrutivos não são executados por padrão,
evitando alterar dados do ambiente; para eles, use um banco descartável de
integração e uma coleção específica de dados de teste.

## Pré-requisitos

Suba a stack:

```powershell
docker compose -f backend-java/docker-compose.yml up -d
```

Newman pode ser executado via `npx`, sem instalação global:

```powershell
npx --yes newman run tests/newman/promptuario.postman_collection.json `
  -e tests/newman/promptuario.postman_environment.json `
  --reporters cli,junit `
  --reporter-junit-export tests/newman/results/newman.xml
```

Para usar outra credencial sem editar o arquivo versionado:

```powershell
npx --yes newman run tests/newman/promptuario.postman_collection.json `
  -e tests/newman/promptuario.postman_environment.json `
  --env-var "email=admin@promptuario.health" `
  --env-var "password=Admin@12345"
```

Ou use o script PowerShell, que também gera `tests/newman/results/newman.xml`:

```powershell
.\tests\newman\run-newman.ps1 -Password "Admin@12345"
```

O `patientId` é descoberto automaticamente na listagem. Também pode ser fixado com `--env-var patientId=...`.
