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
import br.edu.ifrn.promptuario.reporting.worker.exporters.JsonReportExporter;
import br.edu.ifrn.promptuario.reporting.worker.exporters.ReportExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExecutionWorkerTest {

    @Test
    void completesJsonReportAndTriggersWebhook() {
        ReportJobRepository jobs = mock(ReportJobRepository.class);
        DailyStatsRepository stats = mock(DailyStatsRepository.class);
        S3ReportStorageService storage = mock(S3ReportStorageService.class);
        WebhookDeliveryService webhooks = mock(WebhookDeliveryService.class);
        ReportJob job = new ReportJob(ReportType.PATIENTS, "user-1", OutputFormat.JSON,
                Map.of("from_date", "2026-01-01", "to_date", "2026-01-02"));
        job.setId("job-1");
        job.setCreatedAt(Instant.now());

        when(jobs.claimPendingJob("job-1", ReportStatus.PENDING, ReportStatus.RUNNING)).thenReturn(1);
        when(jobs.findById("job-1")).thenReturn(Optional.of(job));
        when(stats.findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                "NEW_PATIENTS", "2026-01-01", "2026-01-02"))
                .thenReturn(List.of(new DailyStats("2026-01-02", "NEW_PATIENTS", null, 3, Map.of())));
        when(storage.uploadReport(eq("PATIENTS"), eq("job-1"), eq("json"), any(byte[].class),
                eq("application/json"))).thenReturn("reports/patients/job-1.json");
        doThrow(new IllegalStateException("notification executor unavailable"))
                .when(webhooks).notifyReportCompleted(job);

        ReportExecutionWorker worker = new ReportExecutionWorker(
                jobs, stats, storage, webhooks, List.of(new JsonReportExporter(new ObjectMapper())));
        worker.execute("job-1");

        assertEquals(ReportStatus.COMPLETED, job.getStatus());
        assertEquals("reports/patients/job-1.json", job.getS3Key());
        assertEquals(1, job.getRowCount());
        verify(jobs).saveAndFlush(job);
        verify(webhooks).notifyReportCompleted(job);
    }

    @Test
    void duplicateDeliveryDoesNotReprocessClaimedJob() {
        ReportJobRepository jobs = mock(ReportJobRepository.class);
        DailyStatsRepository stats = mock(DailyStatsRepository.class);
        ReportExecutionWorker worker = new ReportExecutionWorker(
                jobs, stats, mock(S3ReportStorageService.class), mock(WebhookDeliveryService.class), List.of());
        when(jobs.claimPendingJob("job-1", ReportStatus.PENDING, ReportStatus.RUNNING)).thenReturn(0);

        worker.execute("job-1");

        verify(jobs, never()).findById("job-1");
        verify(stats, never()).findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                any(), any(), any());
    }

        @Test
        void marksJobFailedWhenExporterThrows() {
                ReportJobRepository jobs = mock(ReportJobRepository.class);
                DailyStatsRepository stats = mock(DailyStatsRepository.class);
                S3ReportStorageService storage = mock(S3ReportStorageService.class);
                WebhookDeliveryService webhooks = mock(WebhookDeliveryService.class);
                ReportJob job = new ReportJob(ReportType.PATIENTS, "user-1", OutputFormat.CSV,
                                Map.of("from_date", "2026-01-01", "to_date", "2026-01-02"));
                job.setId("job-failure");
                when(jobs.claimPendingJob("job-failure", ReportStatus.PENDING, ReportStatus.RUNNING)).thenReturn(1);
                when(jobs.findById("job-failure")).thenReturn(Optional.of(job));
                when(stats.findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
                                "NEW_PATIENTS", "2026-01-01", "2026-01-02")).thenReturn(List.of());

                ReportExecutionWorker worker = new ReportExecutionWorker(jobs, stats, storage, webhooks,
                                List.of(new ReportExporter() {
                                        @Override
                                        public byte[] export(List<Map<String, Object>> data, String reportTitle,
                                                                                 Map<String, Object> metadata) {
                                                throw new IllegalStateException("export failed");
                                        }

                                        @Override
                                        public String getContentType() {
                                                return "text/csv";
                                        }

                                        @Override
                                        public String getFileExtension() {
                                                return "csv";
                                        }
                                }));

                worker.execute("job-failure");

                assertEquals(ReportStatus.FAILED, job.getStatus());
                assertEquals("export failed", job.getErrorMessage());
                verify(jobs).saveAndFlush(job);
                verify(storage, never()).uploadReport(anyString(), anyString(), anyString(), any(byte[].class), anyString());
                verify(webhooks, never()).notifyReportCompleted(any());
        }
}
