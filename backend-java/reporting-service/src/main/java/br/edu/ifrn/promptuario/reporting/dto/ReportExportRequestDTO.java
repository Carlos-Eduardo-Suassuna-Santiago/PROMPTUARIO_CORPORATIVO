package br.edu.ifrn.promptuario.reporting.dto;

import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReportExportRequestDTO(
        @NotNull(message = "Tipo de relatório é obrigatório") ReportType reportType,
        @NotNull(message = "Formato de saída é obrigatório") OutputFormat outputFormat,
        Map<String, Object> parameters) {

    public ReportExportRequestDTO {
        parameters = parameters == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }
}