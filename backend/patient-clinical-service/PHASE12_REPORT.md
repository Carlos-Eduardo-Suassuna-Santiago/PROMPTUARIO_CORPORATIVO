# Relatório da Fase 12 — Corte e Remoção dos Serviços Antigos

**Data:** 15/10/2026  
**Status:** ✅ Concluída com Sucesso

---

## 1. Objetivos da Fase 12

1. **Backup final dos bancos e serviços antigos:**
   - Realizado backup via `pg_dump` dos bancos `patient_db`, `clinical_db` e `patient_clinical_db`.
   - Arquivos salvaguardados em `backend/.phase12-backups/`:
     - `phase12_patient_db.sql` (4.04 MB)
     - `phase12_clinical_db.sql` (1.29 MB)
     - `phase12_patient_clinical_db.sql` (8.26 MB)
     - `legacy-services-backup.zip` (código-fonte dos serviços legados)

2. **Ativação e limpeza definitiva do Gateway:**
   - Rotas `/api/v1/patients/*`, `/api/v1/appointments/*`, `/api/v1/schedules/*` e `/api/v1/records/*` apontando diretamente para `PATIENT_CLINICAL_SERVICE_URL`.
   - Remoção de variáveis legadas de rollback dinâmico (`LEGACY_PATIENT_SERVICE_URL`, `LEGACY_CLINICAL_SERVICE_URL`, `GATEWAY_ROUTING_MODE`).
   - Endpoint `/healthz/services` validando `patient_clinical` e mantendo aliases retrocompatíveis para `patient` e `clinical`.

3. **Monitoramento e estabilidade:**
   - Logs de containers verificados: `patient-clinical-service`, `patient-clinical-worker` e `gateway` em execução saudável, sem exceções.
   - Celery worker executando tarefas assíncronas `clinical.generate_prescription_pdf` com status OK e persistência no MinIO `prescriptions`.
   - Containers legados de banco de dados (`backend-db-patient-1` e `backend-db-clinical-1`) desligados com sucesso sem qualquer impacto nos serviços ativos.

4. **Remoção de componentes obsoletos:**
   - `backend/patient-service/` removido (cópia de segurança em `.phase12-backups/`).
   - `backend/clinical-service/` removido (cópia de segurança em `.phase12-backups/`).
   - `patient_db` e `clinical_db` removidos do `docker-compose.yml` (serviços `db-patient`, `db-clinical` e volumes `patient_data`, `clinical_data`).
   - Modelo `LegacyPatientData` / tabela `patient_projections` removido do código SQLAlchemy e do metadata unificado.
   - Configurações antigas do Gateway e do `docker-compose.yml` saneadas.

5. **Atualização de documentação, CI/CD e automações:**
   - `.github/workflows/backend-ci.yml` atualizado para testar e validar lint de `patient-clinical-service`.
   - `.github/workflows/docker-build.yml` atualizado com o serviço unificado.
   - `backend/Makefile` e `backend/README.md` atualizados.
   - `DOCUMENTATION/00_VISAO_SISTEMA_UNIFICADO.md` consolidado como estado final da arquitetura.

---

## 2. Critérios Finais de Aceite

| Critério | Status | Evidência |
|---|---|---|
| Existir apenas um serviço para Patient e Clinical | ✅ Aprovado | `backend/patient-clinical-service` é o único microsserviço ativo. |
| Todas as rotas antigas continuarem funcionando | ✅ Aprovado | Gateway roteia `/patients`, `/appointments`, `/schedules` e `/records`. |
| Não haver chamada HTTP entre Patient e Clinical | ✅ Aprovado | Serviços integrados no mesmo processo e no mesmo banco (`patient_clinical_db`). |
| Não haver uso de `patient_projections` | ✅ Aprovado | Nenhuma classe nem consulta do código unificado referencia projeções. |
| Dados migrados sem alteração de IDs | ✅ Aprovado | UUIDs de pacientes, consultas, prontuários e prescrições 100% preservados. |
| AI e Reporting continuarem recebendo eventos | ✅ Aprovado | Eventos RabbitMQ (`PatientCreated`, `AppointmentCreated`, etc.) mantidos e ativos. |
| Worker de prescrição continuar funcional | ✅ Aprovado | Celery `patient-clinical-worker` gerando PDFs e salvando em `s3://prescriptions/`. |
| Documentos e PDFs continuarem acessíveis | ✅ Aprovado | Buckets MinIO `patient-documents` e `prescriptions` ativos e acessíveis. |
| Todos os testes relevantes passarem | ✅ Aprovado | 92 testes aprovados (3 pulados no host Windows por dependência de biblioteca C nativa do WeasyPrint). |
| Gateway apontar para o novo serviço | ✅ Aprovado | Configuração `PATIENT_CLINICAL_SERVICE_URL` apontando para `:8002` / `patient-clinical-service:8000`. |
| Procedimento de rollback documentado | ✅ Aprovado | Seção 4 deste documento detalha o procedimento passo a passo. |
| Documentação refletir a nova arquitetura | ✅ Aprovado | Diagramas, READMEs e documentos mestres sincronizados. |

---

## 3. Validações e Testes Executados

- **Compilação Python:**
  `python -m compileall backend/` → 0 erros de sintaxe ou importação.
- **Testes Unitários:**
  `pytest -o asyncio_mode=auto backend/gateway/tests backend/patient-clinical-service/tests`  
  Resultado: **92 passed, 3 skipped, 0 failed**.
- **IAM Tests:**
  `pytest backend/iam-service/tests`  
  Resultado: **10 passed, 0 failed**.
- **AI Schemas Tests:**
  `pytest backend/ai-service/tests/test_schemas.py`  
  Resultado: **11 passed, 0 failed**.
- **Health Aggregation Check:**
  `curl http://localhost:8000/healthz/services`  
  Resultado: `{"status":"ok","services":{"iam":"ok","patient_clinical":"ok","ai":"ok","reporting":"ok","patient":"ok","clinical":"ok"}}`.

---

## 4. Procedimento de Rollback

Caso seja necessário reverter a migração:

1. **Restaurar os Bancos de Dados:**
   - Recriar os bancos antigos através dos dumps armazenados em `backend/.phase12-backups/`:
     ```bash
     psql -U patient -d patient_db < backend/.phase12-backups/phase12_patient_db.sql
     psql -U clinical -d clinical_db < backend/.phase12-backups/phase12_clinical_db.sql
     ```
2. **Restaurar o Código-Fonte:**
   - Descompactar `backend/.phase12-backups/legacy-services-backup.zip` no diretório `backend/` ou recuperar a tag Git anterior à Fase 12.
3. **Restaurar Docker Compose:**
   - Reverter as alterações de `backend/docker-compose.yml` via Git (`git checkout <commit_anterior> backend/docker-compose.yml`).
   - Reiniciar os containers com `docker compose up -d`.
