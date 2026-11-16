package br.edu.ifrn.promptuario.reporting.dto;

import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesDtoPropertiesUsingSnakeCase() throws Exception {
        ReportExportRequestDTO request = new ReportExportRequestDTO(
                ReportType.CONSULTATIONS,
                OutputFormat.CSV,
                Map.of("from_date", "2026-01-01"));
        DashboardSummaryDTO summary = new DashboardSummaryDTO(12, 3, 1, 7, Map.of());

        JsonNode requestJson = objectMapper.readTree(objectMapper.writeValueAsString(request));
        JsonNode summaryJson = objectMapper.readTree(objectMapper.writeValueAsString(summary));

        assertEquals("CONSULTATIONS", requestJson.get("report_type").asText());
        assertEquals("CSV", requestJson.get("output_format").asText());
        assertTrue(requestJson.has("parameters"));
        assertEquals(12, summaryJson.get("total_consultations_today").asLong());
        assertEquals(3, summaryJson.get("total_new_patients_today").asLong());
        assertTrue(summaryJson.has("metrics_window"));
        assertTrue(!summaryJson.has("totalConsultationsToday"));
    }
}
