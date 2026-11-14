package br.edu.ifrn.promptuario.backup.controller;

import br.edu.ifrn.promptuario.backup.model.ExecutionMode;
import br.edu.ifrn.promptuario.backup.service.BackupOrchestratorService;
import br.edu.ifrn.promptuario.backup.service.StatusReportService;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador operacional de backups.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /healthz} — status do serviço.</li>
 *   <li>{@code GET /api/v1/backups/status} — último status consolidado de
 *       {@code /var/backups/status.json}.</li>
 *   <li>{@code POST /api/v1/backups/execute} — dispara backup assíncrono sob demanda.</li>
 * </ul>
 */
@RestController
@RequestMapping({"", "/api/v1/backups"})
public class BackupOperationController {

    private static final Logger log = LoggerFactory.getLogger(BackupOperationController.class);

    private static final Path STATUS_FILE = java.nio.file.Paths.get("/var/backups/status.json");

    @Autowired(required = false)
    private BackupOrchestratorService backupOrchestratorService;

    @Autowired(required = false)
    private StatusReportService statusReportService;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * Health check público do serviço.
     */
    @GetMapping(value = "/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> healthz() {
        return Map.of(
            "status", "ok",
            "service", "backup-service",
            "version", "1.0.0-java"
        );
    }

    /**
     * Retorna o último status consolidado do backup, lido do
     * {@code /var/backups/status.json}.
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> status() {
        try {
            if (Files.exists(STATUS_FILE)) {
                byte[] content = Files.readAllBytes(STATUS_FILE);
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new String(content));
            }
        } catch (Exception e) {
            log.warn("Não foi possível ler o status em {}: {}", STATUS_FILE, e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Nenhum status de backup disponível"));
    }

    /**
     * Dispara um backup completo sob demanda, de forma assíncrona.
     * Retorna imediatamente com 202 Aceito enquanto a execução ocorre em background.
     */
    @PostMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> execute() {
        if (backupOrchestratorService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "BackupOrchestratorService não disponível"));
        }

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Backup sob demanda iniciado via API REST.");
                backupOrchestratorService.executeFullBackup(ExecutionMode.FULL);
                log.info("Backup sob demanda concluído.");
                if (meterRegistry != null) {
                    meterRegistry.counter("backup_api_execute_total", "status", "success").increment();
                }
            } catch (Exception e) {
                log.error("Falha no backup sob demanda: {}", e.getMessage(), e);
                if (meterRegistry != null) {
                    meterRegistry.counter("backup_api_execute_total", "status", "failed").increment();
                }
            }
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("message", "Backup sob demanda iniciado em background"));
    }
}
