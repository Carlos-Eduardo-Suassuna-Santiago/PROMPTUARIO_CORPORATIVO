"""
PROMPTUÁRIO — Roteiro de teste do Backup Runner (Fase B0 / topologia unificada)

Valida a correção crítica aplicada a ``backend/backup/backup_runner.py``:
  * a lista ``POSTGRES_DATABASES`` reflete a topologia unificada pós-Fase 12
    (``db-patient-clinical`` / ``patient_clinical_db``), sem os hosts órfãos;
  * credenciais default podem ser sobrescritas por variáveis de ambiente;
  * o ``/var/backups/status.json`` registra tempo de execução, tamanho de cada
    dump e o status final (``success`` / ``partial``);
  * uma falha isolada em um banco NÃO derruba a execução dos demais.

O roteiro é autossuficiente: injeta stubs de ``boto3``/``botocore``/``schedule``
quando eles não estão instalados, permitindo execução em qualquer ambiente
(sem Docker, sem MinIO e sem dependências de produção).

Uso:
    python backend/backup/test_backup_runner.py
    # ou, via pytest:
    python -m pytest backend/backup/test_backup_runner.py -v
"""
from __future__ import annotations

import importlib
import json
import os
import sys
import tempfile
import types
import unittest
from pathlib import Path
from unittest import mock

BACKUP_DIR = Path(__file__).resolve().parent
if str(BACKUP_DIR) not in sys.path:
    sys.path.insert(0, str(BACKUP_DIR))

# Chaves de ambiente controladas pelos testes — neutralizadas antes do import
# para garantir determinismo independentemente do ambiente do operador.
MANAGED_ENV_KEYS = (
    "POSTGRES_USER",
    "POSTGRES_PASSWORD",
    "IAM_DB_USER",
    "IAM_DB_PASSWORD",
    "IAM_DB_HOST",
    "PATIENT_CLINICAL_DB_USER",
    "PATIENT_CLINICAL_DB_PASSWORD",
    "PATIENT_CLINICAL_DB_HOST",
    "REPORTING_DB_USER",
    "REPORTING_DB_PASSWORD",
    "REPORTING_DB_HOST",
)


def _install_dependency_stubs() -> None:
    """Injeta stubs de boto3/botocore/schedule quando ausentes no ambiente."""
    try:
        import boto3  # noqa: F401
    except ModuleNotFoundError:
        boto3_stub = types.ModuleType("boto3")
        boto3_stub.client = lambda *args, **kwargs: mock.MagicMock()
        sys.modules["boto3"] = boto3_stub

    try:
        import botocore.exceptions  # noqa: F401
    except ModuleNotFoundError:
        botocore_stub = types.ModuleType("botocore")
        exceptions_stub = types.ModuleType("botocore.exceptions")

        class ClientError(Exception):
            pass

        class EndpointConnectionError(Exception):
            pass

        exceptions_stub.ClientError = ClientError
        exceptions_stub.EndpointConnectionError = EndpointConnectionError
        botocore_stub.exceptions = exceptions_stub
        sys.modules["botocore"] = botocore_stub
        sys.modules["botocore.exceptions"] = exceptions_stub

    try:
        import schedule  # noqa: F401
    except ModuleNotFoundError:
        schedule_stub = types.ModuleType("schedule")

        class _EveryJob:
            def hours(self):
                return self

            def seconds(self):
                return self

            def do(self, *args, **kwargs):
                return None

        schedule_stub.every = lambda *args, **kwargs: _EveryJob()
        schedule_stub.run_pending = lambda: None
        sys.modules["schedule"] = schedule_stub


# Diretório temporário e isolado para artefatos de teste (status.json/backup.log).
_TMP_DIR = tempfile.TemporaryDirectory(prefix="backup-runner-test-")
os.environ["BACKUP_ROOT_DIR"] = _TMP_DIR.name
for _key in MANAGED_ENV_KEYS:
    os.environ.pop(_key, None)

_install_dependency_stubs()

import backup_runner as br  # noqa: E402  (import após bootstrap de ambiente/stubs)


class PostgresTopologyTest(unittest.TestCase):
    """A topologia unificada deve substituir os hosts órfãos legados."""

    def test_databases_refletem_topologia_unificada(self):
        databases = {cfg["dbname"]: cfg for cfg in br.POSTGRES_DATABASES}

        self.assertEqual(
            set(databases),
            {"iam_db", "patient_clinical_db", "reporting_db"},
            "Devem existir exatamente iam_db, patient_clinical_db e reporting_db",
        )
        self.assertEqual(len(br.POSTGRES_DATABASES), 3)

        self.assertEqual(databases["iam_db"]["host"], "db-iam")
        self.assertEqual(databases["patient_clinical_db"]["host"], "db-patient-clinical")
        self.assertEqual(databases["reporting_db"]["host"], "db-reporting")

        unified = databases["patient_clinical_db"]
        self.assertEqual(unified["user"], "clinical")
        self.assertEqual(unified["password"], "clinical_pass")

    def test_hosts_orfaos_ausentes(self):
        hosts = [cfg["host"] for cfg in br.POSTGRES_DATABASES]
        self.assertNotIn("db-patient", hosts)
        self.assertNotIn("db-clinical", hosts)
        self.assertIn("db-patient-clinical", hosts)


class EnvOverrideTest(unittest.TestCase):
    """POSTGRES_USER/POSTGRES_PASSWORD e variáveis por banco devem prevalecer."""

    def tearDown(self):
        for key in MANAGED_ENV_KEYS:
            os.environ.pop(key, None)
        importlib.reload(br)

    def test_credenciais_default_quando_sem_override(self):
        for cfg in br.POSTGRES_DATABASES:
            self.assertNotEqual(cfg["user"], "")
            self.assertNotEqual(cfg["password"], "")

    def test_postgres_user_e_password_globais_sobrescrevem_defaults(self):
        os.environ["POSTGRES_USER"] = "svc_backup"
        os.environ["POSTGRES_PASSWORD"] = "s3cr3t-value"
        importlib.reload(br)

        for cfg in br.POSTGRES_DATABASES:
            self.assertEqual(cfg["user"], "svc_backup")
            self.assertEqual(cfg["password"], "s3cr3t-value")

    def test_variavel_especifica_do_banco_tem_prioridade(self):
        os.environ["POSTGRES_USER"] = "svc_backup"
        os.environ["PATIENT_CLINICAL_DB_USER"] = "clinical_especifico"
        importlib.reload(br)

        databases = {cfg["dbname"]: cfg for cfg in br.POSTGRES_DATABASES}
        self.assertEqual(databases["patient_clinical_db"]["user"], "clinical_especifico")
        self.assertEqual(databases["iam_db"]["user"], "svc_backup")

    def test_host_pode_ser_sobrescrito(self):
        os.environ["PATIENT_CLINICAL_DB_HOST"] = "db-patient-clinical-replica"
        importlib.reload(br)

        databases = {cfg["dbname"]: cfg for cfg in br.POSTGRES_DATABASES}
        self.assertEqual(databases["patient_clinical_db"]["host"], "db-patient-clinical-replica")


class StatusReportTest(unittest.TestCase):
    """Estrutura do /var/backups/status.json exigida na baseline."""

    def test_status_json_registra_tempo_tamanhos_e_status_final(self):
        results = [
            {"database": "iam_db", "kind": "postgres", "status": "uploaded", "size_mb": 2.5},
            {"database": "patient_clinical_db", "kind": "postgres", "status": "local_only", "size_mb": 8.31},
            {"database": "reporting_db", "kind": "postgres", "status": "uploaded", "size_mb": 1.02},
            {"database": "ai_db", "kind": "mongo", "status": "uploaded", "size_mb": 12.7},
        ]

        br.write_status_report(results, "partial", "manual", 42.5)

        payload = json.loads(br.STATUS_FILE.read_text(encoding="utf-8"))
        self.assertEqual(payload["status"], "partial")
        self.assertEqual(payload["mode"], "manual")
        self.assertEqual(payload["elapsed_seconds"], 42.5)
        self.assertEqual(payload["database_count"], 4)
        self.assertIn("generated_at", payload)

        for entry in payload["backups"]:
            self.assertIn("database", entry)
            self.assertIn("status", entry)
            self.assertIn("size_mb", entry)


class RunAllBackupsIsolationTest(unittest.TestCase):
    """Falha isolada em um banco não pode derrubar os demais."""

    def setUp(self):
        # Neutraliza dependências externas (MinIO/rede) do orquestrador.
        for target, return_value in (("ensure_bucket", None), ("cleanup_old_backups", 0)):
            patcher = mock.patch.object(br, target, return_value=return_value)
            patcher.start()
            self.addCleanup(patcher.stop)

    @staticmethod
    def _ok(database: str, kind: str, mode: str) -> dict:
        return {"database": database, "kind": kind, "status": "uploaded", "size_mb": 1.0, "mode": mode}

    def test_falha_isolada_gera_status_partial_sem_abortar(self):
        def fake_postgres(cfg, mode):
            if cfg["dbname"] == "patient_clinical_db":
                raise RuntimeError("simulated outage no banco unificado")
            return {
                "database": cfg["dbname"],
                "kind": "postgres",
                "status": "local_only",
                "size_mb": 1.0,
                "mode": mode,
            }

        def fake_mongo(cfg, mode):
            return self._ok(cfg["dbname"], "mongo", mode)

        with mock.patch.object(br, "backup_postgres", side_effect=fake_postgres), \
                mock.patch.object(br, "backup_mongo", side_effect=fake_mongo):
            results = br.run_all_backups(mode="manual")

        by_db = {entry["database"]: entry for entry in results}

        # Todos os alvos foram processados, apesar da falha isolada.
        self.assertEqual(set(by_db), {"iam_db", "patient_clinical_db", "reporting_db", "ai_db"})
        self.assertEqual(by_db["patient_clinical_db"]["status"], "failed")
        self.assertIn("simulated outage", by_db["patient_clinical_db"]["error"])
        self.assertEqual(by_db["iam_db"]["status"], "local_only")
        self.assertEqual(by_db["reporting_db"]["status"], "local_only")
        self.assertEqual(by_db["ai_db"]["status"], "uploaded")

        payload = json.loads(br.STATUS_FILE.read_text(encoding="utf-8"))
        self.assertEqual(payload["status"], "partial")
        self.assertEqual(payload["database_count"], 4)

    def test_todos_os_bancos_ok_gera_status_success(self):
        def fake_postgres(cfg, mode):
            return self._ok(cfg["dbname"], "postgres", mode)

        def fake_mongo(cfg, mode):
            return self._ok(cfg["dbname"], "mongo", mode)

        with mock.patch.object(br, "backup_postgres", side_effect=fake_postgres), \
                mock.patch.object(br, "backup_mongo", side_effect=fake_mongo):
            results = br.run_all_backups(mode="manual")

        self.assertTrue(all(entry["status"] == "uploaded" for entry in results))
        payload = json.loads(br.STATUS_FILE.read_text(encoding="utf-8"))
        self.assertEqual(payload["status"], "success")


if __name__ == "__main__":
    unittest.main(verbosity=2)
