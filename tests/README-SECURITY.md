# Testes de segurança

Execute todos os cenários somente em ambiente descartável, com credenciais e dados de teste. Nunca use dados reais, tokens de produção ou bancos compartilhados.

## Execução centralizada

```powershell
.\tests\run-tests.ps1 -Suite security
```

Esse comando executa `npm audit --audit-level=high`, `pip-audit`, `bandit` e OWASP Dependency-Check nos serviços Java. Falhas altas/críticas devem bloquear o CI; exceções precisam registrar justificativa, versão afetada e prazo.

Para executar também os cenários API da coleção Newman:

```powershell
.\tests\run-tests.ps1 -Suite security -SecurityPassword '<senha-descartável>'
```

Ou executando o runner específico:

```powershell
.\tests\security\run-security.ps1 -Scope api -NewmanPassword '<senha-descartável>'
```

Ou diretamente:

```powershell
.\tests\newman\run-newman.ps1 -Password '<senha-descartável>'
```

## Cenários obrigatórios

- Login correto, incorreto, usuário inexistente/inativo e credenciais vazias.
- JWT ausente, malformado, expirado, com chave incorreta e algoritmo não permitido.
- Refresh válido, revogado, expirado e reutilizado.
- Matriz RBAC para `ADMIN`, `DOCTOR`, `ATTENDANT` e `PATIENT`, com distinção entre 401 e 403.
- Isolamento entre pacientes, prontuários, consultas, auditoria e relatórios de usuários diferentes.
- Mass assignment, SQL/NoSQL injection, XSS, path traversal, upload inválido, rate limiting e brute force.
- CORS, preflight, headers de segurança, HTTPS e ausência de segredos em URL/logs/erros.
- Mascaramento de PII e segredos, auditoria com usuário/role/IP/timestamp, correlação e idempotência de eventos.
- Retry, DLQ, replay, duplicação RabbitMQ e anonimização LGPD.

A coleção Newman já valida login inválido, acesso anônimo e contratos protegidos. Cenários 403, refresh revogado/expirado, isolamento entre usuários, uploads maliciosos e eventos duplicados exigem fixtures e tokens de roles específicos; não devem ser considerados aprovados apenas porque a coleção retornou 200.
