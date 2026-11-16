package br.edu.ifrn.promptuario.reporting.dto;

import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportStatus;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReportJobResponseDTO(
        String id,
        ReportType reportType,
        OutputFormat outputFormat,
        ReportStatus status,
        int rowCount,
        String s3Key,
        String downloadUrl,
        String errorMessage,
        Instant createdAt,
        Instant completedAt) {

    public static ReportJobResponseDTO fromEntity(ReportJob job, String presignedUrl) {
        if (job == null) {
            throw new IllegalArgumentException("ReportJob não pode ser nulo");
        }
        return new ReportJobResponseDTO(
                job.getId(),
                job.getReportType(),
                job.getOutputFormat(),
                job.getStatus(),
                job.getRowCount(),
                job.getS3Key(),
                presignedUrl,
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt());
    }
}