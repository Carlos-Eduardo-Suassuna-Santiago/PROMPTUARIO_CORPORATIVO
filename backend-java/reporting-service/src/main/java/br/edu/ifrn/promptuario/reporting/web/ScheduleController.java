package br.edu.ifrn.promptuario.reporting.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ScheduleController(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody ScheduleRequest request,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        validateCron(request.cronExpression());
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO report_schedules
                    (id, name, report_type, output_format, cron_expression, parameters, recipients, active, created_by)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """, id, request.name(), request.reportType(), request.outputFormat(), request.cronExpression(),
                json(request.parameters()), json(request.recipients()), request.active(), userId);
        return find(id);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT * FROM report_schedules ORDER BY created_at DESC OFFSET ? LIMIT ?
                """, (safePage - 1) * safeSize, safeSize).stream().map(this::normalize).toList();
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM report_schedules", Integer.class);
        return Map.of("items", items, "total", total == null ? 0 : total);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id) { return find(id); }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @Valid @RequestBody ScheduleRequest request) {
        validateCron(request.cronExpression());
        int changed = jdbc.update("""
                UPDATE report_schedules SET name = ?, report_type = ?, output_format = ?, cron_expression = ?,
                    parameters = ?::jsonb, recipients = ?::jsonb, active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, request.name(), request.reportType(), request.outputFormat(), request.cronExpression(),
                json(request.parameters()), json(request.recipients()), request.active(), id);
        if (changed == 0) throw new IllegalArgumentException("Schedule not found: " + id);
        return find(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (jdbc.update("DELETE FROM report_schedules WHERE id = ?", id) == 0) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
    }

    private Map<String, Object> find(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM report_schedules WHERE id = ?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Schedule not found: " + id);
        return normalize(rows.getFirst());
    }

    private Map<String, Object> normalize(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.put("parameters", parse(row.get("parameters")));
        row.put("recipients", parse(row.get("recipients")));
        return row;
    }

    private Object parse(Object value) {
        if (value == null) return Map.of();
        try { return mapper.readValue(value.toString(), Object.class); }
        catch (JsonProcessingException ignored) { return value; }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid schedule JSON", exception); }
    }

    private void validateCron(String cron) {
        if (cron == null || cron.trim().split("\\s+").length != 5) {
            throw new IllegalArgumentException("cron_expression must contain five fields");
        }
    }

    public record ScheduleRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String reportType,
            @NotBlank String outputFormat,
            @NotBlank String cronExpression,
            Map<String, Object> parameters,
            List<String> recipients,
            Boolean active) {
        public ScheduleRequest {
            parameters = parameters == null ? Map.of() : parameters;
            recipients = recipients == null ? List.of() : recipients;
            active = active == null || active;
        }
    }
}
