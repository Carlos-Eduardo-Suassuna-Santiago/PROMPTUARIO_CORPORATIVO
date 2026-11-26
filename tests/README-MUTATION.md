# Testes de mutação e critérios de aceite

Testes de mutação devem ser executados somente depois da suíte unitária estar verde. A execução completa é indicada para pipeline noturno ou sob demanda; em pull requests, limitar ao módulo/escopo alterado.

## Execução centralizada

```powershell
.\tests\run-tests.ps1 -Suite mutation -MutationScope java -Service patient-clinical-service
.\tests\run-tests.ps1 -Suite mutation -MutationScope python
.\tests\run-tests.ps1 -Suite mutation -MutationScope frontend
```

O runner Java usa PIT com score mínimo de 70%. O objetivo para regras críticas de autenticação, permissões, persistência, transações e eventos é evoluir para 80%. O runner Python usa Mutmut e o Frontend usa Stryker com Vitest.

## Execução direta

```powershell
Set-Location backend-java/patient-clinical-service
mvn test org.pitest:pitest-maven:mutationCoverage -DmutationThreshold=70

Set-Location ..\..\backend
mutmut run
mutmut results
mutmut html

Set-Location ..\frontend
npx stryker run
```

Artefatos esperados:

- Java: `backend-java/<serviço>/target/pit-reports`;
- Python: resultados do Mutmut e relatório HTML;
- Frontend: relatório do Stryker;
- suíte base: resultados JUnit e coverage já produzidos pelos runners existentes.

Mutantes sobreviventes em segurança, permissões, persistência, regras clínicas ou consumidores de eventos bloqueiam o CI, salvo justificativa registrada com escopo, risco e prazo. Cobertura de linhas não substitui o score de mutação.

## Critérios de aceite

Devem passar sem falhas:

```powershell
Set-Location backend-java/patient-clinical-service
mvn verify

Set-Location ..\..\frontend
npm run type-check
npm run lint
npm run test
npm run build
```

Também devem estar documentados ou aprovados no ambiente integrado: Testcontainers/Newman, fluxo E2E prioritário, health checks Docker, relatórios JUnit/coverage/Newman e limitações de infraestrutura ou dados de teste.
