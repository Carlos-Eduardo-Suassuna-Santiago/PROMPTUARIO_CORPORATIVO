package br.edu.ifrn.promptuario.reporting.worker.exporters;

import java.util.List;
import java.util.Map;

public interface ReportExporter {

    byte[] export(List<Map<String, Object>> data, String reportTitle, Map<String, Object> metadata);

    String getContentType();

    String getFileExtension();
}