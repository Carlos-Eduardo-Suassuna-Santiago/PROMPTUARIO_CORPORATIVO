# BACKUP_BASELINE.md — Baseline do Serviço de Backup (Fase B0)

> **Escopo:** correção crítica de contingência do runner Python
> (`backend/backup/backup_runner.py`) e da orquestração
> (`backend/docker-compose.yml`) após a **Fase 12** de unificação do PROMPTUÁRIO.
> Documento de referência operacional (SRE / DBA) para *disaster recovery*.

---

## 1. Contexto da correção

A Fase 12 fundiu `patient-service` e `clinical-service` em
`patient-clinical-service`, com banco único **`patient_clinical_db`** hospedado no
container **`db-patient-clinical`**. O runner anterior ainda tentava dumpar os
hosts órfãos inexistentes `db-patient` e `db-clinical`, o que:

- gerava falhas `connection refused` / `could not translate host name`;
- deixava o **banco clínico unificado sem proteção** contra desastres.

| Antes (quebrado) | Depois (corrigido) |
| :--- | :--- |
| `db-patient` → `patient_db` | **removido** |
| `db-clinical` → `clinical_db` | **removido** |
| — | **`db-patient-clinical` → `patient_clinical_db`** |
| `db-iam` → `iam_db` | mantido |
| `db-reporting` → `reporting_db` | mantido |
| `db-ai` → `ai_db` (MongoDB) | mantido |

### Topologia alvo

| Alvo | Tipo | Container | Banco | Usuário | Env de override de host |
| :--- | :--- | :--- | :--- | :--- | :--- |
| IAM | PostgreSQL | `db-iam` | `iam_db` | `iam` | `IAM_DB_HOST` |
| Patient-Clinical | PostgreSQL | `db-patient-clinical` | `patient_clinical_db` | `clinical` | `PATIENT_CLINICAL_DB_HOST` |
| Reporting | PostgreSQL | `db-reporting` | `reporting_db` | `reporting` | `REPORTING_DB_HOST` |
| AI | MongoDB | `db-ai` | `ai_db` | `ai` | `MONGO_HOST` |

**Overrides de credenciais** (`backup_runner._apply_postgres_env_overrides`),
em ordem decrescente de prioridade:

1. `<PREFIXO>_DB_USER` / `<PREFIXO>_DB_PASSWORD` (ex.: `PATIENT_CLINICAL_DB_USER`);
2. `POSTGRES_USER` / `POSTGRES_PASSWORD` (genérico do ambiente);
3. credencial default declarada em `POSTGRES_DATABASES`.

O `<PREFIXO>` é derivado do nome do banco (`patient_clinical_db` → `PATIENT_CLINICAL`).

---

## 2. Comando de execução de contingência

Execução **sob demanda**, imediata, para garantir um dump válido fora da janela
agendada (equivalente a `make backup-once`):

```bash
docker compose run --rm backup-service python backup_runner.py --once
```

Variantes úteis:

```bash
# Restaurar um dump PostgreSQL sob demanda (DR controlado)
docker compose run --rm backup-service python backup_runner.py \
    --restore --kind postgres --file /var/backups/postgresql/patient_clinical_db/<AAAA>/<MM>/<DD>/postgres_patient_clinical_db_<ts>.sql.gz \
    --target patient_clinical_db

# Modo agendado (produção) — sobe o serviço em background
docker compose up -d backup-service
```

> O `--once` executa **todos** os alvos (`iam_db`, `patient_clinical_db`,
> `reporting_db`, `ai_db`), grava o manifesto local e faz upload para o bucket
> MinIO `backups`.

---

## 3. Estrutura do arquivo `/var/backups/status.json`

Gerado ao final de cada ciclo por `write_status_report()` e persistido no volume
`backup_data` montado em `/var/backups` do container `backup-service`.

| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `generated_at` | string (ISO-8601 UTC) | Timestamp de geração do manifesto |
| `mode` | string | `manual` (`--once`) ou `scheduled` (agendado) |
| `status` | string | **Status final**: `success` \| `partial` |
| `elapsed_seconds` | number | **Tempo de execução** total do ciclo (segundos) |
| `database_count` | integer | Quantidade de alvos processados |
| `backups[]` | array | Registro **por banco** (ver abaixo) |

### Campos de cada item de `backups[]`

| Campo | Descrição |
| :--- | :--- |
| `database` | Nome do banco (`iam_db`, `patient_clinical_db`, `reporting_db`, `ai_db`) |
| `kind` | `postgres` ou `mongo` |
| `status` | `uploaded` (local + MinIO) \| `local_only` (falha no upload, dump local ok) \| `failed` |
| `path` | Caminho local do dump (`/var/backups/postgresql/<db>/<Y>/<m>/<d>/...`) |
| `s3_key` | Chave no bucket MinIO `backups` (ou `null` em `local_only`) |
| `size_mb` | **Tamanho do dump** em megabytes (arredondado a 2 casas) |
| `mode` | Modo da execução |
| `error` | Presente apenas quando `status == "failed"` |

### Regra do status final

```text
status = "success"  se TODOS os itens tiverem status ∈ { "uploaded", "local_only" }
status = "partial"  caso contrário (ao menos um "failed")
```

### Exemplo de manifesto

```json
{
  "generated_at": "2026-11-11T18:05:42.123456+00:00",
  "mode": "manual",
  "status": "partial",
  "elapsed_seconds": 37.42,
  "database_count": 4,
  "backups": [
    {
      "database": "iam_db", "kind": "postgres", "status": "uploaded",
      "path": "/var/backups/postgresql/iam_db/2026/11/11/postgres_iam_db_20261111_180505.sql.gz",
      "s3_key": "backups/postgresql/iam_db/2026/11/11/postgres_iam_db_20261111_180505.sql.gz",
      "size_mb": 2.51, "mode": "manual"
    },
    {
      "database": "patient_clinical_db", "kind": "postgres", "status": "uploaded",
      "path": "/var/backups/postgresql/patient_clinical_db/2026/11/11/postgres_patient_clinical_db_20261111_180512.sql.gz",
      "s3_key": "backups/postgresql/patient_clinical_db/2026/11/11/postgres_patient_clinical_db_20261111_180512.sql.gz",
      "size_mb": 8.31, "mode": "manual"
    },
    { "database": "reporting_db", "kind": "postgres", "status": "local_only",
      "path": "/var/backups/postgresql/reporting_db/2026/11/11/postgres_reporting_db_20261111_180530.sql.gz",
      "s3_key": null, "size_mb": 1.02, "mode": "manual" },
    { "database": "ai_db", "kind": "mongo", "status": "uploaded",
      "path": "/var/backups/mongodb/ai_db/2026/11/11/mongo_ai_db_20261111_180535.archive.gz",
      "s3_key": "backups/mongodb/ai_db/2026/11/11/mongo_ai_db_20261111_180535.archive.gz",
      "size_mb": 12.70, "mode": "manual" }
  ]
}
```

---

## 4. Matriz de `depends_on` e redes (docker-compose.yml)

O serviço `backup-service` só inicia após todos os alvos estarem saudáveis,
garantindo que nenhum dump seja disparado contra um banco ainda subindo.

| Serviço dependente | Condição | Rede | Healthcheck |
| :--- | :--- | :--- | :--- |
| `db-iam` | `service_healthy` | `backend` | `pg_isready -U iam -d iam_db` |
| `db-patient-clinical` | `service_healthy` | `backend` | `pg_isready -U clinical -d patient_clinical_db` |
| `db-reporting` | `service_healthy` | `backend` | `pg_isready -U reporting -d reporting_db` |
| `db-ai` | `service_healthy` | `backend` | `mongosh --eval db.adminCommand('ping')` |
| `minio` | `service_healthy` | `backend` | `curl -f http://localhost:9000/minio/health/live` |

Todos os alvos e o `backup-service` compartilham a **mesma rede bridge
`backend`**, permitindo a resolução DNS pelos nomes dos containers
(`db-iam`, `db-patient-clinical`, `db-reporting`, `db-ai`, `minio`).

Persistência: o volume nomeado `backup_data` é montado em `/var/backups`, o que
preserva `status.json`, `backup.log` e os dumps locais entre recriações do
container.

Verificação rápida:

```bash
docker compose config | grep -A6 'backup-service:'
docker compose ps
```

---

## 5. Coleta do baseline (tempo e tamanho)

Procedimento para registrar tempo e tamanho por banco:

```bash
# 1. Garantir a infraestrutura saudável
docker compose up -d db-iam db-patient-clinical db-reporting db-ai minio

# 2. Executar um ciclo único de contingência
docker compose run --rm backup-service python backup_runner.py --once

# 3. Inspecionar o tempo/tamanho consolidados
docker compose exec backup-service cat /var/backups/status.json
# (ou) make status
```

Registrar em planilha de baseline: `elapsed_seconds` e o `size_mb` de cada item
de `backups[]` — esses valores são a referência para detectar anomalias
(ex.: dump muito menor que o histórico costuma indicar truncamento/corrupção).

---

## 6. Critérios de aceite (Fase B0)

- [x] `POSTGRES_DATABASES` sem `db-patient`/`db-clinical` e com `patient_clinical_db` (`db-patient-clinical`).
- [x] Execução de `docker compose run --rm backup-service python backup_runner.py --once` concluída com sucesso.
- [x] `/var/backups/status.json` registra dumps válidos para `iam_db`, `patient_clinical_db`, `reporting_db` e `ai_db`.
- [x] Dump de `patient_clinical_db` gravado no volume local e enviado ao MinIO (bucket `backups`).
- [x] `depends_on` com os 5 alvos (`service_healthy`) e todos na rede `backend`.
- [x] Falha isolada em um banco **não derruba** a execução dos demais (status `partial`).

---

## 7. Testes automatizados

```bash
# Roteiro funcional do runner (autossuficiente, sem Docker/MinIO)
python backend/backup/test_backup_runner.py

# Roteiro de verificação estática do stack de backup (Dockerfile, compose, endpoints)
python backend/scripts/test_backup_service.py
```

O `test_backup_runner.py` cobre: topologia unificada, ausência de hosts órfãos,
overrides por `POSTGRES_USER`/`POSTGRES_PASSWORD` e por prefixo de banco,
estrutura do `status.json` e o isolamento de falhas no orquestrador.

---

## 8. Rollback

Em caso de regressão, restaurar o estado anterior do código e reconstruir:

```bash
git checkout backend/backup/backup_runner.py backend/docker-compose.yml
docker compose build backup-service
```

> Risco B0 mapeado: erro de sintaxe na edição do script legado. O rollback via
> `git checkout` restaura imediatamente o estado anterior do arquivo.
