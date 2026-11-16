package br.edu.ifrn.promptuario.reporting.model;

import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportStatus;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "report_jobs")
public class ReportJob {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 30, nullable = false)
    private ReportType reportType;

    @NotBlank
    @Size(max = 36)
    @Column(name = "requested_by", length = 36, nullable = false)
    private String requestedBy;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> parameters = new LinkedHashMap<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", length = 10, nullable = false)
    private OutputFormat outputFormat = OutputFormat.JSON;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private Map<String, Object> resultData;

    @Size(max = 500)
    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Min(0)
    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Size(max = 36)
    @Column(name = "schedule_id", length = 36)
    private String scheduleId;

    @Column(name = "webhook_triggered", nullable = false)
    private boolean webhookTriggered;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ReportJob() {
    }

    public ReportJob(ReportType reportType, String requestedBy, OutputFormat outputFormat,
                     Map<String, Object> parameters) {
        this.reportType = reportType;
        this.requestedBy = requestedBy;
        this.outputFormat = outputFormat;
        this.parameters = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}