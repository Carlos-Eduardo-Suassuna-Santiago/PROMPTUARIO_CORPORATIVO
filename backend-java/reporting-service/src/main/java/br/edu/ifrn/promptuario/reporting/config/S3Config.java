package br.edu.ifrn.promptuario.reporting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Objects;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    private final String endpoint;
    private final String publicEndpoint;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String reportsBucket;

    public S3Config(
            @Value("${app.storage.s3.endpoint}") String endpoint,
            @Value("${app.storage.s3.public-endpoint:}") String publicEndpoint,
            @Value("${app.storage.s3.access-key}") String accessKey,
            @Value("${app.storage.s3.secret-key}") String secretKey,
            @Value("${app.storage.s3.region:us-east-1}") String region,
            @Value("${app.storage.s3.bucket-reports:reports}") String reportsBucket) {
        this.endpoint = requireEndpoint(endpoint, "S3_ENDPOINT");
        this.publicEndpoint = publicEndpoint == null || publicEndpoint.isBlank()
                ? null
                : requireEndpoint(publicEndpoint, "S3_PUBLIC_ENDPOINT");
        this.accessKey = Objects.requireNonNull(accessKey, "S3 access key must not be null");
        this.secretKey = Objects.requireNonNull(secretKey, "S3 secret key must not be null");
        this.region = Objects.requireNonNull(region, "S3 region must not be null");
        this.reportsBucket = Objects.requireNonNull(reportsBucket, "Reports bucket must not be null");
    }

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
        // Sign with the public host itself when configured. Rewriting the host after
        // signing would invalidate SigV4 because the host is part of the signature.
        String signingEndpoint = publicEndpoint == null ? endpoint : publicEndpoint;
        return S3Presigner.builder()
                .endpointOverride(URI.create(signingEndpoint))
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(pathStyleConfiguration())
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureReportsBucket() {
        ensureBucket(s3Client(), reportsBucket);
    }

    private void ensureBucket(S3Client client, String bucket) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket is ready: bucket={}", bucket);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                createBucket(client, bucket);
                return;
            }
            log.warn("Could not verify S3 bucket: bucket={}, statusCode={}, errorCode={}",
                    bucket, exception.statusCode(), exception.awsErrorDetails() == null
                            ? "unknown" : exception.awsErrorDetails().errorCode(), exception);
        } catch (RuntimeException exception) {
            log.warn("S3 bucket bootstrap is temporarily unavailable: bucket={}, reason={}",
                    bucket, exception.getMessage(), exception);
        }
    }

    private void createBucket(S3Client client, String bucket) {
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket: bucket={}", bucket);
        } catch (S3Exception exception) {
            // A concurrent service instance may have created the bucket after HeadBucket.
            if (exception.statusCode() == 409 || "BucketAlreadyOwnedByYou".equals(
                    exception.awsErrorDetails() == null ? null : exception.awsErrorDetails().errorCode())) {
                log.info("S3 bucket already exists: bucket={}", bucket);
                return;
            }
            log.warn("Could not create S3 bucket: bucket={}, statusCode={}",
                    bucket, exception.statusCode(), exception);
        } catch (RuntimeException exception) {
            log.warn("S3 bucket creation is temporarily unavailable: bucket={}, reason={}",
                    bucket, exception.getMessage(), exception);
        }
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration pathStyleConfiguration() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    private static String requireEndpoint(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        URI uri = URI.create(value);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException(propertyName + " must be an absolute HTTP(S) URL");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(propertyName + " must use HTTP or HTTPS");
        }
        return value.replaceAll("/$", "");
    }
}