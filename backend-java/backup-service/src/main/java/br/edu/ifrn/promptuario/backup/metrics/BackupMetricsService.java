package br.edu.ifrn.promptuario.backup.metrics;

import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * Métricas padronizadas do serviço de backup (Micrometer/Prometheus).
 *
 * <p>Os nomes são declarados na notação pontuada do Micrometer e o
 * {@code PrometheusMeterRegistry} os converte para snake_case com unidade/sufixo,
 * produzindo exatamente: {@code backup_execution_duration_seconds},
 * {@code backup_artifact_size_bytes}, {@code backup_last_success_timestamp_seconds},
 * {@code backup_status_total}, {@code backup_retention_deleted_total} e
 * {@code backup_restore_duration_seconds}.
 *
 * <p>Os gauges mantêm referência forte do valor corrente em mapas por alvo,
 * evitando o descarte por GC (que faria o gauge reportar {@code NaN}).
 */
@Service
public class BackupMetricsService {

    /** Nome Micrometer do timer de execução dos dumps por banco. */
    public static final String METRIC_EXECUTION_DURATION = "backup.execution.duration";

    /** Nome Micrometer do gauge de tamanho do artefato. */
    public static final String METRIC_ARTIFACT_SIZE = "backup.artifact.size";

    /** Nome Micrometer do gauge do último backup bem-sucedido. */
    public static final String METRIC_LAST_SUCCESS = "backup.last.success.timestamp";

    /** Nome Micrometer do contador de execuções por status. */
    public static final String METRIC_STATUS = "backup.status";

    /** Nome Micrometer do contador de artefatos expurgados. */
    public static final String METRIC_RETENTION_DELETED = "backup.retention.deleted";

    /** Nome Micrometer do timer de restaurações. */
    public static final String METRIC_RESTORE_DURATION = "backup.restore.duration";

    /** Nome Micrometer do gauge de resultado do drill de contingência. */
    public static final String METRIC_DRILL_STATUS = "backup.restore.drill.status";

    /** Nome Micrometer do contador de drills executados. */
    public static final String METRIC_DRILL_TOTAL = "backup.restore.drill.total";

    /** Valor canônico do rótulo {@code status} em caso de sucesso. */
    public static final String STATUS_SUCCESS = "success";

    /** Valor canônico do rótulo {@code status} em caso de falha. */
    public static final String STATUS_FAILED = "failed";

    private static final String TAG_TARGET = "target";
    private static final String TAG_STATUS = "status";
    private static final String TAG_RPO = "rpo";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<DatabaseTarget, AtomicLong> artifactSizes = new ConcurrentHashMap<>();
    private final ConcurrentMap<DatabaseTarget, AtomicLong> lastSuccessEpochSeconds = new ConcurrentHashMap<>();
    private final ConcurrentMap<DatabaseTarget, AtomicInteger> drillStatuses = new ConcurrentHashMap<>();

    public BackupMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Registra o término de um ciclo de backup por alvo: duração, total por
     * status e — em caso de sucesso — o tamanho do artefato e o instante do
     * último backup bem-sucedido.
     */
    public void recordBackupExecution(
            DatabaseTarget target, boolean success, long durationMillis, long artifactBytes) {
        String status = statusLabel(success);
        recordDuration(METRIC_EXECUTION_DURATION, "Tempo total da execução do backup por banco",
                target, status, durationMillis);
        meterRegistry.counter(METRIC_STATUS, TAG_TARGET, target.name(), TAG_STATUS, status).increment();

        if (success) {
            artifactSizeGauge(target).set(Math.max(artifactBytes, 0L));
            lastSuccessGauge(target).set(Instant.now().getEpochSecond());
        }
    }

    /** Registra uma restauração (tempo gasto e total por status). */
    public void recordRestoreExecution(DatabaseTarget target, boolean success, long durationMillis) {
        String status = statusLabel(success);
        recordDuration(METRIC_RESTORE_DURATION, "Tempo gasto em restaurações", target, status, durationMillis);
        meterRegistry.counter(METRIC_STATUS, TAG_TARGET, target.name(), TAG_STATUS, status).increment();
    }

    /** Contabiliza os artefatos expurgados pela política de retenção. */
    public void recordRetentionDeleted(long deleted) {
        meterRegistry.counter(METRIC_RETENTION_DELETED).increment(Math.max(deleted, 0L));
    }

    /** Registra o resultado do drill de contingência por alvo. */
    public void recordDrill(DatabaseTarget target, boolean success, boolean rpoMet) {
        drillStatusGauge(target).set(success ? 1 : 0);
        meterRegistry.counter(METRIC_DRILL_TOTAL,
                        TAG_TARGET, target.name(),
                        TAG_STATUS, statusLabel(success),
                        TAG_RPO, rpoMet ? "met" : "missed")
                .increment();
    }

    /** Epoch (segundos) do último backup bem-sucedido do alvo. */
    public long lastSuccessEpochSeconds(DatabaseTarget target) {
        return lastSuccessGauge(target).get();
    }

    /** Tamanho (bytes) do último artefato bem-sucedido do alvo. */
    public long artifactSizeBytes(DatabaseTarget target) {
        return artifactSizeGauge(target).get();
    }

    /** Resultado (1/0) do último drill do alvo. */
    public int drillStatus(DatabaseTarget target) {
        return drillStatusGauge(target).get();
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private static String statusLabel(boolean success) {
        return success ? STATUS_SUCCESS : STATUS_FAILED;
    }

    private void recordDuration(
            String name, String description, DatabaseTarget target, String status, long durationMillis) {
        Timer.builder(name)
                .description(description)
                .tag(TAG_TARGET, target.name())
                .tag(TAG_STATUS, status)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(durationMillis, 0L)));
    }

    private AtomicLong artifactSizeGauge(DatabaseTarget target) {
        return artifactSizes.computeIfAbsent(target, candidate -> {
            AtomicLong holder = new AtomicLong(0L);
            Gauge.builder(METRIC_ARTIFACT_SIZE, holder, AtomicLong::doubleValue)
                    .description("Tamanho do último artefato comprimido e criptografado, em bytes")
                    .baseUnit("bytes")
                    .tag(TAG_TARGET, candidate.name())
                    .strongReference(true)
                    .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong lastSuccessGauge(DatabaseTarget target) {
        return lastSuccessEpochSeconds.computeIfAbsent(target, candidate -> {
            AtomicLong holder = new AtomicLong(0L);
            Gauge.builder(METRIC_LAST_SUCCESS, holder, AtomicLong::doubleValue)
                    .description("Epoch em segundos do último backup concluído com sucesso")
                    .baseUnit("seconds")
                    .tag(TAG_TARGET, candidate.name())
                    .strongReference(true)
                    .register(meterRegistry);
            return holder;
        });
    }

    private AtomicInteger drillStatusGauge(DatabaseTarget target) {
        return drillStatuses.computeIfAbsent(target, candidate -> {
            AtomicInteger holder = new AtomicInteger(0);
            Gauge.builder(METRIC_DRILL_STATUS, holder, AtomicInteger::doubleValue)
                    .description("Resultado do último drill de contingência (1=sucesso, 0=falha)")
                    .tag(TAG_TARGET, candidate.name())
                    .strongReference(true)
                    .register(meterRegistry);
            return holder;
        });
    }
}
