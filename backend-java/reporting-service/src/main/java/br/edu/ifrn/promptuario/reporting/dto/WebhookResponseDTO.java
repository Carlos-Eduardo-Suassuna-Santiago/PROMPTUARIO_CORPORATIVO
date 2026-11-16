package br.edu.ifrn.promptuario.reporting.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookResponseDTO(
        String id,
        String url,
        String description,
        boolean active,
        int consecutiveFailures,
        Instant lastSuccessAt) {
}