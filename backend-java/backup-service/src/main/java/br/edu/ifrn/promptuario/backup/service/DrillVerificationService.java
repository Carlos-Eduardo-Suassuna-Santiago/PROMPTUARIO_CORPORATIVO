package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.metrics.BackupMetricsService;
import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.restorer.AbstractStreamingDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.MongoDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.PostgresDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.RestoreFailedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Drill de contingência (dry-run) para atestar compliance de RTO e RPO.
 *
 * <p>Restaura o artefato mais recente em um banco <strong>efêmero/homologação</strong>
 * (sufixo {@value #DRILL_DB_SUFFIX} acrescido ao nome do banco), sem sobrescrever
 * a produção, e registra o resultado em {@value #DRILL_STATUS_FILE_NAME}.
 */
@Service
public class DrillVerificationService {

    /** Arquivo de status consolidado do último drill. */
    public static final String DRILL_STATUS_FILE_NAME = "backup_restore_drill_status.json";

    /** Sufixo do banco efêmero usado no drill (nunca aponta para produção). */
    public static final String DRILL_DB_SUFFIX = "_drill";

    private static final Logger log = LoggerFactory.getLogger(DrillVerificationService.class);
    private static final String EVENT_DRILL = "DRILL";

    private static final List<DatabaseTarget> DRILL_TARGETS = List.of(
            DatabaseTarget.IAM_DB,
            DatabaseTarget.PATIENT_CLINICAL_DB,
            DatabaseTarget.REPORTING_DB,
            DatabaseTarget.MONGO_AI_DB);

    private final BackupProperties backupProperties;
    private final RestoreAuditService auditService;
    private final BackupMetricsService metricsService;
    private final ObjectMapper objectMapper;
    private final PostgresDatabaseRestorer postgresRestorer;
    private final MongoDatabaseRestorer mongoRestorer;

    public DrillVerificationService(
            BackupProperties backupProperties,
            RestoreAuditService auditService,
            BackupMetricsService metricsService,
            ObjectMapper objectMapper,
            PostgresDatabaseRestorer postgresRestorer,
            MongoDatabaseRestorer mongoRestorer) {
        this.backupProperties = backupProperties;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        this.postgresRestorer = postgresRestorer;
        this.mongoRestorer = mongoRestorer;
    }

    /**
     * Drill mensal automatizado (dry-run) de todos os alvos. Nunca lança: falhas
     * são registradas no status e nas métricas.
     */
    @Scheduled(cron = "${promptuario.backup.drill-cron}")
    public void runMonthlyDrill() {
        log.info("═══ Iniciando drill mensal de contingência (dry-run) ═══");
        List<DrillResult> results = new ArrayList<>();
        for (DatabaseTarget target : DRILL_TARGETS) {
            try {
                results.add(runDrill(target));
            } catch (RuntimeException e) {
                log.error("Erro inesperado no drill de {}", target, e);
            }
        }
        persistStatus(results);
        log.info("═══ Drill de contingência concluído ({} alvos avaliados) ═══", results.size());
    }

    /**
     * Executa o drill de um alvo específico restaurando o artefato mais recente
     * na base efêmera e medindo RTO/RPO.
     */
    public DrillResult runDrill(DatabaseTarget target) {
        long startNanos = System.nanoTime();
        Path artifact = findLatestArtifact(target);

        if (artifact == null) {
            return record(target, null, null, false, false, 0L, 0L,
                    "Nenhum artefato de backup disponível para o drill");
        }

        String artifactName = artifact.toString();
        String sha256;
        long ageHours;
        try {
            sha256 = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);
            ageHours = ageHours(artifact);
        } catch (IOException e) {
            return record(target, artifactName, null, false, false, 0L, elapsedSeconds(startNanos),
                    "Falha ao inspecionar o artefato do drill: " + e.getMessage());
        }

        boolean rpoMet = ageHours <= backupProperties.rpoHours();
        DatabaseEndpoint drillEndpoint = drillEndpoint(target);
        String message;
        boolean success = false;

        try {
            success = executeDrillRestore(target, drillEndpoint, artifact, sha256);
            message = success
                    ? "Drill concluído na base efêmera '" + drillEndpoint.dbname() + "'"
                    : "Drill barrado na validação pré-restore do artefato";
        } catch (RestoreFailedException e) {
            message = e.getMessage();
            log.error("Drill de {} falhou: {}", target, message);
        } catch (RuntimeException e) {
            message = "Erro inesperado no drill: " + e.getMessage();
            log.error(message, e);
        }

        return record(target, artifactName, drillEndpoint.dbname(), success, rpoMet, ageHours,
                elapsedSeconds(startNanos), message);
    }

    // ── Localização do artefato e da base efêmera ────────────────────────────

    /** Artefato de backup mais recente do alvo no volume local (ou {@code null}). */
    private Path findLatestArtifact(DatabaseTarget target) {
        Path base = backupProperties.rootDir()
                .resolve(kindOf(target).folder())
                .resolve(dbnameOf(target));
        if (!Files.isDirectory(base)) {
            log.warn("Diretório de artefatos inexistente para o drill de {}: {}", target, base);
            return null;
        }
        try (Stream<Path> paths = Files.walk(base)) {
            return paths.filter(Files::isRegularFile)
                    .max(Comparator.comparing(DrillVerificationService::lastModified))
                    .orElse(null);
        } catch (IOException e) {
            log.warn("Falha ao localizar artefatos de {}: {}", target, e.getMessage());
            return null;
        }
    }

    private static FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0L);
        }
    }

    private static long ageHours(Path artifact) throws IOException {
        return Duration.between(Files.getLastModifiedTime(artifact).toInstant(), Instant.now()).toHours();
    }

    /** Endpoint da base efêmera ({@code <dbname>_drill}) — jamais a produção. */
    private DatabaseEndpoint drillEndpoint(DatabaseTarget target) {
        DatabaseEndpoint base = baseEndpoint(target);
        return new DatabaseEndpoint(
                base.host(), base.port(), base.user(), base.password(), base.dbname() + DRILL_DB_SUFFIX);
    }

    private DatabaseEndpoint baseEndpoint(DatabaseTarget target) {
        if (target == DatabaseTarget.MONGO_AI_DB) {
            BackupProperties.Mongo mongo = backupProperties.mongo();
            return new DatabaseEndpoint(mongo.host(), mongo.port(), mongo.user(), mongo.password(), mongo.dbname());
        }
        BackupProperties.DatabaseConnection connection = backupProperties.postgresTargets().get(target);
        if (connection == null) {
            throw new IllegalArgumentException("Alvo não suportado pelo drill: " + target);
        }
        return new DatabaseEndpoint(
                connection.host(), connection.port(), connection.user(),
                connection.password(), connection.dbname());
    }

    private String dbnameOf(DatabaseTarget target) {
        return baseEndpoint(target).dbname();
    }

    private static BackupKind kindOf(DatabaseTarget target) {
        return target == DatabaseTarget.MONGO_AI_DB ? BackupKind.MONGO : BackupKind.POSTGRES;
    }

    /**
     * Aciona o restaurador concreto apontando para a base efêmera
     * ({@code restoreInto}), sem alterar o alvo de produção.
     */
    private boolean executeDrillRestore(
            DatabaseTarget target, DatabaseEndpoint drillEndpoint, Path artifact, String sha256) {
        return switch (target) {
            case IAM_DB, PATIENT_CLINICAL_DB, REPORTING_DB ->
                    postgresRestorer.restoreInto(drillEndpoint, artifact, sha256);
            case MONGO_AI_DB -> mongoRestorer.restoreInto(drillEndpoint, artifact, sha256);
            case MINIO_BUCKETS -> throw new IllegalArgumentException(
                    "MINIO_BUCKETS não é um alvo de drill de contingência");
        };
    }

    // ── Registro do resultado ────────────────────────────────────────────────

    private DrillResult record(
            DatabaseTarget target,
            String artifact,
            String drillDatabase,
            boolean success,
            boolean rpoMet,
            long backupAgeHours,
            double rtoSeconds,
            String message) {

        boolean rtoMet = success && rtoSeconds <= backupProperties.restoreTimeoutSeconds();
        DrillResult result = new DrillResult(
                target, drillDatabase, artifact, success, rpoMet, rtoMet, rtoSeconds, backupAgeHours,
                message, Instant.now());

        metricsService.recordDrill(target, success, rpoMet);
        auditService.record(RestoreAuditService.RestoreAuditEntry.of(
                EVENT_DRILL, target.name(), artifact, null, success, (long) (rtoSeconds * 1000.0d), message));

        log.info("Drill {}: sucesso={} rto={}s (met={}) rpo={}h (met={}) — {}",
                target, success, rtoSeconds, rtoMet, backupAgeHours, rpoMet, message);
        return result;
    }

    /** Persiste o status consolidado em {@value #DRILL_STATUS_FILE_NAME}. */
    public Path persistStatus(List<DrillResult> results) {
        Path statusFile = backupProperties.rootDir().resolve(DRILL_STATUS_FILE_NAME);
        try {
            if (statusFile.getParent() != null) {
                Files.createDirectories(statusFile.getParent());
            }
            DrillStatus status =
                    new DrillStatus(Instant.now(), backupProperties.rpoHours(), List.copyOf(results));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(statusFile.toFile(), status);
            log.info("Status do drill gravado em {}", statusFile);
            return statusFile;
        } catch (IOException e) {
            log.error("Falha ao gravar o status do drill em {}: {}", statusFile, e.getMessage());
            return null;
        }
    }

    private static double elapsedSeconds(long startNanos) {
        return Math.round(((System.nanoTime() - startNanos) / 1_000_000_000.0d) * 100.0d) / 100.0d;
    }

    /** Resultado de um drill por alvo (atestado de RTO/RPO). */
    public record DrillResult(
            DatabaseTarget target,
            String drillDatabase,
            String artifact,
            boolean success,
            boolean rpoMet,
            boolean rtoMet,
            double rtoSeconds,
            long backupAgeHours,
            String message,
            Instant executedAt) {
    }

    /** Estado consolidado do último drill. */
    public record DrillStatus(Instant generatedAt, int rpoWindowHours, List<DrillResult> results) {
    }
}
