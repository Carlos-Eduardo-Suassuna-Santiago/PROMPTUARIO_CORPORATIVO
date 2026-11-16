package br.edu.ifrn.promptuario.reporting.worker.exporters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonReportExporter implements ReportExporter {

    private final ObjectMapper objectMapper;

    public JsonReportExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] export(List<Map<String, Object>> data, String reportTitle, Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsBytes(ExporterSupport.safeData(data));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível gerar o relatório JSON", exception);
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}