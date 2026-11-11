package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Log de auditoria das restaurações e drills de contingência.
 *
 * <p>Registra cada evento em duas trilhas: um logger dedicado
 * ({@code br.edu.ifrn.promptuario.backup.AUDIT.RESTORE}, para agregadores de log)
 * e um arquivo persistente em {@code {rootDir}/restore-audit.log} no formato
 * JSON Lines (uma linha por evento), adequado a auditoria/compliance.
 */
@Service
public class RestoreAuditService {

    /** Nome do arquivo de auditoria dentro de {@code BACKUP_ROOT_DIR}. */
    public static final String AUDIT_FILE_NAME = "restore-audit.log";

    private static final Logger log = LoggerFactory.getLogger(RestoreAuditService.class);
    private static final Logger auditLogger =
            LoggerFactory.getLogger("br.edu.ifrn.promptuario.backup.AUDIT.RESTORE");

    private final BackupProperties backupProperties;
    private final ObjectMapper objectMapper;

    public RestoreAuditService(BackupProperties backupProperties, ObjectMapper objectMapper) {
        this.backupProperties = backupProperties;
        this.objectMapper = objectMapper;
    }

    /** Grava um evento de auditoria (nunca lança: auditoria não pode derrubar a restauração). */
    public void record(RestoreAuditEntry entry) {
        String json;
        try {
            json = objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            log.warn("Falha ao serializar o registro de auditoria: {}", e.getMessage());
            return;
        }
        auditLogger.info(json);
        try {
            Path file = auditFile();
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, json + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Não foi possível gravar o log de auditoria em {}: {}", auditFile(), e.getMessage());
        }
    }

    /** Caminho do arquivo de auditoria. */
    public Path auditFile() {
        return backupProperties.rootDir().resolve(AUDIT_FILE_NAME);
    }

    /** Lê os eventos de auditoria registrados, na ordem de gravação. */
    public List<RestoreAuditEntry> readEntries() throws IOException {
        Path file = auditFile();
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .map(this::parse)
                .toList();
    }

    private RestoreAuditEntry parse(String line) {
        try {
            return objectMapper.readValue(line, RestoreAuditEntry.class);
        } catch (Exception e) {
            throw new IllegalStateException("Linha de auditoria inválida: " + line, e);
        }
    }

    /**
     * Registro de auditoria de uma operação de restauração/drill.
     *
     * @param event          tipo do evento (ex.: {@code RESTORE}, {@code DRILL})
     * @param target         alvo lógico ({@link br.edu.ifrn.promptuario.backup.model.DatabaseTarget})
     * @param artifact       artefato utilizado (caminho local ou chave MinIO)
     * @param expectedSha256 hash validado (pode ser {@code null} se não informado)
     * @param success        resultado da operação
     * @param durationMillis tempo total (RTO observado)
     * @param message        detalhe/erro (já sem credenciais)
     * @param timestamp      instante do evento
     */
    public record RestoreAuditEntry(
            String event,
            String target,
            String artifact,
            String expectedSha256,
            boolean success,
            long durationMillis,
            String message,
            Instant timestamp) {

        public static RestoreAuditEntry of(
                String event,
                String target,
                String artifact,
                String expectedSha256,
                boolean success,
                long durationMillis,
                String message) {
            return new RestoreAuditEntry(
                    event, target, artifact, expectedSha256, success, durationMillis, message, Instant.now());
        }
    }
}
