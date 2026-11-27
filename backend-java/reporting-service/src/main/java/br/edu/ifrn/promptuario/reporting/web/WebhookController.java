package br.edu.ifrn.promptuario.reporting.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public WebhookController(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody WebhookRequest request,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO webhook_configs
                    (id, url, secret, description, active, max_retries, retry_interval_seconds, events, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """, id, request.url(), request.secret(), request.description(), request.active(),
                request.maxRetries(), request.retryIntervalSeconds(), json(request.events()), userId);
        return find(id);
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> items = jdbc.queryForList("SELECT * FROM webhook_configs ORDER BY created_at DESC")
                .stream().map(this::normalize).toList();
        return Map.of("items", items, "total", items.size());
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id) { return find(id); }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @Valid @RequestBody WebhookRequest request) {
        int changed = jdbc.update("""
                UPDATE webhook_configs SET url = ?, secret = ?, description = ?, active = ?, max_retries = ?,
                    retry_interval_seconds = ?, events = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.url(), request.secret(), request.description(), request.active(), request.maxRetries(),
                request.retryIntervalSeconds(), json(request.events()), id);
        if (changed == 0) throw new IllegalArgumentException("Webhook not found: " + id);
        return find(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (jdbc.update("DELETE FROM webhook_configs WHERE id = ?", id) == 0) {
            throw new IllegalArgumentException("Webhook not found: " + id);
        }
    }

    @GetMapping("/{id}/deliveries")
    public Map<String, Object> deliveries(@PathVariable String id) {
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT * FROM webhook_delivery_logs WHERE webhook_config_id = ? ORDER BY delivered_at DESC LIMIT 100
                """, id);
        return Map.of("items", items, "total", items.size());
    }

    private Map<String, Object> find(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM webhook_configs WHERE id = ?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Webhook not found: " + id);
        return normalize(rows.getFirst());
    }

    private Map<String, Object> normalize(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.remove("secret");
        row.put("events", parse(row.get("events")));
        return row;
    }

    private Object parse(Object value) {
        if (value == null) return List.of();
        try { return mapper.readValue(value.toString(), Object.class); }
        catch (JsonProcessingException ignored) { return value; }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value == null ? List.of("report.completed") : value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid webhook JSON", exception); }
    }

    public record WebhookRequest(
            @NotBlank String url,
            @NotBlank @Size(min = 16, max = 128) String secret,
            @Size(max = 255) String description,
            Boolean active,
            Integer maxRetries,
            Integer retryIntervalSeconds,
            List<String> events) {
        public WebhookRequest {
            description = description == null ? "" : description;
            active = active == null || active;
            maxRetries = maxRetries == null ? 3 : maxRetries;
            retryIntervalSeconds = retryIntervalSeconds == null ? 60 : retryIntervalSeconds;
            events = events == null ? List.of("report.completed") : events;
        }
    }
}
