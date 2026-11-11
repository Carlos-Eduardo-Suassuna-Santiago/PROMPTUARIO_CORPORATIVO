package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Política de retenção: expurga artefatos locais e objetos no MinIO cuja data
 * seja anterior ao cutoff ({@code agora - retention-days}).
 *
 * <p>Os arquivos de metadados operacionais ({@code status.json} e
 * {@code manifest.json}) são protegidos e nunca expurgados: eles são o insumo
 * de monitoramento do serviço, não artefatos de dump.
 */
@Service
public class RetentionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupService.class);

    /** Arquivos de metadados que jamais devem ser removidos pela retenção. */
    private static final Set<String> PROTECTED_FILES = Set.of(
            StatusReportService.STATUS_FILE_NAME,
            StatusReportService.MANIFEST_FILE_NAME);

    private final BackupProperties backupProperties;
    private final S3BackupStorageService storageService;

    public RetentionCleanupService(BackupProperties backupProperties, S3BackupStorageService storageService) {
        this.backupProperties = backupProperties;
        this.storageService = storageService;
    }

    /**
     * Executa a limpeza local e remota.
     *
     * @return total de artefatos removidos (local + MinIO)
     */
    public int cleanup() {
        Instant cutoff = Instant.now().minus(backupProperties.retentionDays(), ChronoUnit.DAYS);
        log.info("Iniciando limpeza de retenção (retenção de {} dias, cutoff={})",
                backupProperties.retentionDays(), cutoff);

        int localRemoved = 0;
        int remoteRemoved = 0;

        try {
            localRemoved = cleanupLocal(cutoff);
        } catch (IOException e) {
            log.warn("Falha na limpeza local de retenção: {}", e.getMessage());
        }
        try {
            remoteRemoved = storageService.deleteObjectsOlderThan(cutoff);
        } catch (RuntimeException e) {
            log.warn("Falha na limpeza de retenção no MinIO: {}", e.getMessage());
        }

        int total = localRemoved + remoteRemoved;
        log.info("Limpeza de retenção concluída: {} artefato(s) local(is) e {} objeto(s) no MinIO",
                localRemoved, remoteRemoved);
        return total;
    }

    private int cleanupLocal(Instant cutoff) throws IOException {
        Path root = backupProperties.rootDir();
        if (!Files.isDirectory(root)) {
            log.debug("Diretório de backups '{}' inexistente; nada a limpar.", root);
            return 0;
        }

        List<Path> expired;
        try (Stream<Path> paths = Files.walk(root)) {
            expired = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !PROTECTED_FILES.contains(path.getFileName().toString()))
                    .filter(path -> isOlderThan(path, cutoff))
                    .toList();
        }

        int removed = 0;
        for (Path path : expired) {
            try {
                if (Files.deleteIfExists(path)) {
                    removed++;
                }
            } catch (IOException e) {
                log.warn("Não foi possível remover o artefato expirado {}: {}", path, e.getMessage());
            }
        }
        return removed;
    }

    private boolean isOlderThan(Path path, Instant cutoff) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            return lastModified.toInstant().isBefore(cutoff);
        } catch (IOException e) {
            log.warn("Não foi possível ler a data de modificação de {}: {}", path, e.getMessage());
            return false;
        }
    }
}
