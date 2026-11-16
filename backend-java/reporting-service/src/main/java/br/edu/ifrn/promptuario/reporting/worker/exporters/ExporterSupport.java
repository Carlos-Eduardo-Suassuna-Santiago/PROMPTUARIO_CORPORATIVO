package br.edu.ifrn.promptuario.reporting.worker.exporters;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ExporterSupport {

    static final int MAX_ROWS = 100_000;
    static final int MAX_COLUMNS = 200;
    static final long MAX_CELLS = 1_000_000;

    private ExporterSupport() {
    }

    static List<Map<String, Object>> safeData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        if (data.size() > MAX_ROWS) {
            throw new IllegalArgumentException("O relatório excede o limite de " + MAX_ROWS + " linhas");
        }
        return data;
    }

    static List<String> columns(List<Map<String, Object>> data) {
        if (data.isEmpty() || data.get(0) == null || data.get(0).isEmpty()) {
            return List.of();
        }
        if (data.get(0).size() > MAX_COLUMNS) {
            throw new IllegalArgumentException("O relatório excede o limite de " + MAX_COLUMNS + " colunas");
        }
        if ((long) data.size() * data.get(0).size() > MAX_CELLS) {
            throw new IllegalArgumentException("O relatório excede o limite de " + MAX_CELLS + " células");
        }
        return new ArrayList<>(data.get(0).keySet());
    }

    static Object value(Map<String, Object> row, String column) {
        return row == null ? null : row.get(column);
    }

    static String asText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof TemporalAccessor || value instanceof Number || value instanceof Boolean
                || value instanceof CharSequence || value instanceof java.util.Date) {
            return value.toString();
        }
        return value.toString();
    }

    static Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    static boolean isFullSystem(String title, Map<String, Object> metadata) {
        Map<String, Object> safe = safeMetadata(metadata);
        Object reportType = safe.getOrDefault("report_type", safe.get("reportType"));
        return "FULL_SYSTEM".equalsIgnoreCase(String.valueOf(reportType))
                || (title != null && (title.toUpperCase(java.util.Locale.ROOT).contains("FULL_SYSTEM")
                || title.toLowerCase(java.util.Locale.ROOT).contains("completo")));
    }

}