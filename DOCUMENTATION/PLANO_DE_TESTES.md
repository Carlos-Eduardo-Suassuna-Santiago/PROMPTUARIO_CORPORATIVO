# Plano de Testes — Promptuário

Plano alinhado à arquitetura atual: serviços Spring Boot 3.3.4/Java 21 em `backend-java`, serviços auxiliares Python/FastAPI em `backend`, frontend React 18 + TypeScript/Vite e execução local por Docker Compose.

## 1. Matriz de cobertura

| Camada | Tecnologias | Objetivo | Execução principal |
|---|---|---|---|
| Unidade Java | JUnit 5, Mockito, Spring Boot Test | regras, serviços, controllers e segurança | `mvn test` |
| Integração Java | Testcontainers (PostgreSQL, RabbitMQ, LocalStack/MinIO), Flyway, JPA | persistência, migrações, eventos e S3 | `mvn verify` |
| Unidade Python | pytest, pytest-asyncio, coverage | FastAPI, schemas e workers auxiliares | `pytest` |
| Frontend | Vitest, Testing Library, TypeScript, ESLint | componentes, stores, hooks e validações | `npm run test` |
| E2E | Playwright | fluxos reais navegador → gateway → serviços | `npm run test:e2e` |
| Contrato/API | Newman/Postman | endpoints, autenticação, RBAC e payloads | `tests/newman/run-newman.ps1` |
| Operacional | Docker Compose, Actuator, Prometheus | health, dependências e observabilidade | smoke checks |

## 2. Testes unitários

### 2.1 Backend Java

Os módulos Spring Boot (`iam-service`, `patient-clinical-service`, `gateway`, `reporting-service` e demais serviços Java) devem cobrir entidades, DTOs Jakarta Validation, mapeamentos JPA, regras de negócio, controllers, CORS, JWT/OAuth2 e consumidores/publicadores AMQP.

```powershell
Set-Location backend-java/patient-clinical-service
mvn test
mvn verify
```

Usar `@WebMvcTest` para controllers, Mockito para dependências externas e `@DataJpaTest` para repositórios. Não conectar a banco compartilhado nos testes unitários.

### 2.2 Serviços Python auxiliares

Aplicar pytest aos serviços FastAPI (incluindo IA, reporting e workers), com mocks para PostgreSQL, RabbitMQ, Redis e APIs externas.

```powershell
Set-Location backend
pytest -q --cov=. --cov-report=term-missing
```

## 3. Integração e persistência

Os testes de integração Java devem usar Testcontainers para PostgreSQL 15, RabbitMQ e LocalStack/MinIO, executando as migrações Flyway desde um banco vazio. Validar JPA/JSONB, constraints, transações, publicação/consumo de eventos, geração de PDF e upload S3.

```powershell
Set-Location backend-java/patient-clinical-service
mvn verify
```

Para o ambiente integrado:

```powershell
docker compose -f backend-java/docker-compose.yml up -d --build
docker compose -f backend-java/docker-compose.yml ps
```

Os testes devem usar dados descartáveis e nunca executar `DROP` ou seed destrutivo em bancos de desenvolvimento.

## 4. Testes de contrato e API (Newman)

A coleção `tests/newman/promptuario.postman_collection.json` cobre gateway, health checks, login, usuários, pacientes, consultas, prontuários, relatórios, auditoria, slots públicos e IA. A execução validada contém 30 requisições e 31 asserções.

```powershell
.\tests\newman\run-newman.ps1 -Password "<senha-do-admin>"
```

O script exige uma senha explícita, executa com `--bail` e gera `tests/newman/results/newman.xml`. Validar também respostas 401/403 para RBAC e payloads inválidos; não considerar 404 de endpoints opcionais como sucesso sem justificativa documentada.

## 5. Frontend

O frontend usa React, TypeScript, Vite, React Router, TanStack Query, Zustand, Axios, React Hook Form, Zod, Tailwind e Recharts.

```powershell
Set-Location frontend
npm ci
npm run type-check
npm run lint
npm run test
npm run build
npm run test:e2e
```

Cobrir autenticação, refresh/expiração de token, CORS, filtros, estados de loading/erro/vazio, formulários, navegação, RBAC, visualização de prontuários/análises e tratamento de respostas paginadas. O Playwright deve rodar contra frontend e backend iniciados pelo Compose.

## 6. Smoke tests e health checks

Após o Compose estar saudável, verificar:

```powershell
curl.exe http://localhost:8000/healthz
curl.exe http://localhost:8000/healthz/services
curl.exe http://localhost:8000/actuator/health
```

Validar gateway, IAM, patient-clinical, reporting, IA, PostgreSQL, RabbitMQ, Redis e MinIO. Falha de OTEL/telemetria deve ser registrada separadamente e não mascarar indisponibilidade funcional.

## 7. Fluxo E2E prioritário

1. Login de paciente e médico (credencial local ou OAuth configurado).
2. Criação/associação do perfil do paciente.
3. Agendamento, confirmação e cancelamento conforme RBAC.
4. Criação do prontuário SOAP e atualização automática do status da consulta.
5. Emissão de prescrição/atestado e geração assíncrona do PDF.
6. Solicitação de análise de IA, acompanhamento de status e exibição visual do resultado.
7. Consulta dos logs de auditoria com usuário e IP.

## 8. Testes de segurança

Os testes de segurança devem ser executados em ambiente descartável, sem dados reais, e abranger todos os serviços Java, APIs Python, gateway e frontend.

### 8.1 Autenticação e autorização

- Login com senha correta, senha incorreta, usuário inexistente, usuário inativo e credenciais vazias.
- JWT ausente, malformado, expirado, assinado com chave incorreta e com algoritmo não permitido.
- Refresh token válido, revogado, expirado e reutilizado.
- Matriz RBAC para `ADMIN`, `DOCTOR`, `ATTENDANT` e `PATIENT`, garantindo 401 para ausência de autenticação e 403 para privilégio insuficiente.
- Tentativa de acessar ou alterar paciente, prontuário, consulta, auditoria e relatórios pertencentes a outro usuário.
- Validação de que endpoints administrativos não aceitam apenas alterações no payload para elevar a role.

Executar os cenários de 401/403 também pela coleção Newman:

```powershell
.\tests\newman\run-newman.ps1 -Password "<senha-do-admin>"
```

### 8.2 OWASP e validação de entrada

- SQL/NoSQL injection em filtros, busca, ordenação e parâmetros de relatório.
- XSS refletido e armazenado em nomes, observações, prontuários, documentos e resultados de IA.
- Mass assignment nos DTOs, campos desconhecidos, tipos incorretos, limites de tamanho e payloads JSON profundamente aninhados.
- Path traversal e nomes maliciosos em documentos e chaves S3/MinIO.
- Upload limitado por tamanho, MIME real, extensão e conteúdo; rejeitar arquivos executáveis e PDFs inválidos.
- Rate limiting e proteção contra brute force no login, refresh e endpoints de IA.

### 8.3 CORS, headers e transporte

- Permitir somente as origens configuradas do frontend; garantir um único valor `Access-Control-Allow-Origin`.
- Validar preflight `OPTIONS`, métodos e headers permitidos, credenciais e rejeição de origens desconhecidas.
- Conferir headers de segurança: HSTS, CSP quando aplicável, `X-Content-Type-Options`, `X-Frame-Options`, cache e política de referrer.
- Em ambientes não locais, exigir HTTPS e nunca expor segredos, tokens ou senhas em URL, logs ou mensagens de erro.

### 8.4 Privacidade, auditoria e mensageria

- Mascarar CPF, tokens, senhas, segredos S3 e dados clínicos em logs.
- Registrar usuário, role, IP, timestamp, operação e recurso nos eventos de auditoria; validar também requisições anônimas e falhas de autenticação.
- Confirmar que eventos RabbitMQ não carregam credenciais ou PII desnecessária, possuem correlação e são idempotentes.
- Testar retry/DLQ, replay e duplicação de eventos sem criar registros duplicados.
- Validar anonimização LGPD e impossibilidade de recuperar dados anonimizados por endpoints ou arquivos S3 antigos.

### 8.5 Dependências e análise automatizada

```powershell
# Frontend
Set-Location frontend
npm audit --audit-level=high

# Python
Set-Location ..\backend
pip-audit
bandit -r .

# Java (na raiz do serviço)
Set-Location ..\backend-java\patient-clinical-service
mvn org.owasp:dependency-check-maven:check
```

As falhas críticas/altas devem bloquear o CI. Falsos positivos precisam de justificativa registrada, versão afetada e prazo de correção.

## 9. Carga e resiliência

Usar k6 ou Locust para carga de login, listagens paginadas, consultas e IA; medir p95, taxa de erro e consumo de recursos. Validar timeouts, circuit breakers, indisponibilidade de PostgreSQL/RabbitMQ/Redis/MinIO, idempotência de eventos e recuperação após reinício dos containers.

## 10. Critérios de aceite

- `mvn verify`, `npm run type-check`, `npm run lint`, `npm run test` e `npm run build` sem falhas.

## Testes de mutaÃ§Ã£o

Testes de mutaÃ§Ã£o verificam a qualidade dos testes introduzindo pequenas alteraÃ§Ãµes controladas no cÃ³digo. Cada mutaÃ§Ã£o deve ser eliminada por algum teste; mutaÃ§Ãµes sobreviventes indicam cobertura comportamental insuficiente.

### Java/Spring Boot

Usar PIT (Pitest) nos mÃ³dulos Java, priorizando autenticaÃ§Ã£o/RBAC, serviÃ§os clÃ­nicos, validaÃ§Ãµes, transaÃ§Ãµes e consumidores de eventos. Excluir apenas classes geradas ou sem lÃ³gica, com justificativa.

```powershell
Set-Location backend-java/patient-clinical-service
mvn test org.pitest:pitest-maven:mutationCoverage
```

Publicar `target/pit-reports` e adotar mutation score mÃ­nimo de 70%, evoluindo para 80% nas regras crÃ­ticas. Mutantes sobreviventes em seguranÃ§a, permissÃµes, persistÃªncia e regras clÃ­nicas devem bloquear o CI atÃ© serem cobertos ou justificados.

### Python/FastAPI

Usar Mutmut ou Cosmic Ray nos serviÃ§os auxiliares, com foco em autenticaÃ§Ã£o, schemas Pydantic, regras de IA, consultas e tratamento de erros.

```powershell
Set-Location backend
mutmut run
mutmut results
mutmut html
```

Executar com mocks e fixtures isoladas, sem alterar bancos ou serviÃ§os compartilhados.

### Frontend React/TypeScript

Usar Stryker Mutator com Vitest para componentes, stores Zustand, hooks TanStack Query, validaÃ§Ãµes Zod e transformaÃ§Ãµes de resposta. A cobertura base pode ser executada com `npm run test:coverage`; priorizar estados de erro, loading, autorizaÃ§Ã£o e filtros.

```powershell
Set-Location frontend
npx stryker run
```

### Regras de CI

- Executar mutaÃ§Ã£o completa sob demanda ou em pipeline noturno; em pull requests, limitar ao escopo alterado.
- Manter os testes unitÃ¡rios verdes antes da mutaÃ§Ã£o.
- Arquivar score, mutantes sobreviventes, tempo de execuÃ§Ã£o e exclusÃµes justificadas.
- NÃ£o confundir cobertura de linhas com eficÃ¡cia: linhas cobertas ainda podem gerar mutantes sobreviventes.
- Falha por score abaixo do limite ou por mutante crÃ­tico sobrevivente deve reprovar o pipeline.
## CritÃ©rios de aceite (continuaÃ§Ã£o)

- Testes Testcontainers e Newman sem falhas no ambiente limpo.
- Fluxo E2E prioritário concluído com dados persistidos e eventos auditáveis.
- Health checks e dependências Docker saudáveis.
- Relatórios JUnit/coverage/Newman arquivados no CI.
- Falhas de infraestrutura, dados de teste e limitações conhecidas documentadas separadamente.
