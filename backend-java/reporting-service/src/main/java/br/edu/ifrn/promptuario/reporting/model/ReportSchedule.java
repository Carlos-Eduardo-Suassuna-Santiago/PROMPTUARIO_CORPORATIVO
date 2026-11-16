package br.edu.ifrn.promptuario.reporting.model;

import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "report_schedules")
public class ReportSchedule {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Size(max = 120)
    @Column(length = 120, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 30, nullable = false)
    private ReportType reportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", length = 10, nullable = false)
    private OutputFormat outputFormat = OutputFormat.XLSX;

    @NotBlank
    @Size(max = 100)
    @Column(name = "cron_expression", length = 100, nullable = false)
    private String cronExpression;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> parameters = new LinkedHashMap<>();

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> recipients = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @NotBlank
    @Size(max = 36)
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ReportSchedule() {
    }

    public ReportSchedule(String name, ReportType reportType, OutputFormat outputFormat,
                          String cronExpression, String createdBy,
                          Map<String, Object> parameters, List<String> recipients) {
        this.name = name;
        this.reportType = reportType;
        this.outputFormat = outputFormat == null ? OutputFormat.XLSX : outputFormat;
        this.cronExpression = cronExpression;
        this.createdBy = createdBy;
        this.parameters = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);
        this.recipients = recipients == null ? new ArrayList<>() : new ArrayList<>(recipients);
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}