package br.edu.ifrn.promptuario.backup.controller;

import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.service.RestoreOrchestratorService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de restauração controlada (Disaster Recovery).
 *
 * <p>Endpoint:
 * <ul>
 *   <li>{@code POST /api/v1/backups/restore} — inicia restore controlado de um
 *       artefato informado no corpo da requisição.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/backups")
public class RestoreOperationController {

    private static final Logger log = LoggerFactory.getLogger(RestoreOperationController.class);

    @Autowired(required = false)
    private RestoreOrchestratorService restoreOrchestratorService;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * Requisição de restauração controlada.
     */
    public record RestoreRequest(
            String kind,
            String target,
            String file,
            String sha256
    ) {}

    /**
     * Inicia uma restauração controlada do artefato especificado.
     *
     * <p>O corpo da requisição deve conter:
     * <ul>
     *   <li>{@code kind}: tipo do banco ({@code postgres} ou {@code mongo})</li>
     *   <li>{@code target}: nome ou enum do alvo</li>
     *   <li>{@code file}: caminho do artefato ou chave no MinIO</li>
     *   <li>{@code sha256}: (opcional) SHA-256 esperado para validação de integridade</li>
     * </ul>
     */
    @PostMapping(value = "/restore", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> restore(@RequestBody RestoreRequest request) {
        if (restoreOrchestratorService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "RestoreOrchestratorService não disponível"));
        }

        if (request.kind == null || request.target == null || request.file == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Campos obrigatórios: kind, target, file"));
        }

        BackupKind backupKind;
        try {
            backupKind = BackupKind.valueOf(request.kind.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Tipo de banco inválido: " + request.kind));
        }

        try {
            log.info("Restauração via API REST: kind={}, target={}, file={}",
                    backupKind, request.target, request.file);
            RestoreOrchestratorService.RestoreOutcome outcome =
                    restoreOrchestratorService.restoreDatabase(
                            request.target, request.file, request.sha256);

            if (outcome.success()) {
                if (meterRegistry != null) {
                    meterRegistry.counter("backup_api_restore_total", "status", "success").increment();
                }
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "target", outcome.targetName(),
                    "artifact", outcome.artifact(),
                    "durationMillis", outcome.durationMillis(),
                    "message", outcome.message()
                ));
            } else {
                if (meterRegistry != null) {
                    meterRegistry.counter("backup_api_restore_total", "status", "failed").increment();
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "failed",
                        "target", request.target,
                        "message", outcome.message()
                    ));
            }
        } catch (Exception e) {
            log.error("Erro inesperado na restauração via API: {}", e.getMessage(), e);
            if (meterRegistry != null) {
                meterRegistry.counter("backup_api_restore_total", "status", "error").increment();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro inesperado: " + e.getMessage()));
        }
    }
}
