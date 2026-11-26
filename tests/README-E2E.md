# Fluxo E2E prioritário

O fluxo Playwright prioritário deve usar dados descartáveis e credenciais de teste fornecidas por variáveis de ambiente. Não usar credenciais administrativas reais nem dados pessoais.

1. Login de paciente e médico, usando credencial local ou OAuth explicitamente configurado.
2. Criação ou associação do perfil do paciente.
3. Agendamento, confirmação e cancelamento respeitando RBAC.
4. Criação do prontuário SOAP e verificação da atualização automática do status da consulta.
5. Emissão de prescrição e atestado, aguardando o processamento assíncrono e validando o PDF.
6. Solicitação de análise de IA, acompanhamento de `PENDING`, `RUNNING`, `COMPLETED` ou `FAILED` e exibição visual do resultado.
7. Consulta dos logs de auditoria, validando usuário e IP.

O cenário de análise de IA já está implementado em `frontend/e2e/ai-analysis.spec.ts`. As demais etapas dependem de fixtures e credenciais E2E; devem falhar quando o contrato funcional, RBAC ou serviço assíncrono estiver indisponível.

Execução contra Compose:

```powershell
Set-Location backend-java
docker compose up -d --build
Set-Location ..
docker compose -f frontend/docker-compose.yml up -d --build
.\tests\run-tests.ps1 -Suite frontend -Mode integration
```
