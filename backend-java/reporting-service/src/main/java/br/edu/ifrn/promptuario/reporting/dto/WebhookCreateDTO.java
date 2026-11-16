package br.edu.ifrn.promptuario.reporting.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookCreateDTO(
        @NotBlank @URL @Size(max = 500) String url,
        @NotBlank @Size(min = 16, max = 128) String secret,
        @Size(max = 255) String description,
        List<String> events) {

    public WebhookCreateDTO {
        events = events == null
                ? List.of("report.completed")
                : Collections.unmodifiableList(new ArrayList<>(events));
    }
}