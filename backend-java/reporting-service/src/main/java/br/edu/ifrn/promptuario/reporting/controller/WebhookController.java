package br.edu.ifrn.promptuario.reporting.controller;

import br.edu.ifrn.promptuario.reporting.dto.WebhookCreateDTO;
import br.edu.ifrn.promptuario.reporting.dto.WebhookResponseDTO;
import br.edu.ifrn.promptuario.reporting.model.WebhookConfig;
import br.edu.ifrn.promptuario.reporting.repository.WebhookConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/webhooks")
@Tag(name = "Webhooks")
public class WebhookController {

    private final WebhookConfigRepository webhookConfigRepository;

    public WebhookController(WebhookConfigRepository webhookConfigRepository) {
        this.webhookConfigRepository = webhookConfigRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista webhooks de notificação cadastrados")
    @Transactional(readOnly = true)
    public List<WebhookResponseDTO> list() {
        return webhookConfigRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra novo endpoint webhook")
    @Transactional
    public ResponseEntity<WebhookResponseDTO> create(
            @Valid @RequestBody WebhookCreateDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        WebhookConfig config = new WebhookConfig(
                request.url(),
                request.secret(),
                userId,
                request.description(),
                request.events());
        WebhookConfig saved = webhookConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private WebhookResponseDTO toResponse(WebhookConfig config) {
        return new WebhookResponseDTO(
                config.getId(),
                config.getUrl(),
                config.getDescription(),
                config.isActive(),
                config.getConsecutiveFailures(),
                config.getLastSuccessAt());
    }
}