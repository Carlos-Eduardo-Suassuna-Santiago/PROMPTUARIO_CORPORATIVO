package br.edu.ifrn.promptuario.reporting.worker.exporters;

import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CsvReportExporter implements ReportExporter {

    private static final String UTF8_BOM = "\uFEFF";

    @Override
    public byte[] export(List<Map<String, Object>> data, String reportTitle, Map<String, Object> metadata) {
        List<Map<String, Object>> rows = ExporterSupport.safeData(data);
        List<String> columns = ExporterSupport.columns(rows);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             CSVWriter csv = new CSVWriter(writer)) {
            writer.write(UTF8_BOM);
            if (!columns.isEmpty()) {
                csv.writeNext(columns.toArray(String[]::new), false);
                for (Map<String, Object> row : rows) {
                    String[] values = new String[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        values[i] = ExporterSupport.asText(ExporterSupport.value(row, columns.get(i)));
                    }
                    csv.writeNext(values, false);
                }
            }
            csv.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o relatório CSV", exception);
        }
    }

    @Override
    public String getContentType() {
        return "text/csv; charset=UTF-8";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }
}