package br.edu.ifrn.promptuario.shared.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public void uploadFile(String bucket, String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key)
                .contentType(contentType).contentLength((long) content.length).build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception ex) {
            log.error("S3 upload failed: bucket={}, key={}, status={}", bucket, key, ex.statusCode(), ex);
            throw ex;
        }
    }

    public void uploadInputStream(String bucket, String key, InputStream inputStream,
                                  long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key)
                .contentType(contentType).contentLength(contentLength).build();
        try {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (S3Exception ex) {
            log.error("S3 stream upload failed: bucket={}, key={}, status={}", bucket, key, ex.statusCode(), ex);
            throw ex;
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String bucket, String key) {
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            log.error("S3 download failed: bucket={}, key={}, status={}", bucket, key, ex.statusCode(), ex);
            throw ex;
        }
    }

    public URL generatePresignedDownloadUrl(String bucket, String key, Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Presigned URL duration must be positive");
        }
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return s3Presigner.presignGetObject(request).url();
    }

    public void deleteFile(String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            log.error("S3 delete failed: bucket={}, key={}, status={}", bucket, key, ex.statusCode(), ex);
            throw ex;
        }
    }
}
