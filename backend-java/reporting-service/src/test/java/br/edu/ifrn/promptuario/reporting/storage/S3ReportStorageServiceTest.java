package br.edu.ifrn.promptuario.reporting.storage;

import br.edu.ifrn.promptuario.reporting.config.S3Config;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ReportStorageServiceTest {

    @Test
    void uploadsReportWithCanonicalKeyAndMetadata() {
        S3Client client = mock(S3Client.class);
        S3Presigner presigner = mock(S3Presigner.class);
        when(client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        S3ReportStorageService service = new S3ReportStorageService(client, presigner, "reports", 60);

        String key = service.uploadReport("CONSULTATIONS", "job-123", "CSV",
                "data".getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/csv");

        assertEquals("reports/consultations/job-123.csv", key);
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertEquals("reports", request.getValue().bucket());
        assertEquals(key, request.getValue().key());
        assertEquals("text/csv", request.getValue().contentType());
        assertEquals("4", request.getValue().metadata().get("content-length"));
        assertEquals("job-123", request.getValue().metadata().get("job-id"));
        assertTrue(request.getValue().metadata().containsKey("generated-at"));
    }

    @Test
    void presignedUrlUsesConfiguredPublicEndpointAndDefaultExpiry() {
        S3Config config = new S3Config(
                "http://minio:9000", "https://reports.example.test", "access", "secret", "us-east-1", "reports");
        try (S3Presigner presigner = config.s3Presigner()) {
            S3ReportStorageService service = new S3ReportStorageService(
                    mock(S3Client.class), presigner, "reports", 60);

            String url = service.generatePresignedDownloadUrl("reports/consultations/job-123.csv", null);

            assertTrue(url.startsWith("https://reports.example.test/reports/"));
            assertTrue(url.contains("X-Amz-Expires=3600"));
        }
    }

    @Test
    void rejectsUnsafeKeysAndExpirationAboveS3Limit() {
        S3ReportStorageService service = new S3ReportStorageService(
                mock(S3Client.class), mock(S3Presigner.class), "reports", 60);

        assertThrows(IllegalArgumentException.class,
                () -> service.uploadReport("CONSULTATIONS", "../job", "csv", new byte[0], "text/csv"));
        assertThrows(IllegalArgumentException.class,
                () -> service.generatePresignedDownloadUrl("reports/consultations/job.csv", Duration.ofDays(8)));
    }

    @Test
    void deletesReportByBucketAndKey() {
        S3Client client = mock(S3Client.class);
        S3ReportStorageService service = new S3ReportStorageService(
                client, mock(S3Presigner.class), "reports", 60);

        service.deleteReport("reports/consultations/job-123.csv");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(request.capture());
        assertEquals("reports", request.getValue().bucket());
        assertEquals("reports/consultations/job-123.csv", request.getValue().key());
    }
}
