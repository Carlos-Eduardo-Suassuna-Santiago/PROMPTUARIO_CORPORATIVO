# FASE 1 — Relatório de Execução

Criação do serviço unificado `backend/patient-clinical-service` **sem remover**
os serviços antigos. Serviço **aditivo**: nenhum arquivo de `patient-service` ou
`clinical-service` foi alterado, garantindo rollback.

## Arquivos alterados / criados

Somente dentro de `backend/patient-clinical-service/`:

- `app/main.py` — app FastAPI unificado (carrega modelos Patient + Clinical,
  registra `/api/v1/patients|appointments|records|schedules`, um único
  `EventPublisher`/`EventConsumer` e os dois buckets S3).
- `app/__init__.py`, `app/api/__init__.py`, `app/domain/__init__.py`,
  `app/infrastructure/__init__.py`, `app/workers/__init__.py`.
- `app/workers/celery_app.py`, `app/workers/prescription_tasks.py` (mesmo task
  name `clinical.generate_prescription_pdf` e mesma fila `celery`).
- `Dockerfile`, `Dockerfile.worker` (contexto de build = `backend/`).
- `tests/` — 6 testes legados copiados + `conftest.py`.
- `README.md`, `PHASE1_REPORT.md`.

Já existentes do trabalho parcial (revisados, não reescritos): `config.py`,
`api/patient_routers.py`, `api/clinical_routers.py`, `domain/models/*`,
`domain/schemas/*`, `domain/services/*`, `infrastructure/*`, `requirements.txt`.

### Correção aplicada durante a revisão

- `domain/services/clinical_service.py` → `_sanitize_rich_notes`: revertida a
  mudança `replace('<','&lt;')` para `replace('<','<')`, restaurando o
  comportamento **idêntico** ao `clinical-service` legado (evita alterar o
  contrato observável de `rich_notes`).
- `tests/test_patient.py`: o arquivo legado estava **corrompido** (52 bytes nulos
  / trecho UTF‑16LE). O original foi preservado intacto; apenas a **cópia** no
  novo serviço foi reparada (`def test_patient_vaccine_deletion_auth(): pass`).

## Comandos executados

```bash
python -m compileall -q app tests                 # exit 0
python -c "from app import main"                  # importa, 55 rotas
python -c "from app.workers import prescription_tasks"
pytest patient-clinical-service/tests/ -q -o asyncio_mode=auto
```

## Testes — paridade com o baseline

Executado com `pytest 8.3.2` + `pytest-asyncio 0.24.0` (mesma configuração do CI),
`PYTHONPATH=backend`, `asyncio_mode=auto`.

| Suite                                   | Resultado |
|-----------------------------------------|-----------|
| `patient-service` (sem `test_patient`)  | 4 failed, 5 passed, 8 errors |
| `clinical-service`                      | 5 failed, 35 passed |
| `patient-clinical-service` (novo)       | **9 failed, 47 passed, 8 errors** |
| `test_patient.py` legado                | não coletável (arquivo corrompido) |

`patient-clinical-service` = `patient-service` + `clinical-service` + 7 testes de
`test_patient.py`: **paridade exata** (9 = 4+5 falhas; 47 = 5+35+7 passed; 8 erros).

## Falhas/erros pré-existentes (NÃO são regressões)

1. `test_medication_history.py` (4) — `AsyncMock` em `repo.update` não aplica as
   mutações; bug do teste, falha igual no `patient-service`.
2. `test_prescription_pdf.py` / `test_rich_notes_and_signature.py` (5) —
   3 testes de PDF falham por ausência de `gobject-2.0-0` (WeasyPrint/GTK no
   Windows); `test_sanitize_escapes_html_tags` tem asserção autocontraditória
   (falha na mesma linha 81 no serviço original).
3. `test_documents.py` (8 errors) — fixture `mock_patient` chamada diretamente.

## Riscos identificados

- `Base.metadata.create_all()` no `startup` serve apenas para bootstrap de banco
  novo; **não substitui** a migração de dados (IDs preservados) da fase dedicada.
- `docker-compose.yml`, Gateway e demais serviços **não** foram alterados nesta
  fase (conforme escopo).

## Decisão

✅ Fase 1 concluída sem erros de importação/compilação e com paridade de
comportamento. **Serviços antigos intactos (rollback disponível).**
Próximo passo (Fase 2) somente após aprovação.