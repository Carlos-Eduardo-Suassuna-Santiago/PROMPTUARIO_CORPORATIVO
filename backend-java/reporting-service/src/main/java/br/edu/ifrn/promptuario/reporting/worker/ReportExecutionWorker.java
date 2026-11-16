package br.edu.ifrn.promptuario.reporting.worker;

import br.edu.ifrn.promptuario.reporting.model.DailyStats;
import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportStatus;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import br.edu.ifrn.promptuario.reporting.repository.DailyStatsRepository;
import br.edu.ifrn.promptuario.reporting.repository.ReportJobRepository;
import br.edu.ifrn.promptuario.reporting.service.WebhookDeliveryService;
import br.edu.ifrn.promptuario.reporting.storage.S3ReportStorageService;
import br.edu.ifrn.promptuario.reporting.worker.exporters.ReportExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportExecutionWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportExecutionWorker.class);
    private static final int MAX_REPORT_ROWS = 100_000;
    private static final int MAX_QUERY_ROWS = MAX_REPORT_ROWS + 1;

    private final ReportJobRepository reportJobRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final S3ReportStorageService storageService;
    private final WebhookDeliveryService webhookDeliveryService;
    private final Map<OutputFormat, ReportExporter> exporters;

    public ReportExecutionWorker(
            ReportJobRepository reportJobRepository,
            DailyStatsRepository dailyStatsRepository,
            S3ReportStorageService storageService,
            WebhookDeliveryService webhookDeliveryService,
            List<ReportExporter> exporterList) {
        this.reportJobRepository = reportJobRepository;
        this.dailyStatsRepository = dailyStatsRepository;
        this.storageService = storageService;
        this.webhookDeliveryService = webhookDeliveryService;
        this.exporters = exporterList.stream().collect(Collectors.toUnmodifiableMap(
                exporter -> OutputFormat.valueOf(exporter.getFileExtension().toUpperCase(Locale.ROOT)),
                Function.identity()));
    }

    @RabbitListener(queues = "${app.messaging.job-queue:reporting.job.queue}")
    public void execute(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            log.warn("Received an empty reporting job id; acknowledging malformed message");
            return;
        }

        int claimed = reportJobRepository.claimPendingJob(jobId, ReportStatus.PENDING, ReportStatus.RUNNING);
        if (claimed == 0) {
            log.info("Ignoring duplicate or non-pending report job: jobId={}", jobId);
            return;
        }

        ReportJob job = reportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Claimed report job no longer exists: jobId={}", jobId);
            return;
        }

        try {
            List<Map<String, Object>> data = generateData(job);
            ReportExporter exporter = exporters.get(job.getOutputFormat());
            if (exporter == null) {
                throw new IllegalStateException("Unsupported report format: " + job.getOutputFormat());
            }
            Map<String, Object> metadata = Map.of(
                    "job_id", job.getId(),
                    "report_type", job.getReportType().name(),
                    "requested_by", job.getRequestedBy(),
                    "generated_at", Instant.now());
            byte[] content = exporter.export(data, job.getReportType().name(), metadata);
            String s3Key = storageService.uploadReport(
                    job.getReportType().name(), job.getId(), exporter.getFileExtension(),
                    content, exporter.getContentType());

            job.setStatus(ReportStatus.COMPLETED);
            job.setS3Key(s3Key);
            job.setRowCount(data.size());
            job.setCompletedAt(Instant.now());
            reportJobRepository.saveAndFlush(job);
            log.info("Report job completed: jobId={}, reportType={}, rows={}, s3Key={}",
                    job.getId(), job.getReportType(), data.size(), s3Key);
            try {
                webhookDeliveryService.notifyReportCompleted(job);
            } catch (RuntimeException webhookException) {
                // A notification scheduling problem must not revert a successfully stored report to FAILED.
                log.error("Could not schedule completion webhook: jobId={}", job.getId(), webhookException);
            }
        } catch (Exception exception) {
            job.setStatus(ReportStatus.FAILED);
            job.setErrorMessage(errorMessage(exception));
            job.setCompletedAt(Instant.now());
            reportJobRepository.saveAndFlush(job);
            log.error("Report job failed: jobId={}, reportType={}, reason={}",
                    jobId, job.getReportType(), exception.getMessage(), exception);
        }
    }

    private List<Map<String, Object>> generateData(ReportJob job) {
        Map<String, Object> parameters = job.getParameters() == null ? Map.of() : job.getParameters();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = parseDate(parameters.get("from_date"), LocalDate.of(2024, 1, 1), "from_date");
        LocalDate to = parseDate(parameters.get("to_date"), today, "to_date");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from_date must be on or before to_date");
        }
        String fromDate = from.toString();
        String toDate = to.toString();

        return switch (job.getReportType()) {
            case CONSULTATIONS -> mapMetric(
                    dailyStatsRepository.findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                            "CONSULTATIONS", fromDate, toDate), "consultations");
            case PATIENTS -> mapMetric(
                    dailyStatsRepository.findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                            "NEW_PATIENTS", fromDate, toDate), "new_patients");
            case PRESCRIPTIONS -> mapMetric(
                    dailyStatsRepository.findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                            "PRESCRIPTIONS", fromDate, toDate), "prescriptions");
            case DOCTORS -> doctorData(parameters, fromDate, toDate);
            case FULL_SYSTEM -> fullSystemData(fromDate, toDate);
            case CUSTOM -> customData(parameters, fromDate, toDate);
        };
    }

    private List<Map<String, Object>> mapMetric(List<DailyStats> stats, String valueColumn) {
        ensureQueryLimit(stats);
        List<Map<String, Object>> result = new ArrayList<>(stats.size());
        for (DailyStats stat : stats) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", stat.getStatDate());
            row.put(valueColumn, stat.getValue());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> doctorData(Map<String, Object> parameters, String fromDate, String toDate) {
        List<DailyStats> stats = dailyStatsRepository
                .findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                        "DOCTOR_CONSULTATIONS", fromDate, toDate);
        ensureQueryLimit(stats);
        Object doctorId = parameters.get("doctor_id");
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailyStats stat : stats) {
            if (doctorId != null && !Objects.equals(String.valueOf(doctorId), stat.getEntityId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("doctor_id", stat.getEntityId());
            row.put("date", stat.getStatDate());
            row.put("consultations", stat.getValue());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> fullSystemData(String fromDate, String toDate) {
        List<DailyStats> stats = dailyStatsRepository
                .findTop100001ByStatDateBetweenOrderByStatDateDescStatTypeAscEntityIdAsc(fromDate, toDate);
        ensureQueryLimit(stats);

        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (DailyStats stat : stats) {
            Map<String, Object> row = byDate.computeIfAbsent(stat.getStatDate(), date -> {
                Map<String, Object> initial = new LinkedHashMap<>();
                initial.put("Data", date);
                initial.put("Consultas", 0L);
                initial.put("Novos Pacientes", 0L);
                initial.put("Cancelamentos", 0L);
                initial.put("Prescrições", 0L);
                initial.put("Consultas por Médico", 0L);
                return initial;
            });
            String metric = switch (stat.getStatType()) {
                case "CONSULTATIONS" -> "Consultas";
                case "NEW_PATIENTS" -> "Novos Pacientes";
                case "CANCELLATIONS" -> "Cancelamentos";
                case "PRESCRIPTIONS" -> "Prescrições";
                case "DOCTOR_CONSULTATIONS" -> "Consultas por Médico";
                default -> null;
            };
            if (metric != null) {
                row.put(metric, ((Number) row.get(metric)).longValue() + stat.getValue());
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private List<Map<String, Object>> customData(Map<String, Object> parameters, String fromDate, String toDate) {
        List<DailyStats> stats = dailyStatsRepository
                .findTop100001ByStatDateBetweenOrderByStatDateDescStatTypeAscEntityIdAsc(fromDate, toDate);
        ensureQueryLimit(stats);
        Object requestedType = parameters.get("stat_type");
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailyStats stat : stats) {
            if (requestedType != null && !Objects.equals(String.valueOf(requestedType), stat.getStatType())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", stat.getStatDate());
            row.put("stat_type", stat.getStatType());
            row.put("entity_id", stat.getEntityId());
            row.put("value", stat.getValue());
            row.put("metadata", stat.getMetadata());
            result.add(row);
        }
        return result;
    }

    private LocalDate parseDate(Object value, LocalDate fallback, String parameterName) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " must use YYYY-MM-DD format", exception);
        }
    }

    private void ensureQueryLimit(List<DailyStats> stats) {
        if (stats.size() >= MAX_QUERY_ROWS) {
            throw new IllegalArgumentException("O relatório excede o limite de " + MAX_REPORT_ROWS + " linhas");
        }
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 4_000 ? message : message.substring(0, 4_000);
    }
}