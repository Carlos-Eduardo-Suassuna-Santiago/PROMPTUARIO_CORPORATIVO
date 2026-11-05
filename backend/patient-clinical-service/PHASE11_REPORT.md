# Fase 11 - Testes completos

## Resultados

- Patient original: 24 passed.
- Clinical original: 40 passed.
- Patient-Clinical: 83 passed.
- Testes adicionais de invariantes: 7 passed.
- Gateway: 6 passed.
- IAM FastAPI: 1 passed.
- IAM unitarios independentes: 5 passed.
- Smoke tests: Gateway, servico unificado e servicos legados saudaveis.
- Banco: `orphan_by_id_or_user_id=0`; `patient_projections=2701` preservada durante a transicao.
- RabbitMQ: ping passou.
- Redis DB 3: PONG.
- Worker: ativo.

A matriz adicional cobre banco compartilhado, propagacao de `patient_id`, prontuario com consulta inexistente, isolamento/autorizacao, cancelamento, desativacao idempotente, publicacao de evento e rollback nao destrutivo.

## Correcoes relacionadas aos testes

- Recuperados testes Patient e IAM que continham NULs e nao podiam ser coletados.
- Corrigidos fixtures/assertions inconsistentes de documentos, medicamentos, PDF e Gateway.
- Sanitizacao de rich notes passou a escapar HTML.
- `PatientDocument.is_active` passou a ser explicitamente `True` na criacao.
- Bibliotecas nativas do WeasyPrint foram adicionadas as imagens de API; `pydyf==0.10.0` foi fixado no Clinical.

## Bloqueio restante

A execucao integral de `iam-service/tests/test_auth.py` continua bloqueando no ciclo de vida do teste que inicializa infraestrutura externa; o subconjunto independente e `test_auth_fastapi.py` passaram. Portanto, a Fase 11 nao deve ser marcada como totalmente encerrada ate esse teste de ciclo de vida ser isolado ou executado com RabbitMQ/Redis de teste disponiveis.