package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.BackupJobExecution;
import br.edu.ifrn.promptuario.backup.model.BackupManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.crypto.CipherOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Persistência dos relatórios do ciclo de backup.
 *
 * <ul>
 *   <li>{@code /var/backups/status.json} — relatório operacional em texto claro,
 *       consumido por monitoramento e pelo console do serviço.</li>
 *   <li>{@code /var/backups/manifest.json} — manifesto criptográfico
 *       (AES-256-GCM com IV prefixado), usado para auditoria e restauração.</li>
 * </ul>
 */
@Service
public class StatusReportService {

    /** Nome do relatório operacional persistente. */
    public static final String STATUS_FILE_NAME = "status.json";

    /** Nome do manifesto criptográfico do ciclo. */
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    private static final Logger log = LoggerFactory.getLogger(StatusReportService.class);

    private final BackupProperties backupProperties;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public StatusReportService(
            BackupProperties backupProperties,
            CryptoService cryptoService,
            ObjectMapper objectMapper) {
        this.backupProperties = backupProperties;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    /** Grava/atualiza o {@code status.json} com o estado consolidado do ciclo. */
    public Path writeStatusReport(BackupJobExecution execution) throws IOException {
        Path target = backupProperties.rootDir().resolve(STATUS_FILE_NAME);
        Files.createDirectories(target.getParent());

        byte[] payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(execution);
        Files.write(target, payload);

        log.info("Status do ciclo salvo em {} ({} linhas, status={})",
                target, payload.length, execution.getStatus());
        return target;
    }

    /**
     * Grava o manifesto criptográfico do ciclo. Quando a criptografia está
     * habilitada, o arquivo é gravado no formato
     * {@code [IV(12 bytes)][ciphertext + tag GCM]}; caso contrário é JSON puro.
     */
    public Path writeManifest(BackupManifest manifest) throws IOException {
        Path target = backupProperties.rootDir().resolve(MANIFEST_FILE_NAME);
        Files.createDirectories(target.getParent());

        byte[] payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);

        if (cryptoService.isEnabled()) {
            try (OutputStream fileOut = Files.newOutputStream(target);
                 CipherOutputStream cipherOut =
                         cryptoService.createEncryptingStream(fileOut, cryptoService.activeKey())) {
                cipherOut.write(payload);
            }
        } else {
            Files.write(target, payload);
        }

        log.info("Manifesto ({}) gravado em {} com {} bytes",
                cryptoService.transformationLabel(), target, Files.size(target));
        return target;
    }
}
