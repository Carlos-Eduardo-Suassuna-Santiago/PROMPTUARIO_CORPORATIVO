# Fase 10 — Gateway

## Resultado

Concluída a atualização reversível do Gateway para o serviço unificado.

- Rotas `/api/v1/patients/*`, `/appointments/*`, `/schedules/*` e `/records/*` encaminham para `patient-clinical-service` em `GATEWAY_ROUTING_MODE=unified`.
- Rollback preservado por `GATEWAY_ROUTING_MODE=legacy`, `LEGACY_PATIENT_SERVICE_URL` e `LEGACY_CLINICAL_SERVICE_URL`.
- Serviços antigos permaneceram ativos em paralelo; nenhuma URL pública, ID ou evento externo foi removido.
- Health agregado: `iam`, `patient`, `clinical`, `ai` e `reporting` retornaram `ok`.

## Validações

- `docker compose config --quiet`: passou.
- Build e startup do Gateway: passaram; container saudável na porta pública `8000`.
- Login pelo Gateway: passou.
- Criação e leitura de paciente: passaram.
- Criação de consulta, prontuário e prescrição: passaram.
- Upload e download pré-assinado de documento: passaram após corrigir o commit do `DocumentRepository`; chave persistida no bucket `patient-documents`.
- Download pré-assinado de PDF de prescrição: passou e apontou para `prescriptions`.
- Compilação Python dos arquivos alterados: passou.
- Testes locais com `pytest`: não executados porque o Python do host não possui o pacote `pytest`; os smoke tests de integração foram executados via Compose.
