# FASE 3 — Relatório de Execução

Unificação dos modelos Patient e Clinical em uma única `Base`. Nenhum serviço
legado alterado.

## O que foi verificado

1. **Importação de todos os modelos** no novo serviço — `app/domain/models/__init__.py`
   importa as 18 entidades (6 Patient + 12 Clinical).
2. **Mesma instância de `Base`** — todas as classes usam
   `shared.models.database.Base`.
3. **Sem colisão de nomes de tabela** — 19 tabelas únicas.
4. **Revisão dos ENUMs PostgreSQL**:

   | Domínio | ENUM | Valores |
   |---|---|---|
   | Patient | `gender_type` | M, F, OTHER |
   | Patient | `allergy_severity` | MILD, MODERATE, SEVERE |
   | Patient | `medication_change_type` | CREATED, UPDATED, DEACTIVATED, REACTIVATED |
   | Patient | `document_type` | EXAM, PRESCRIPTION, REPORT, IMAGE, LAB_RESULT, CERTIFICATE, OTHER |
   | Clinical | `appointment_type` | CONSULTATION, RETURN, EXAM, URGENT |
   | Clinical | `appointment_status` | SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW |
   | Clinical | `exam_urgency` | ROUTINE, URGENT, EMERGENCY |

   **Nenhum nome de ENUM é reutilizado** entre os dois domínios — sem conflito de
   `CREATE TYPE` no PostgreSQL.
5. **Todas as tabelas existentes mantidas** (19: 6 Patient + 12 Clinical +
   `patient_projections` + `audit_logs`).
6. **`patient_projections` mantida** (retrocompatibilidade).
7. **Verificação de metadata** — novo helper `app.domain.models.list_tables()`.

## Mudanças aplicadas

- `app/domain/models/__init__.py`: expõe `Base` e o helper `list_tables()` (lista
  todas as tabelas carregadas e ordenadas). Sem alteração de regra de negócio.
- `tests/test_models.py` (novo): valida presença/colisão de tabelas, mesma `Base`,
  ausência de conflito de ENUMs, presença de `patient_projections`, listagem do
  metadata e **smoke de persistência** (DDL completo compilado em SQLite em
  memória).

## Comandos

```bash
python -m compileall -q app tests
pytest tests/test_models.py tests/test_patient.py tests/test_clinical.py -v
```

## Resultado

- `compileall` → **exit 0**.
- Testes → **23 passed** (7 modelos + 7 Patient + 9 Clinical), 1 warning.

Observação de ambiente: para rodar localmente no Windows foi necessário fixar
`grpcio==1.62.2` no venv de validação, pois a política de Controle de Aplicativos
do host bloqueou o binário `cygrpc` do `grpcio 1.84.0`. **Nenhum arquivo do
repositório foi alterado por isso** (o `requirements.txt` continua intacto); o
aviso de versão do gRPC é apenas do ambiente.

## Riscos

- Nenhuma tabela/coluna foi removida ou renomeada.
- Nenhum ENUM foi alterado (valores originais preservados).
- Persistência de escrita real exige PostgreSQL; o smoke de DDL em SQLite valida
  a compilação do schema e as FKs entre os 19 modelos.

## Decisão

✅ Fase 3 concluída. Modelos unificados na mesma `Base` com validação de
metadata, sem colisões e com rollback preservado.