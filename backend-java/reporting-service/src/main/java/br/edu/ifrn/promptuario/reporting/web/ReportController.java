package br.edu.ifrn.promptuario.reporting.web;

import br.edu.ifrn.promptuario.reporting.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> export(@Valid @RequestBody ExportRequest request,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return service.request(request.reportType(), request.outputFormat(), request.parameters(), userId);
    }

    @PostMapping("/export/custom")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> custom(@Valid @RequestBody CustomExportRequest request,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        Map<String, Object> parameters = new java.util.LinkedHashMap<>(request.parameters());
        parameters.put("sql_query_name", request.sqlQueryName());
        Map<String, Object> response = new java.util.LinkedHashMap<>(service.request("CUSTOM", request.outputFormat(), parameters, userId));
        response.put("query_template", request.sqlQueryName());
        return response;
    }

    @PostMapping("/export/multi-sheet")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> multiSheet(@Valid @RequestBody MultiSheetRequest request,
                                           @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return service.request("CUSTOM", "XLSX", Map.of("filename", request.filename(), "sheets", request.sheets()), userId);
    }

    @GetMapping("/export/{id}")
    public Map<String, Object> get(@PathVariable String id) { return service.find(id); }

    @GetMapping("/export/{id}/download")
    public Map<String, Object> download(@PathVariable String id) {
        Map<String, Object> job = service.find(id);
        if (!"COMPLETED".equals(job.get("status"))) throw new IllegalStateException("Report not completed: " + job.get("status"));
        return Map.of("result", job.getOrDefault("result_data", Map.of()), "format", job.get("output_format"));
    }

    @GetMapping("/consultations")
    public Map<String, Object> consultations(@RequestParam(name = "from_date", required = false) String fromDate,
                                             @RequestParam(name = "to_date", required = false) String toDate) {
        List<Map<String, Object>> data = service.dailyStats("CONSULTATIONS", fromDate, toDate, null);
        return Map.of("data", data, "total_days", data.size());
    }

    @GetMapping("/patients")
    public Map<String, Object> patients(@RequestParam(name = "from_date", required = false) String fromDate,
                                        @RequestParam(name = "to_date", required = false) String toDate) {
        List<Map<String, Object>> data = service.dailyStats("NEW_PATIENTS", fromDate, toDate, null);
        int total = data.stream().mapToInt(row -> ((Number) row.getOrDefault("value", 0)).intValue()).sum();
        return Map.of("data", data, "total_new_patients", total);
    }

    @GetMapping("/doctors")
    public Map<String, Object> doctors(@RequestParam(name = "doctor_id", required = false) String doctorId,
                                       @RequestParam(name = "from_date", required = false) String fromDate,
                                       @RequestParam(name = "to_date", required = false) String toDate) {
        return Map.of("data", service.dailyStats("DOCTOR_CONSULTATIONS", fromDate, toDate, doctorId));
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Integer consultations = service.sumStats("CONSULTATIONS", null);
        Integer newPatients = service.sumStats("NEW_PATIENTS", java.time.LocalDate.now().withDayOfMonth(1).toString());
        Integer cancellations = service.sumStats("CANCELLATIONS", java.time.LocalDate.now().toString());
        return Map.of("consultations_today", consultations,
                "new_patients_this_month", newPatients,
                "cancellations_today", cancellations,
                "as_of", java.time.OffsetDateTime.now().toString());
    }

    public record ExportRequest(@NotBlank String reportType, @NotBlank String outputFormat, Map<String, Object> parameters) {
        public ExportRequest { parameters = parameters == null ? Map.of() : parameters; }
    }

    public record CustomExportRequest(@NotBlank @Size(max = 64) String sqlQueryName,
                                      @NotBlank String outputFormat, Map<String, Object> parameters) {
        public CustomExportRequest { parameters = parameters == null ? Map.of() : parameters; }
    }

    public record MultiSheetRequest(@NotEmpty @Size(max = 20) List<Map<String, Object>> sheets,
                                    @Size(max = 120) String filename) {
        public MultiSheetRequest { filename = filename == null ? "multi_sheet_report.xlsx" : filename; }
    }
}
