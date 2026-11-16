package br.edu.ifrn.promptuario.reporting.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardSummaryDTO(
        long totalConsultationsToday,
        long totalNewPatientsToday,
        long totalCancellationsToday,
        long totalPrescriptionsToday,
        Map<String, Object> metricsWindow) {

    public DashboardSummaryDTO {
        metricsWindow = metricsWindow == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metricsWindow));
    }
}