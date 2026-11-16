package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.dto.ReportExportRequestDTO;
import br.edu.ifrn.promptuario.reporting.dto.ReportJobResponseDTO;
import br.edu.ifrn.promptuario.reporting.messaging.ReportJobPublisher;
import br.edu.ifrn.promptuario.reporting.model.ReportAuditLog;
import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import br.edu.ifrn.promptuario.reporting.repository.ReportAuditLogRepository;
import br.edu.ifrn.promptuario.reporting.repository.ReportJobRepository;
import br.edu.ifrn.promptuario.reporting.storage.S3ReportStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportJobServiceTest {

    @Test
    void persistsJobAndAuditBeforePublishingAfterCommit() {
        ReportJobRepository jobs = mock(ReportJobRepository.class);
        ReportAuditLogRepository audits = mock(ReportAuditLogRepository.class);
        ReportJobPublisher publisher = mock(ReportJobPublisher.class);
        S3ReportStorageService storage = mock(S3ReportStorageService.class);
        when(jobs.saveAndFlush(any(ReportJob.class))).thenAnswer(invocation -> {
            ReportJob job = invocation.getArgument(0);
            job.setId("job-42");
            job.setCreatedAt(Instant.now());
            return job;
        });
        when(audits.save(any(ReportAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportJobService service = new ReportJobService(jobs, audits, publisher, storage);
        TransactionSynchronizationManager.initSynchronization();
        try {
            ReportJobResponseDTO response = service.createExportJob(
                    new ReportExportRequestDTO(ReportType.CONSULTATIONS, OutputFormat.CSV,
                            Map.of("from_date", "2026-01-01")), "user-42");

            assertEquals("job-42", response.id());
            assertEquals("PENDING", response.status().name());
            verify(jobs).saveAndFlush(any(ReportJob.class));
            verify(audits).save(any(ReportAuditLog.class));
            verify(publisher, never()).publish(any());

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            verify(publisher).publish("job-42");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
