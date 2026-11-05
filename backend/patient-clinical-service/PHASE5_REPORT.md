# FASE 5 — Relatório de Execução

Remoção da dependência operacional da projeção legada no `patient-clinical-service`.

## Alterações

- Consultas clínicas usam `Patient` diretamente para filtro por nome, resolução por usuário e composição das respostas.
- Serviços, routers e `clinical_repository.py` não importam nem consultam a projeção.
- O `PatientProjectionRepository` e os handlers internos de `PatientCreatedEvent`/`PatientUpdatedEvent` não existem no serviço unificado.
- A publicação de `PatientCreatedEvent` e `PatientUpdatedEvent` permanece no `PatientService`, preservando consumidores externos.
- `UserDeactivatedEvent` localiza `Patient` diretamente, desativa o cadastro, cancela consultas futuras e grava auditoria em um único commit transacional.
- O símbolo de modelo foi renomeado para `LegacyPatientData`; o mapeamento mantém a tabela física `patient_projections` para rollback e compatibilidade. O mapeamento não é usado por nenhum fluxo.

## Validação

- `python -m compileall -q backend/patient-clinical-service/app backend/patient-clinical-service/tests` — **exit 0**.
- Busca por `PatientProjection` e `ProjectionRepository` no código executável de `backend/patient-clinical-service` (excluindo este relatório histórico) — **nenhuma ocorrência**.
- Busca por `patient_projections` — somente o mapeamento legado, os testes de preservação da tabela e o relatório histórico da Fase 3.
- `pytest` — não executado: o ambiente não possui o módulo/executável `pytest`.
- Import smoke — não executado: o ambiente não possui `sqlalchemy`.

## Contratos e rollback

Nenhuma URL, ID ou evento publicado foi alterado. `patient-service` e `clinical-service` permanecem intactos; a alteração está restrita ao serviço unificado já existente.

## Resultado

A Fase 5 está implementada no código e validada estaticamente. A validação de execução permanece pendente até instalar as dependências do `requirements.txt` no ambiente de testes.
