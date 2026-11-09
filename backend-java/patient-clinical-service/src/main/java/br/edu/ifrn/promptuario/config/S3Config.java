package br.edu.ifrn.promptuario.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${app.storage.s3.endpoint}")
    private String endpoint;

    @Value("${app.storage.s3.access-key}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key}")
    private String secretKey;

    @Value("${app.storage.s3.region:us-east-1}")
    private String region;

    @Value("${app.storage.s3.bucket-patient-documents:patient-documents}")
    private String patientDocumentsBucket;

    @Value("${app.storage.s3.bucket-prescriptions:prescriptions}")
    private String prescriptionsBucket;

    @Autowired
    private S3Client configuredS3Client;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(pathStyleConfiguration())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(pathStyleConfiguration())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration pathStyleConfiguration() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBuckets() {
        ensureBucket(patientDocumentsBucket);
        ensureBucket(prescriptionsBucket);
    }

    private void ensureBucket(String bucket) {
        try {
            configuredS3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket ready: bucket={}", bucket);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404 || ex.statusCode() == 400) {
                try {
                    configuredS3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                    log.info("S3 bucket created: bucket={}", bucket);
                } catch (S3Exception createError) {
                    log.warn("Could not create S3 bucket: bucket={}, status={}", bucket, createError.statusCode());
                }
            } else {
                log.warn("Could not verify S3 bucket: bucket={}, status={}", bucket, ex.statusCode());
            }
        } catch (RuntimeException ex) {
            log.warn("S3 bootstrap unavailable: bucket={}, message={}", bucket, ex.getMessage());
        }
    }
}
