package br.edu.ifrn.promptuario.reporting.dto;

import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScheduleCreateDTO(
        @NotBlank @Size(max = 120) String name,
        @NotNull ReportType reportType,
        @NotNull OutputFormat outputFormat,
        @NotBlank @Size(max = 100) String cronExpression,
        Map<String, Object> parameters,
        List<String> recipients) {

    public ScheduleCreateDTO {
        parameters = parameters == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        recipients = recipients == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(recipients));
    }
}