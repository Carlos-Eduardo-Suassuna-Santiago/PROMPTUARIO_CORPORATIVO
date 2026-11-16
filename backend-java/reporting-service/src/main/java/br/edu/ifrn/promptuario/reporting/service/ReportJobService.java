package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.dto.ReportExportRequestDTO;
import br.edu.ifrn.promptuario.reporting.dto.ReportJobResponseDTO;
import br.edu.ifrn.promptuario.reporting.messaging.ReportJobPublisher;
import br.edu.ifrn.promptuario.reporting.model.ReportAuditLog;
import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.ReportSchedule;
import br.edu.ifrn.promptuario.reporting.repository.ReportAuditLogRepository;
import br.edu.ifrn.promptuario.reporting.repository.ReportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.Objects;

@Service
public class ReportJobService {

    private final ReportJobRepository reportJobRepository;
    private final ReportAuditLogRepository reportAuditLogRepository;
    private final ReportJobPublisher reportJobPublisher;

    public ReportJobService(ReportJobRepository reportJobRepository,
                            ReportAuditLogRepository reportAuditLogRepository,
                            ReportJobPublisher reportJobPublisher) {
        this.reportJobRepository = reportJobRepository;
        this.reportAuditLogRepository = reportAuditLogRepository;
        this.reportJobPublisher = reportJobPublisher;
    }

    @Transactional
    public ReportJobResponseDTO createExportJob(ReportExportRequestDTO request, String userId) {
        return persistAndSchedule(request, userId, null);
    }

    @Transactional
    public ReportJobResponseDTO createScheduledExportJob(ReportSchedule schedule) {
        Objects.requireNonNull(schedule, "Report schedule must not be null");
        ReportExportRequestDTO request = new ReportExportRequestDTO(
                schedule.getReportType(), schedule.getOutputFormat(), schedule.getParameters());
        return persistAndSchedule(request, schedule.getCreatedBy(), schedule.getId());
    }

    private ReportJobResponseDTO persistAndSchedule(ReportExportRequestDTO request, String userId,
                                                     String scheduleId) {
        Objects.requireNonNull(request, "Export request must not be null");
        Objects.requireNonNull(request.reportType(), "Report type must not be null");
        Objects.requireNonNull(request.outputFormat(), "Output format must not be null");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id must not be blank");
        }

        ReportJob job = new ReportJob(request.reportType(), userId, request.outputFormat(), request.parameters());
        job.setScheduleId(scheduleId);
        ReportJob savedJob = reportJobRepository.saveAndFlush(job);

        ReportAuditLog auditLog = new ReportAuditLog(
                scheduleId == null ? "REPORT_REQUESTED" : "SCHEDULED_REPORT_REQUESTED",
                "report_job",
                savedJob.getId(),
                "Relatório " + request.reportType() + " solicitado no formato " + request.outputFormat(),
                userId,
                Map.of("report_type", request.reportType().name(),
                        "output_format", request.outputFormat().name()),
                null);
        reportAuditLogRepository.save(auditLog);

        String jobId = savedJob.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reportJobPublisher.publish(jobId);
            }
        });
        return ReportJobResponseDTO.fromEntity(savedJob, null);
    }
}