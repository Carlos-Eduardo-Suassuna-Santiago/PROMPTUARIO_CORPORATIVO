package br.edu.ifrn.promptuario.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportService {
    private static final List<String> REPORT_TYPES = List.of(
            "CONSULTATIONS", "PATIENTS", "DOCTORS", "PRESCRIPTIONS", "CUSTOM", "FULL_SYSTEM");
    private static final List<String> OUTPUT_FORMATS = List.of("JSON", "CSV", "PDF", "XLSX");

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final long workerDelayMs;

    public ReportService(JdbcTemplate jdbc, ObjectMapper mapper,
                         @Value("${app.worker-delay-ms:0}") long workerDelayMs) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.workerDelayMs = workerDelayMs;
    }

    @Transactional
    public Map<String, Object> request(String type, String format, Map<String, Object> parameters,
                                       String requestedBy) {
        validateTypeAndFormat(type, format);
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO report_jobs
                    (id, report_type, requested_by, parameters, status, output_format, row_count)
                VALUES (?, ?, ?, ?::jsonb, 'PENDING', ?, 0)
                """, id, type, requestedBy, json(parameters), format);
        jdbc.update("""
                INSERT INTO report_audit_logs
                    (id, event_type, entity_type, entity_id, description, performed_by, event_metadata)
                VALUES (?, 'REPORT_REQUESTED', 'report_job', ?, ?, ?, ?::jsonb)
                """, UUID.randomUUID().toString(), id,
                "Report requested: " + type + " (" + format + ")", requestedBy,
                json(Map.of("report_type", type, "output_format", format)));
        processAsync(id);
        return Map.of("job_id", id, "status", "PENDING");
    }

    @Async("reportingExecutor")
    public CompletableFuture<Void> processAsync(String id) {
        try {
            if (workerDelayMs > 0) Thread.sleep(workerDelayMs);
            jdbc.update("UPDATE report_jobs SET status = 'RUNNING' WHERE id = ? AND status = 'PENDING'", id);
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT stat_date, stat_type, entity_id, value, metadata
                    FROM daily_stats
                    WHERE stat_type IN ('CONSULTATIONS', 'NEW_PATIENTS', 'DOCTOR_CONSULTATIONS', 'PRESCRIPTIONS')
                    ORDER BY stat_date DESC
                    LIMIT 500
                    """);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("data", rows);
            result.put("generated_at", OffsetDateTime.now().toString());
            jdbc.update("""
                    UPDATE report_jobs
                    SET status = 'COMPLETED', result_data = ?::jsonb, row_count = ?, completed_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, json(result), rows.size(), id);
            jdbc.update("""
                    INSERT INTO report_audit_logs
                        (id, event_type, entity_type, entity_id, description, performed_by, event_metadata)
                    SELECT ?, 'REPORT_COMPLETED', 'report_job', id, 'Report completed', requested_by, '{}'::jsonb
                    FROM report_jobs WHERE id = ?
                    """, UUID.randomUUID().toString(), id);
        } catch (Exception ex) {
            jdbc.update("UPDATE report_jobs SET status = 'FAILED', error_message = ?, completed_at = CURRENT_TIMESTAMP WHERE id = ?",
                    safeMessage(ex), id);
        }
        return CompletableFuture.completedFuture(null);
    }

    public Map<String, Object> find(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, report_type, status, output_format, row_count, s3_key,
                       result_data, error_message, created_at, completed_at
                FROM report_jobs WHERE id = ?
                """, id);
        if (rows.isEmpty()) throw new ReportNotFoundException(id);
        Map<String, Object> row = new LinkedHashMap<>(rows.getFirst());
        row.put("result_data", parseJson(row.get("result_data")));
        return row;
    }

    public List<Map<String, Object>> dailyStats(String type, String from, String to, String entityId) {
        StringBuilder sql = new StringBuilder("SELECT stat_date, stat_type, entity_id, value, metadata FROM daily_stats WHERE stat_type = ?");
        List<Object> args = new ArrayList<>();
        args.add(type);
        if (from != null && !from.isBlank()) { sql.append(" AND stat_date >= ?"); args.add(from); }
        if (to != null && !to.isBlank()) { sql.append(" AND stat_date <= ?"); args.add(to); }
        if (entityId != null && !entityId.isBlank()) { sql.append(" AND entity_id = ?"); args.add(entityId); }
        sql.append(" ORDER BY stat_date DESC LIMIT 200");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public int sumStats(String type, String from) {
        if (from == null) {
            Integer value = jdbc.queryForObject("SELECT COALESCE(SUM(value), 0) FROM daily_stats WHERE stat_type = ? AND stat_date = CURRENT_DATE::text", Integer.class, type);
            return value == null ? 0 : value;
        }
        Integer value = jdbc.queryForObject("SELECT COALESCE(SUM(value), 0) FROM daily_stats WHERE stat_type = ? AND stat_date >= ?", Integer.class, type, from);
        return value == null ? 0 : value;
    }

    private void validateTypeAndFormat(String type, String format) {
        if (!REPORT_TYPES.contains(type)) throw new IllegalArgumentException("Unsupported report_type: " + type);
        if (!OUTPUT_FORMATS.contains(format)) throw new IllegalArgumentException("Unsupported output_format: " + format);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid JSON parameters", e); }
    }

    private Object parseJson(Object value) {
        if (value == null) return null;
        try { return mapper.readValue(value.toString(), Object.class); }
        catch (JsonProcessingException ignored) { return value; }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
