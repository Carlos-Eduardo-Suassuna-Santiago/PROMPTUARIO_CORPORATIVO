package br.edu.ifrn.promptuario.reporting.dto;

import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScheduleResponseDTO(
        String id,
        String name,
        ReportType reportType,
        OutputFormat outputFormat,
        String cronExpression,
        boolean active,
        Instant lastRunAt,
        Instant nextRunAt) {
}