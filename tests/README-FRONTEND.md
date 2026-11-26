# Testes Frontend

O frontend usa React 18, TypeScript, Vite, Vitest, Testing Library, ESLint e Playwright.

## Execução unitária e de verificação

```powershell
Set-Location frontend
npm ci
npm run type-check
npm run lint
npm run test
npm run build
```

Ou pela switch centralizada:

```powershell
.\tests\run-tests.ps1 -Suite frontend -Mode verify
```

## E2E

Suba frontend e backend pelo Compose antes do Playwright:

```powershell
Set-Location backend-java
docker compose up -d --build
Set-Location ..
docker compose -f frontend/docker-compose.yml up -d --build
Set-Location frontend
$env:E2E_FRONTEND_URL = 'http://127.0.0.1'
npm run test:e2e
$env:E2E_FRONTEND_URL = $null
```

O Playwright usa `E2E_FRONTEND_PORT` quando necessário e, por padrão, acessa `http://127.0.0.1:4173`. Os testes E2E devem cobrir autenticação, navegação, RBAC, estados de erro/loading/vazio e integração com respostas paginadas da API.
