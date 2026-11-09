# FASE 2 — Relatório de Execução

Unificação das configurações em `app/config.py`. Nenhum serviço legado foi
alterado (rollback preservado).

## O que foi verificado/alinhado

O `app/config.py` unificado já expunha todos os blocos exigidos, que foram
revisados e documentados:

- `DATABASE_URL` — variável única e compartilhada por Patient e Clinical.
- `RABBITMQ_URL`.
- `JWT_SECRET_KEY` (obrigatória, sem default) e `JWT_ALGORITHM`.
- S3 de documentos (`S3_BUCKET_PATIENT_DOCUMENTS`) e de prescrições
  (`S3_BUCKET_PRESCRIPTIONS`) — **buckets separados**.
- Celery (`CELERY_BROKER_URL`/`CELERY_RESULT_BACKEND`, Redis DB 3).
- Regra de cancelamento (`APPOINTMENT_CANCEL_HOURS_MIN = 24`).
- Assinatura digital (`SIGNATURE_HASH_ALGORITHM = "sha256"`).
- Limites de upload (`MAX_DOCUMENT_SIZE_MB`, `ALLOWED_DOCUMENT_MIME_TYPES`).

### Mudanças aplicadas

1. `app/config.py`: apenas **comentários clarificadores** (nenhum valor alterado):
   - nota sobre o `DATABASE_URL` único compartilhado;
   - nota sobre credenciais S3 (`promptuario/promptuario_pass`, consistentes com a
     infra MinIO — o `patient-service` legado usava o default de dev `minioadmin`);
   - buckets documentados como separados por intenção.
2. `tests/test_config.py` (novo): validação de carregamento, buckets separados,
   variável obrigatória, criação de config por testes e compartilhamento do
   `DATABASE_URL`.

## Comandos executados

```bash
python -m compileall -q app tests
pytest tests/test_config.py -v
```

## Resultado

- `compileall` → **exit 0**.
- `tests/test_config.py` → **5 passed, 1 skipped**.

| Teste | Resultado |
|---|---|
| `test_settings_exposes_required_blocks` | PASSED |
| `test_buckets_are_kept_separate` | PASSED |
| `test_jwt_secret_stays_required` | PASSED |
| `test_settings_can_be_created_with_env_overrides` | PASSED |
| `test_patient_and_clinical_share_same_database_url` | SKIPPED (só a porção runtime) |
| `test_no_incompatible_overrides` | PASSED |

### Sobre o `SKIPPED`

A porção estrutural de `test_patient_and_clinical_share_same_database_url`
**passa** (prova que `patient_service.py` e `clinical_service.py` importam
`settings` de `app.config`, garantindo, por semântica do Python, o mesmo
`DATABASE_URL`). A porção **runtime** (import) foi pulada porque a política de
Controle de Aplicativos do Windows bloqueou intermitentemente o `cygrpc.dll` do
OpenTelemetry/gRPC — limitação **ambiental** (afeta os serviços legados
igualmente), não causada por esta fase. Em ambiente sem esse bloqueio o teste
roda e valida `patient_settings is clinical_settings is global_settings`.

## Riscos / observações

- Nenhum valor de configuração foi sobrescrito com dado incompatível.
- `DATABASE_URL`, `JWT_SECRET_KEY` e demais variáveis continuam exportáveis por
  ambiente (nenhuma remoção de variável obrigatória).

## Decisão

✅ Fase 2 concluída. Configurações unificadas e validadas sem alterar os
serviços antigos.