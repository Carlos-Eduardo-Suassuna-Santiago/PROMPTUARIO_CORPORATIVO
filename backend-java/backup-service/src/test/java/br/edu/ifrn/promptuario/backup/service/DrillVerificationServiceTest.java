package br.edu.ifrn.promptuario.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.metrics.BackupMetricsService;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.restorer.MongoDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.PostgresDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.RestoreFailedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/** Testes do drill mensal de contingência (base efêmera, RTO/RPO e status persistido). */
class DrillVerificationServiceTest {

    private static final String PATIENT_DB = "patient_clinical_db";

    @TempDir
    Path rootDir;

    private BackupProperties properties;
    private PostgresDatabaseRestorer postgres;
    private MongoDatabaseRestorer mongo;
    private BackupMetricsService metricsService;
    private SimpleMeterRegistry meterRegistry;
    private DrillVerificationService drill;

    @BeforeEach
    void setUp() {
        properties = properties();
        postgres = mock(PostgresDatabaseRestorer.class);
        mongo = mock(MongoDatabaseRestorer.class);
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new BackupMetricsService(meterRegistry);
        RestoreAuditService auditService = new RestoreAuditService(
                properties, new ObjectMapper().registerModule(new JavaTimeModule()));
        drill = new DrillVerificationService(
                properties, auditService, metricsService,
                new ObjectMapper().registerModule(new JavaTimeModule()), postgres, mongo);
    }

    private BackupProperties properties() {
        BackupProperties.DatabaseConnection iam =
                new BackupProperties.DatabaseConnection("h", 5432, "iam_db", "iam", "iam_pass");
        BackupProperties.DatabaseConnection patient =
                new BackupProperties.DatabaseConnection("h", 5432, PATIENT_DB, "clinical", "clinical_pass");
        BackupProperties.DatabaseConnection reporting =
                new BackupProperties.DatabaseConnection("h", 5432, "reporting_db", "reporting", "reporting_pass");
        return new BackupProperties(
                "0 0 3 * * ?", 7, rootDir, true, "secret", 30, 1800, 24, "0 0 4 1 * ?",
                new BackupProperties.Postgres(iam, patient, reporting),
                new BackupProperties.Mongo("h", 27017, "ai_db", "ai", "ai_pass"));
    }

    /** Materializa um artefato no layout esperado, com a idade desejada. */
    private Path placeArtifact(String kindFolder, String dbname, long ageHours) throws Exception {
        Path directory = rootDir.resolve(kindFolder).resolve(dbname).resolve("2026/11/11");
        Files.createDirectories(directory);
        Path artifact = directory.resolve("postgres_" + dbname + "_20261111_000000.sql.gz.enc");
        Files.writeString(artifact, "artefato-sintetico");
        Files.setLastModifiedTime(artifact, FileTime.from(Instant.now().minus(ageHours, ChronoUnit.HOURS)));
        return artifact;
    }

    @Test
    @DisplayName("drill bem-sucedido: restaura na base efêmera e atesta RTO e RPO")
    void succeedsAndAttestsRtoRpo() throws Exception {
        placeArtifact("postgresql", PATIENT_DB, 2);
        when(postgres.restoreInto(any(), any(), anyString())).thenReturn(true);

        DrillVerificationService.DrillResult result = drill.runDrill(DatabaseTarget.PATIENT_CLINICAL_DB);

        assertThat(result.success()).isTrue();
        assertThat(result.rpoMet()).isTrue();   // artefato de 2h dentro da janela de 24h
        assertThat(result.rtoMet()).isTrue();   // muito abaixo do orçamento de 1800s
        assertThat(result.backupAgeHours()).isEqualTo(2);

        // O drill NUNCA aponta para a produção: usa a base efêmera com sufixo.
        ArgumentCaptor<DatabaseEndpoint> captor = ArgumentCaptor.forClass(DatabaseEndpoint.class);
        verify(postgres).restoreInto(captor.capture(), any(), anyString());
        assertThat(captor.getValue().dbname()).isEqualTo(PATIENT_DB + DrillVerificationService.DRILL_DB_SUFFIX);
        assertThat(captor.getValue().dbname()).isNotEqualTo(PATIENT_DB);

        assertThat(meterRegistry.counter(BackupMetricsService.METRIC_DRILL_TOTAL,
                "target", "PATIENT_CLINICAL_DB", "status", "success", "rpo", "met").count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get(BackupMetricsService.METRIC_DRILL_STATUS)
                .tag("target", "PATIENT_CLINICAL_DB").gauge().value()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("sem artefato disponível o drill falha e informa o motivo")
    void failsWhenNoArtifactIsAvailable() {
        DrillVerificationService.DrillResult result = drill.runDrill(DatabaseTarget.REPORTING_DB);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Nenhum artefato");
        assertThat(result.rpoMet()).isFalse();
    }

    @Test
    @DisplayName("artefato fora da janela de RPO é sinalizado como não conforme")
    void detectsRpoBreach() throws Exception {
        placeArtifact("postgresql", PATIENT_DB, 48); // 48h > janela de 24h
        when(postgres.restoreInto(any(), any(), anyString())).thenReturn(true);

        DrillVerificationService.DrillResult result = drill.runDrill(DatabaseTarget.PATIENT_CLINICAL_DB);

        assertThat(result.success()).isTrue();
        assertThat(result.rpoMet()).isFalse();
        assertThat(result.backupAgeHours()).isGreaterThanOrEqualTo(48);
    }

    @Test
    @DisplayName("falha da restauração no drill é registrada sem derrubar o serviço")
    void registersRestoreFailure() throws Exception {
        placeArtifact("postgresql", PATIENT_DB, 1);
        when(postgres.restoreInto(any(), any(), anyString()))
                .thenThrow(new RestoreFailedException("psql falhou (código 1): senha ***"));

        DrillVerificationService.DrillResult result = drill.runDrill(DatabaseTarget.PATIENT_CLINICAL_DB);

        assertThat(result.success()).isFalse();
        assertThat(result.rtoMet()).isFalse();
        assertThat(result.message()).contains("psql falhou");
        assertThat(meterRegistry.get(BackupMetricsService.METRIC_DRILL_STATUS)
                .tag("target", "PATIENT_CLINICAL_DB").gauge().value()).isEqualTo(0.0d);
    }

    @Test
    @DisplayName("o resultado do drill é persistido em backup_restore_drill_status.json")
    void persistsDrillStatus() throws Exception {
        placeArtifact("postgresql", PATIENT_DB, 3);
        when(postgres.restoreInto(any(), any(), anyString())).thenReturn(true);
        var result = drill.runDrill(DatabaseTarget.PATIENT_CLINICAL_DB);

        Path statusFile = drill.persistStatus(java.util.List.of(result));

        assertThat(statusFile).isNotNull();
        assertThat(statusFile.getFileName().toString())
                .isEqualTo(DrillVerificationService.DRILL_STATUS_FILE_NAME);
        String json = Files.readString(statusFile);
        assertThat(json).contains("rpoWindowHours").contains("patient_clinical_db").contains("rtoMet");
    }

    @Test
    @DisplayName("drill do MongoDB usa a base efêmera correspondente")
    void drillsMongoTarget() throws Exception {
        placeArtifact("mongodb", "ai_db", 1);
        when(mongo.restoreInto(any(), any(), anyString())).thenReturn(true);

        DrillVerificationService.DrillResult result = drill.runDrill(DatabaseTarget.MONGO_AI_DB);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<DatabaseEndpoint> captor = ArgumentCaptor.forClass(DatabaseEndpoint.class);
        verify(mongo).restoreInto(captor.capture(), any(), anyString());
        assertThat(captor.getValue().dbname()).isEqualTo("ai_db" + DrillVerificationService.DRILL_DB_SUFFIX);
        verify(postgres, org.mockito.Mockito.never()).restoreInto(any(), any(), any());
    }
}
