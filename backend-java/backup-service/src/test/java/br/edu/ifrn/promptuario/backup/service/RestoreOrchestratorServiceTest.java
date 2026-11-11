package br.edu.ifrn.promptuario.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.metrics.BackupMetricsService;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.restorer.MongoDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.PostgresDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Testes do orquestrador de restauração (mapeamento de alvo, staging, auditoria e métricas). */
class RestoreOrchestratorServiceTest {

    @TempDir
    Path rootDir;

    private S3BackupStorageService storage;
    private PostgresDatabaseRestorer postgres;
    private MongoDatabaseRestorer mongo;
    private RestoreAuditService auditService;
    private BackupMetricsService metricsService;
    private SimpleMeterRegistry meterRegistry;
    private RestoreOrchestratorService orchestrator;

    @BeforeEach
    void setUp() {
        storage = mock(S3BackupStorageService.class);
        postgres = mock(PostgresDatabaseRestorer.class);
        mongo = mock(MongoDatabaseRestorer.class);
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new BackupMetricsService(meterRegistry);
        auditService = new RestoreAuditService(
                properties(), new ObjectMapper().registerModule(new JavaTimeModule()));
        orchestrator = new RestoreOrchestratorService(
                properties(), storage, auditService, metricsService, postgres, mongo);
    }

    private BackupProperties properties() {
        BackupProperties.DatabaseConnection iam =
                new BackupProperties.DatabaseConnection("h", 5432, "iam_db", "iam", "iam_pass");
        BackupProperties.DatabaseConnection patient =
                new BackupProperties.DatabaseConnection("h", 5432, "patient_clinical_db", "clinical", "clinical_pass");
        BackupProperties.DatabaseConnection reporting =
                new BackupProperties.DatabaseConnection("h", 5432, "reporting_db", "reporting", "reporting_pass");
        return new BackupProperties(
                "0 0 3 * * ?", 7, rootDir, true, "secret", 30, 1800, 24, "0 0 4 1 * ?",
                new BackupProperties.Postgres(iam, patient, reporting),
                new BackupProperties.Mongo("h", 27017, "ai_db", "ai", "ai_pass"));
    }

    private Path localArtifact() throws Exception {
        Path artifact = rootDir.resolve("artefatos/postgres_patient_clinical_db.sql.gz.enc");
        Files.createDirectories(artifact.getParent());
        Files.writeString(artifact, "artefato-sintetico");
        return artifact;
    }

    @Test
    @DisplayName("resolve o alvo pelo nome do banco e aciona o restaurador PostgreSQL")
    void resolvesTargetByDatabaseName() throws Exception {
        Path artifact = localArtifact();
        when(postgres.restore(eq(DatabaseTarget.PATIENT_CLINICAL_DB), any(), any())).thenReturn(true);

        var outcome = orchestrator.restoreDatabase("patient_clinical_db", artifact.toString(), "abc123");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.target()).isEqualTo(DatabaseTarget.PATIENT_CLINICAL_DB);
        verify(postgres).restore(eq(DatabaseTarget.PATIENT_CLINICAL_DB), eq(artifact), eq("abc123"));
        verify(storage, never()).downloadArtifact(anyString(), any());
    }

    @Test
    @DisplayName("aceita o nome do enum e a forma normalizada do alvo")
    void acceptsEnumAndNormalizedNames() throws Exception {
        Path artifact = localArtifact();
        when(postgres.restore(any(), any(), any())).thenReturn(true);

        assertThat(orchestrator.restoreDatabase("PATIENT_CLINICAL_DB", artifact.toString(), null).success()).isTrue();
        assertThat(orchestrator.restoreDatabase("patientclinicaldb", artifact.toString(), null).success()).isTrue();
        assertThat(orchestrator.restoreDatabase("iam_db", artifact.toString(), null).target())
                .isEqualTo(DatabaseTarget.IAM_DB);
    }

    @Test
    @DisplayName("resolve o alvo MongoDB e aciona o restaurador correspondente")
    void resolvesMongoTarget() throws Exception {
        Path artifact = localArtifact();
        when(mongo.restore(eq(DatabaseTarget.MONGO_AI_DB), any(), any())).thenReturn(true);

        var outcome = orchestrator.restoreDatabase("ai_db", artifact.toString(), null);

        assertThat(outcome.success()).isTrue();
        verify(mongo).restore(eq(DatabaseTarget.MONGO_AI_DB), any(), any());
    }

    @Test
    @DisplayName("alvo desconhecido: nenhum restaurador é acionado e o evento é auditado")
    void rejectsUnknownTarget() throws Exception {
        var outcome = orchestrator.restoreDatabase("banco_inexistente", "/tmp/x.enc", null);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.target()).isNull();
        assertThat(outcome.message()).contains("não reconhecido");
        verify(postgres, never()).restore(any(), any(), any());
        verify(mongo, never()).restore(any(), any(), any());
        assertThat(auditService.readEntries()).hasSize(1);
    }

    @Test
    @DisplayName("chave remota é baixada para o staging, usada e depois removida")
    void downloadsRemoteArtifactToStaging() throws Exception {
        Path staged = rootDir.resolve("staging/artifact.abc12345");
        Files.createDirectories(staged.getParent());
        Files.writeString(staged, "conteudo-baixado");
        when(storage.downloadArtifact(eq("backups/postgresql/patient_clinical_db/x.enc"), any()))
                .thenReturn(staged);
        when(postgres.restore(eq(DatabaseTarget.PATIENT_CLINICAL_DB), any(), any())).thenReturn(true);

        var outcome = orchestrator.restoreDatabase(
                "patient_clinical_db", "backups/postgresql/patient_clinical_db/x.enc", null);

        assertThat(outcome.success()).isTrue();
        org.mockito.ArgumentCaptor<Path> captor = org.mockito.ArgumentCaptor.forClass(Path.class);
        verify(postgres).restore(eq(DatabaseTarget.PATIENT_CLINICAL_DB), captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(staged);
        assertThat(Files.exists(staged)).isFalse(); // staging higienizado após o uso
    }

    @Test
    @DisplayName("falha do restaurador vira resultado, auditoria e métrica de falha")
    void recordsFailureAndMetrics() throws Exception {
        Path artifact = localArtifact();
        when(postgres.restore(any(), any(), any()))
                .thenThrow(new br.edu.ifrn.promptuario.backup.restorer.RestoreFailedException(
                        "psql falhou (código 1): senha ***"));

        var outcome = orchestrator.restoreDatabase("patient_clinical_db", artifact.toString(), "abc");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.message()).contains("psql falhou");
        assertThat(auditService.readEntries()).hasSize(1);
        assertThat(auditService.readEntries().get(0).success()).isFalse();
        assertThat(meterRegistry.counter(BackupMetricsService.METRIC_STATUS,
                "target", "PATIENT_CLINICAL_DB", "status", "failed").count()).isEqualTo(1.0d);
    }
}
