# FASE 6 — Relatório de Execução

## Estado

A migração foi interrompida antes da criação de `patient_clinical_db`, conforme a regra de não avançar após erro de validação da fase atual.

## Backups concluídos

- `patient_db`: `backend/.phase6-backups/2026-10-04/patient_db.sql` (8,101,114 bytes).
- `clinical_db`: `backend/.phase6-backups/2026-10-04/clinical_db.sql` (2,561,764 bytes).
- MinIO: cópia do volume em `backend/.phase6-backups/2026-10-04/minio-data/`, incluindo os roots `prescriptions`, `reports` e `backups`.
- Nenhuma tabela, banco ou volume de origem foi removido.

## Inspeção da origem

Patient:

- `patients`: 2701
- `allergies`: 674
- `vaccines`: 981
- `continuous_medications`: 593
- `medication_history`: 0
- `patient_documents`: 0

Clinical:

- `appointments`: 1914
- `medical_records`: 109
- `prescriptions`: 109
- `exam_requests`: 0
- `patient_projections`: 2701
- `medical_record_history`: 218
- `prescription_history`: 109
- `exam_request_history`: 0

## Bloqueio

As tabelas `medical_certificates` e `medical_certificate_history` não existem em `clinical_db`, embora estejam definidas no modelo/contrato do serviço unificado. A consulta de validação falhou com `relation "medical_certificates" does not exist`.

Por isso não foram executadas as etapas de criação do banco novo, cópia de dados, comparação de IDs, validação de chaves estrangeiras ou testes contra o destino. Nenhum `DROP TABLE` foi executado e `patient_projections` permanece intacta.

## Próximo passo necessário

Corrigir ou confirmar explicitamente o schema de origem para `medical_certificates` — incluindo sua tabela de histórico, se aplicável — e então repetir a inspeção antes de criar o banco destino.

## Follow-up — correção do schema de origem

O bloqueio foi corrigido após confirmação de que `medical_certificates` e `medical_certificate_history` estavam ausentes em `clinical_db`.

- Migração aplicada: `migrations/20261004_add_medical_certificates.sql`.
- Migração transacional, idempotente e não destrutiva; não usa `Base.metadata.create_all()`.
- Ambas as tabelas foram criadas com as colunas do modelo, FKs e índices esperados.
- Reexecução do script produziu apenas avisos `already exists`, sem alterações destrutivas.
- Contagens clínicas anteriores permaneceram: appointments 1914, medical_records 109, prescriptions 109, exam_requests 0, patient_projections 2701, medical_record_history 218, prescription_history 109 e exam_request_history 0.
- `medical_certificates`: 0; `medical_certificate_history`: 0.
- Verificação de órfãos: 0 em ambas as FKs.

O schema de origem está agora alinhado para a próxima inspeção da migração, mas `patient_clinical_db` ainda não foi criado.

## Resultado da migração do destino — 2026-10-05

- `patient_clinical_db` criado na instância PostgreSQL do Clinical; `patient_db` e `clinical_db` permaneceram intactos.
- Backups usados sem alteração; os dumps UTF-16 foram apenas convertidos para cópias temporárias UTF-8 para restauração.
- Dados Patient e Clinical restaurados preservando IDs; `audit_logs` foi mesclado sem colisões reais (10.924 + 133 = 11.057). A tabela temporária `audit_logs_patient_staging` permanece durante a transição; nenhum DROP foi executado.
- Schema `medical_certificates` e `medical_certificate_history` aplicado no destino, ambos com 0 registros.
- Comparação de IDs: diff 0 em patients, allergies, vaccines, continuous_medications, medication_history, patient_documents, appointments, medical_records, prescriptions, exam_requests, históricos e patient_projections.
- Patient IDs clínicos correspondem por `patients.user_id` (1.914 appointments e 109 medical_records/prescriptions); nenhum `patient_id` ficou sem paciente por `patients.user_id`; por `patients.id`, os registros clínicos legados não coincidem. Essa compatibilidade preserva os IDs legados e deve ser respeitada pela API no cutover.
- FKs internas e órfãos: 0 órfãos; `patient_projections` foi mantida.
- MinIO: `prescriptions` validado; `patient-documents` criado vazio, pois não havia documentos na origem. Os buckets permanecem separados.

## Resultado

A migração não destrutiva do banco destino foi concluída e validada. Os bancos legados e a tabela física `patient_projections` permanecem disponíveis para rollback/transição.