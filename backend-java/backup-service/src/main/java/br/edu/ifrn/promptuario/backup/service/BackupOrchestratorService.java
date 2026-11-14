package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.dumper.DatabaseDumper;
import br.edu.ifrn.promptuario.backup.dumper.MongoDatabaseDumper;
import br.edu.ifrn.promptuario.backup.dumper.PostgresDatabaseDumper;
import br.edu.ifrn.promptuario.backup.metrics.BackupMetricsService;
import br.edu.ifrn.promptuario.backup.model.BackupJobExecution;
import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.model.BackupManifest;
import br.edu.ifrn.promptuario.backup.model.BackupStatus;
import br.edu.ifrn.promptuario.backup.model.DatabaseBackupResult;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.model.ExecutionMode;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquestrador do ciclo completo de backup.
 *
 * <p>Cada alvo é executado em um bloco {@code try/catch} isolado: uma falha em
 * um banco jamais interrompe o dump dos demais, garantindo a máxima cobertura
 * de proteção possível em cenário degradado.
 */
@Service
public class BackupOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(BackupOrchestratorService.class);

    /** Ordem do ciclo (topologia unificada pós-Fase 12). */
    private static final List<DatabaseTarget> DUMP_ORDER = List.of(
            DatabaseTarget.IAM_DB,
            DatabaseTarget.PATIENT_CLINICAL_DB,
            DatabaseTarget.REPORTING_DB,
            DatabaseTarget.MONGO_AI_DB);

    private final BackupProperties backupProperties;
    private final CryptoService cryptoService;
    private final S3BackupStorageService storageService;
    private final StatusReportService statusReportService;
    private final RetentionCleanupService retentionCleanupService;
    private final BackupMetricsService metricsService;
    private final PostgresDatabaseDumper postgresDumper;
    private final MongoDatabaseDumper mongoDumper;

    public BackupOrchestratorService(
            BackupProperties backupProperties,
            CryptoService cryptoService,
            S3BackupStorageService storageService,
            StatusReportService statusReportService,
            RetentionCleanupService retentionCleanupService,
            BackupMetricsService metricsService,
            PostgresDatabaseDumper postgresDumper,
            MongoDatabaseDumper mongoDumper) {
        this.backupProperties = backupProperties;
        this.cryptoService = cryptoService;
        this.storageService = storageService;
        this.statusReportService = statusReportService;
        this.retentionCleanupService = retentionCleanupService;
        this.metricsService = metricsService;
        this.postgresDumper = postgresDumper;
        this.mongoDumper = mongoDumper;
    }

    /**
     * Executa o ciclo completo: bucket &rarr; dumps isolados &rarr; manifesto
     * criptográfico &rarr; status persistente &rarr; retenção.
     *
     * @param mode modo de execução corrente
     * @return a execução consolidada (nunca {@code null}, nunca lança)
     */
    public BackupJobExecution executeFullBackup(ExecutionMode mode) {
        BackupJobExecution execution = BackupJobExecution.builder()
                .jobId(UUID.randomUUID().toString())
                .mode(mode)
                .startedAt(Instant.now())
                .build();

        log.info("═══ Iniciando ciclo de backup [{}] (modo {}) ═══", execution.getJobId(), mode);

        // 1. Garante o bucket de destino (falha aqui não impede os dumps locais).
        try {
            storageService.ensureBucketExists(storageService.defaultBucket());
        } catch (RuntimeException e) {
            log.warn("Não foi possível garantir o bucket '{}': {}", storageService.defaultBucket(), e.getMessage());
        }

        // 2. Dump de cada alvo com isolamento total de falhas.
        for (DatabaseTarget target : DUMP_ORDER) {
            long targetStartNanos = System.nanoTime();
            DatabaseBackupResult result = runIsolated(target, mode);
            execution.addResult(result);
            // Observabilidade em tempo real: duração, status, tamanho e último sucesso.
            metricsService.recordBackupExecution(
                    target, result.isSuccess(), elapsedMillis(targetStartNanos), result.sizeBytes());
        }

        execution.finish();

        // 3. Manifesto criptográfico do ciclo.
        try {
            statusReportService.writeManifest(buildManifest(execution));
        } catch (Exception e) {
            log.error("Falha ao gravar o manifesto do backup: {}", e.getMessage(), e);
        }

        // 4. Relatório de status persistente.
        try {
            statusReportService.writeStatusReport(execution);
        } catch (Exception e) {
            log.error("Falha ao gravar o status do backup: {}", e.getMessage(), e);
        }

        // 5. Política de retenção.
        try {
            retentionCleanupService.cleanup();
        } catch (RuntimeException e) {
            log.warn("Falha na limpeza de retenção: {}", e.getMessage());
        }

        log.info("═══ Ciclo de backup [{}] concluído: status={}, duração={}s ═══",
                execution.getJobId(), execution.getStatus(), execution.getDuration());
        return execution;
    }

    private DatabaseBackupResult runIsolated(DatabaseTarget target, ExecutionMode mode) {
        try {
            return dumperFor(target).dump(target, mode);
        } catch (Exception e) {
            // Blindagem extra: o dumper já converte falhas em FAILED, mas um erro
            // inesperado (ex.: alvo não suportado) não pode derrubar o ciclo.
            log.error("Erro inesperado no dump de {}: {}", target, e.getMessage(), e);
            return DatabaseBackupResult.failure(target, kindOf(target), "Erro inesperado: " + e.getMessage());
        }
    }

    private DatabaseDumper dumperFor(DatabaseTarget target) {
        return switch (target) {
            case IAM_DB, PATIENT_CLINICAL_DB, REPORTING_DB -> postgresDumper;
            case MONGO_AI_DB -> mongoDumper;
            case MINIO_BUCKETS -> throw new IllegalArgumentException(
                    "MINIO_BUCKETS não é um alvo de dump de banco de dados");
        };
    }

    private static BackupKind kindOf(DatabaseTarget target) {
        return target == DatabaseTarget.MONGO_AI_DB ? BackupKind.MONGO : BackupKind.POSTGRES;
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private BackupManifest buildManifest(BackupJobExecution execution) {
        Instant now = Instant.now();
        return new BackupManifest(
                execution.getJobId(),
                now,
                BackupManifest.CURRENT_SCHEMA_VERSION,
                execution.getResults(),
                cryptoService.transformationLabel(),
                execution.getStatus() == BackupStatus.SUCCESS,
                now.plus(backupProperties.retentionDays(), ChronoUnit.DAYS));
    }
}
