package br.edu.ifrn.promptuario.backup.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Valida a nomenclatura exposta no endpoint {@code /actuator/prometheus}.
 *
 * <p>O teste faz um scrape real do {@link PrometheusMeterRegistry}, garantindo
 * que os nomes declarados na notação Micrometer produzem exatamente os nomes
 * consumidos pelos alertas e pelo dashboard do Grafana.
 */
class BackupMetricsServiceTest {

    private PrometheusMeterRegistry registry;
    private BackupMetricsService metrics;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        metrics = new BackupMetricsService(registry);
    }

    @Test
    @DisplayName("exporta os nomes Prometheus exigidos pelos alertas e pelo dashboard")
    void exportsPrometheusNames() {
        metrics.recordBackupExecution(DatabaseTarget.PATIENT_CLINICAL_DB, true, 1500L, 8_388_608L);
        metrics.recordRestoreExecution(DatabaseTarget.IAM_DB, false, 250L);
        metrics.recordRetentionDeleted(3);
        metrics.recordDrill(DatabaseTarget.REPORTING_DB, true, true);

        String scrape = registry.scrape();

        assertThat(scrape).contains("backup_execution_duration_seconds");
        assertThat(scrape).contains("backup_status_total");
        assertThat(scrape).contains("backup_retention_deleted_total");
        assertThat(scrape).contains("backup_restore_duration_seconds");
        assertThat(scrape).contains("backup_artifact_size_bytes{target=\"PATIENT_CLINICAL_DB\"}");
        assertThat(scrape).contains("backup_last_success_timestamp_seconds{target=\"PATIENT_CLINICAL_DB\"}");
        assertThat(scrape).contains("backup_restore_drill_status{target=\"REPORTING_DB\"}");
    }

    @Test
    @DisplayName("rotula status como success/failed e mantém os valores correntes")
    void exposesStatusLabelsAndCurrentValues() {
        metrics.recordBackupExecution(DatabaseTarget.IAM_DB, true, 42L, 2048L);
        metrics.recordBackupExecution(DatabaseTarget.REPORTING_DB, false, 7L, 0L);

        String scrape = registry.scrape();

        assertThat(scrape).contains("status=\"success\"");
        assertThat(scrape).contains("status=\"failed\"");
        assertThat(metrics.artifactSizeBytes(DatabaseTarget.IAM_DB)).isEqualTo(2048L);
        assertThat(metrics.lastSuccessEpochSeconds(DatabaseTarget.IAM_DB)).isPositive();
        assertThat(metrics.artifactSizeBytes(DatabaseTarget.REPORTING_DB)).isZero();
        assertThat(metrics.lastSuccessEpochSeconds(DatabaseTarget.REPORTING_DB)).isZero();
    }

    @Test
    @DisplayName("drill publica o gauge 1/0 e o contador com o rótulo de RPO")
    void exposesDrillMetrics() {
        metrics.recordDrill(DatabaseTarget.MONGO_AI_DB, false, true);

        String scrape = registry.scrape();

        assertThat(scrape).contains("backup_restore_drill_status{target=\"MONGO_AI_DB\"} 0.0");
        assertThat(scrape).contains("rpo=\"met\"");
        assertThat(metrics.drillStatus(DatabaseTarget.MONGO_AI_DB)).isZero();
    }
}
