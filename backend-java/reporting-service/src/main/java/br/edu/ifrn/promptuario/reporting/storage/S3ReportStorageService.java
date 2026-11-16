package br.edu.ifrn.promptuario.reporting.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class S3ReportStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ReportStorageService.class);
    private static final Duration MAX_PRESIGNED_URL_EXPIRATION = Duration.ofDays(7);
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration defaultExpiration;

    public S3ReportStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${app.storage.s3.bucket-reports:reports}") String bucket,
            @Value("${app.storage.s3.presigned-url-expiration-minutes:60}") long expirationMinutes) {
        this.s3Client = Objects.requireNonNull(s3Client, "S3Client must not be null");
        this.s3Presigner = Objects.requireNonNull(s3Presigner, "S3Presigner must not be null");
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Reports bucket must not be blank");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("Presigned URL expiration must be positive");
        }
        this.bucket = bucket;
        this.defaultExpiration = Duration.ofMinutes(expirationMinutes);
        validateExpiration(defaultExpiration);
    }

    public String uploadReport(String reportType, String jobId, String extension,
                               byte[] content, String contentType) {
        Objects.requireNonNull(content, "Report content must not be null");
        String key = buildReportKey(reportType, jobId, extension);
        String effectiveContentType = contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
        Instant generatedAt = Instant.now();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(effectiveContentType)
                .contentLength((long) content.length)
                .metadata(Map.of(
                        "job-id", jobId,
                        "report-type", reportType.toLowerCase(Locale.ROOT),
                        "generated-at", generatedAt.toString(),
                        "content-length", Long.toString(content.length)))
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
            log.info("Report uploaded to S3: bucket={}, key={}, sizeBytes={}, contentType={}",
                    bucket, key, content.length, effectiveContentType);
            return key;
        } catch (S3Exception exception) {
            log.error("S3 report upload failed: bucket={}, key={}, statusCode={}",
                    bucket, key, exception.statusCode(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("S3 report upload failed: bucket={}, key={}, reason={}",
                    bucket, key, exception.getMessage(), exception);
            throw exception;
        }
    }

    public String generatePresignedDownloadUrl(String s3Key, Duration expiration) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("S3 key must not be blank");
        }
        Duration effectiveExpiration = expiration == null ? defaultExpiration : expiration;
        validateExpiration(effectiveExpiration);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(effectiveExpiration)
                .getObjectRequest(getObjectRequest)
                .build();
        try {
            String url = s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
            log.info("Generated presigned report download URL: bucket={}, key={}, expirationSeconds={}",
                    bucket, s3Key, effectiveExpiration.toSeconds());
            return url;
        } catch (RuntimeException exception) {
            log.error("Could not presign report download URL: bucket={}, key={}, reason={}",
                    bucket, s3Key, exception.getMessage(), exception);
            throw exception;
        }
    }

    public void deleteReport(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("S3 key must not be blank");
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
            log.info("Report deleted from S3: bucket={}, key={}", bucket, s3Key);
        } catch (S3Exception exception) {
            log.error("S3 report deletion failed: bucket={}, key={}, statusCode={}",
                    bucket, s3Key, exception.statusCode(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("S3 report deletion failed: bucket={}, key={}, reason={}",
                    bucket, s3Key, exception.getMessage(), exception);
            throw exception;
        }
    }

    private String buildReportKey(String reportType, String jobId, String extension) {
        String safeReportType = validateSegment(reportType, "reportType").toLowerCase(Locale.ROOT);
        String safeJobId = validateSegment(jobId, "jobId");
        String safeExtension = validateSegment(extension, "extension").toLowerCase(Locale.ROOT);
        return "reports/%s/%s.%s".formatted(safeReportType, safeJobId, safeExtension);
    }

    private String validateSegment(String value, String argumentName) {
        if (value == null || !SAFE_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(argumentName + " contains invalid characters");
        }
        return value;
    }

    private void validateExpiration(Duration expiration) {
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("Presigned URL expiration must be positive");
        }
        if (expiration.compareTo(MAX_PRESIGNED_URL_EXPIRATION) > 0) {
            throw new IllegalArgumentException("Presigned URL expiration must not exceed 7 days");
        }
    }
}