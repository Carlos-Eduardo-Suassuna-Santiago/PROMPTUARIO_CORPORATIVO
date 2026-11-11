package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.restorer.DatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.MongoDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.PostgresDatabaseRestorer;
import br.edu.ifrn.promptuario.backup.restorer.RestoreFailedException;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquestrador das restaurações controladas (DR).
 *
 * <p>Responsabilidades: resolver o alvo a partir do nome informado, preparar o
 * artefato (local ou baixado do MinIO para um diretório de staging temporário),
 * acionar o restaurador correto, gravar o log de auditoria e atualizar as
 * métricas de restauração.
 */
@Service
public class RestoreOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(RestoreOrchestratorService.class);

    /** Subdiretório de staging para artefatos remotos. */
    private static final String STAGING_DIR = "staging";
    private static final String EVENT_RESTORE = "RESTORE";

    private final BackupProperties backupProperties;
    private final S3BackupStorageService storageService;
    private final RestoreAuditService auditService;
    private final MeterRegistry meterRegistry;
    private final PostgresDatabaseRestorer postgresRestorer;
    private final MongoDatabaseRestorer mongoRestorer;

    public RestoreOrchestratorService(
            BackupProperties backupProperties,
            S3BackupStorageService storageService,
            RestoreAuditService auditService,
            MeterRegistry meterRegistry,
            PostgresDatabaseRestorer postgresRestorer,
            MongoDatabaseRestorer mongoRestorer) {
        this.backupProperties = backupProperties;
        this.storageService = storageService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
        this.postgresRestorer = postgresRestorer;
        this.mongoRestorer = mongoRestorer;
    }

    /**
     * Restaura um banco a partir de um artefato local ou de uma chave no MinIO.
     *
     * @param targetName nome do alvo: banco ({@code patient_clinical_db}),
     *                   enum ({@code PATIENT_CLINICAL_DB}) ou forma normalizada
     * @param filePath   caminho local do artefato ou chave no MinIO
     * @param sha256     SHA-256 esperado (do manifesto); {@code null} desabilita a checagem
     * @return resultado consolidado da operação (nunca {@code null}, nunca lança)
     */
    public RestoreOutcome restoreDatabase(String targetName, String filePath, String sha256) {
        long startNanos = System.nanoTime();

        DatabaseTarget target = resolveTarget(targetName);
        if (target == null) {
            String message = "Alvo não reconhecido para restauração: " + targetName;
            log.error("{}", message);
            auditService.record(RestoreAuditService.RestoreAuditEntry.of(
                    EVENT_RESTORE, String.valueOf(targetName), filePath, sha256, false, 0L, message));
            return new RestoreOutcome(false, null, targetName, filePath, 0L, message);
        }

        return execute(target, targetName, filePath, sha256, startNanos);
    }

    private RestoreOutcome execute(
            DatabaseTarget target, String targetName, String filePath, String sha256, long startNanos) {

        boolean success = false;
        boolean staged = false;
        Path localArtifact = null;
        String message;

        try {
            Path candidate = Path.of(filePath);
            if (Files.isRegularFile(candidate)) {
                localArtifact = candidate;
            } else {
                // Não é um arquivo local: trata como chave remota no MinIO.
                localArtifact = downloadToStaging(filePath);
                staged = true;
            }

            success = restorerFor(target).restore(target, localArtifact, sha256);
            message = success
                    ? "Restauração concluída"
                    : "Restauração barrada na validação pré-restore (arquivo ausente ou SHA-256 divergente)";
        } catch (RestoreFailedException e) {
            message = e.getMessage();
            log.error("Restauração de '{}' falhou: {}", target, message);
        } catch (RuntimeException e) {
            message = "Erro inesperado na restauração: " + e.getMessage();
            log.error(message, e);
        } finally {
            if (staged) {
                deleteStagedQuietly(localArtifact);
            }
        }

        long elapsedMillis = elapsedMillis(startNanos);
        recordMetrics(target, success, elapsedMillis);
        auditService.record(RestoreAuditService.RestoreAuditEntry.of(
                EVENT_RESTORE, target.name(), filePath, sha256, success, elapsedMillis, message));

        if (success) {
            log.info("Restauração de '{}' finalizada com sucesso em {} ms", target, elapsedMillis);
        }
        return new RestoreOutcome(success, target, targetName, filePath, elapsedMillis, message);
    }

    /** Resultado de uma operação de restauração. */
    public record RestoreOutcome(
            boolean success,
            DatabaseTarget target,
            String targetName,
            String artifact,
            long durationMillis,
            String message) {
    }

    // ── Resolução do alvo e do artefato ──────────────────────────────────────

    /**
     * Resolve o alvo a partir do nome do enum, do nome do banco ou de uma forma
     * normalizada (sem separadores e sem distinção de maiúsculas).
     * Ex.: {@code patient_clinical_db}, {@code PATIENT_CLINICAL_DB} e
     * {@code patientclinicaldb} resolvem para
     * {@link DatabaseTarget#PATIENT_CLINICAL_DB}.
     */
    private DatabaseTarget resolveTarget(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }
        String raw = targetName.trim();
        String normalized = normalize(raw);

        for (DatabaseTarget candidate : DatabaseTarget.values()) {
            if (candidate.name().equalsIgnoreCase(raw) || normalize(candidate.name()).equals(normalized)) {
                return candidate;
            }
        }
        for (var entry : backupProperties.postgresTargets().entrySet()) {
            String dbname = entry.getValue().dbname();
            if (dbname.equalsIgnoreCase(raw) || normalize(dbname).equals(normalized)) {
                return entry.getKey();
            }
        }
        String mongoDbname = backupProperties.mongo().dbname();
        if (mongoDbname.equalsIgnoreCase(raw) || normalize(mongoDbname).equals(normalized)) {
            return DatabaseTarget.MONGO_AI_DB;
        }
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Baixa a chave remota para o diretório de staging com nome único. */
    private Path downloadToStaging(String objectKey) {
        Path keyPath = Path.of(objectKey);
        String fileName = keyPath.getFileName() == null ? "artifact" : keyPath.getFileName().toString();
        Path destination = backupProperties.rootDir()
                .resolve(STAGING_DIR)
                .resolve(fileName + "." + UUID.randomUUID().toString().substring(0, 8));
        log.info("Artefato remoto informado; baixando '{}' para o staging", objectKey);
        return storageService.downloadArtifact(objectKey, destination);
    }

    private void deleteStagedQuietly(Path staged) {
        if (staged == null) {
            return;
        }
        try {
            if (Files.deleteIfExists(staged)) {
                log.debug("Artefato de staging removido: {}", staged);
            }
        } catch (IOException e) {
            log.warn("Não foi possível remover o staging {}: {}", staged, e.getMessage());
        }
    }

    private DatabaseRestorer restorerFor(DatabaseTarget target) {
        return switch (target) {
            case IAM_DB, PATIENT_CLINICAL_DB, REPORTING_DB -> postgresRestorer;
            case MONGO_AI_DB -> mongoRestorer;
            case MINIO_BUCKETS -> throw new IllegalArgumentException(
                    "MINIO_BUCKETS não é um alvo de restauração de banco de dados");
        };
    }

    private void recordMetrics(DatabaseTarget target, boolean success, long elapsedMillis) {
        meterRegistry.counter("backup_restore_total",
                        "target", target.name(),
                        "outcome", success ? "success" : "failure")
                .increment();
        meterRegistry.timer("backup_restore_duration_seconds", "target", target.name())
                .record(Duration.ofMillis(elapsedMillis));
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
