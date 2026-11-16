package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.WebhookConfig;
import br.edu.ifrn.promptuario.reporting.model.WebhookDeliveryLog;
import br.edu.ifrn.promptuario.reporting.repository.WebhookConfigRepository;
import br.edu.ifrn.promptuario.reporting.repository.WebhookDeliveryLogRepository;
import br.edu.ifrn.promptuario.reporting.storage.S3ReportStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final String COMPLETED_EVENT = "report.completed";
    private static final int MAX_RESPONSE_BODY_CHARS = 4_096;
    private static final int MAX_RETRIES = 10;
    private static final int MAX_RETRY_INTERVAL_SECONDS = 3_600;

    private final WebhookConfigRepository webhookConfigRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final S3ReportStorageService storageService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Executor reportTaskExecutor;

    public WebhookDeliveryService(
            WebhookConfigRepository webhookConfigRepository,
            WebhookDeliveryLogRepository deliveryLogRepository,
            S3ReportStorageService storageService,
            ObjectMapper objectMapper,
            @Qualifier("reportTaskExecutor") Executor reportTaskExecutor) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.reportTaskExecutor = reportTaskExecutor;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Async("reportTaskExecutor")
    public void notifyReportCompleted(ReportJob job) {
        if (job == null || job.getS3Key() == null) {
            return;
        }
        List<WebhookConfig> subscriptions = webhookConfigRepository.findByActiveTrue().stream()
                .filter(config -> config.getEvents() != null && config.getEvents().contains(COMPLETED_EVENT))
                .toList();
        if (subscriptions.isEmpty()) {
            return;
        }

        String downloadUrl;
        try {
            downloadUrl = storageService.generatePresignedDownloadUrl(job.getS3Key(), null);
        } catch (RuntimeException exception) {
            log.error("Could not create download URL for completion webhooks: jobId={}", job.getId(), exception);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", COMPLETED_EVENT);
        payload.put("job_id", job.getId());
        payload.put("report_type", job.getReportType().name());
        payload.put("output_format", job.getOutputFormat().name());
        payload.put("status", job.getStatus().name());
        payload.put("row_count", job.getRowCount());
        payload.put("s3_key", job.getS3Key());
        payload.put("download_url", downloadUrl);
        payload.put("completed_at", job.getCompletedAt() == null ? null : job.getCompletedAt().toString());

        final byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException exception) {
            log.error("Could not serialize completion webhook payload: jobId={}", job.getId(), exception);
            return;
        }

        for (WebhookConfig subscription : subscriptions) {
            WebhookTarget target = new WebhookTarget(
                    subscription.getId(), subscription.getUrl(), subscription.getSecret(),
                    Math.min(Math.max(0, subscription.getMaxRetries()), MAX_RETRIES),
                    Math.min(Math.max(0, subscription.getRetryIntervalSeconds()), MAX_RETRY_INTERVAL_SECONDS));
            deliverAttempt(target, job.getId(), body, 1);
        }
    }

    private void deliverAttempt(WebhookTarget target, String jobId, byte[] payload, int attempt) {
        WebhookConfig config = webhookConfigRepository.findById(target.configId()).orElse(null);
        if (config == null || !config.isActive()
                || config.getEvents() == null || !config.getEvents().contains(COMPLETED_EVENT)) {
            log.info("Skipping disabled or removed webhook: webhookConfigId={}, jobId={}", target.configId(), jobId);
            return;
        }

        DeliveryResult result = send(target, jobId, payload);
        WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog(config, jobId, COMPLETED_EVENT, target.url());
        deliveryLog.setAttempt(attempt);
        deliveryLog.setStatusCode(result.statusCode());
        deliveryLog.setResponseBody(truncate(result.responseBody()));
        deliveryLog.setSuccess(result.success());
        deliveryLog.setErrorMessage(truncate(result.errorMessage()));
        deliveryLogRepository.save(deliveryLog);

        java.time.Instant deliveredAt = java.time.Instant.now();
        config.setLastDeliveryAt(deliveredAt);
        if (result.success()) {
            config.setLastSuccessAt(deliveredAt);
            config.setConsecutiveFailures(0);
            webhookConfigRepository.save(config);
            log.info("Webhook delivery succeeded: webhookConfigId={}, jobId={}, attempt={}, statusCode={}",
                    target.configId(), jobId, attempt, result.statusCode());
            return;
        }

        config.setConsecutiveFailures(config.getConsecutiveFailures() + 1);
        webhookConfigRepository.save(config);
        log.warn("Webhook delivery failed: webhookConfigId={}, jobId={}, attempt={}, statusCode={}",
                target.configId(), jobId, attempt, result.statusCode());

        if (attempt <= target.maxRetries()) {
            long delaySeconds = target.retryIntervalSeconds();
            CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS, reportTaskExecutor)
                    .execute(() -> deliverAttempt(target, jobId, payload, attempt + 1));
        }
    }

    private DeliveryResult send(WebhookTarget target, String jobId, byte[] payload) {
        try {
            String signature = signatureHeader(payload, target.secret());
            return restClient.post()
                    .uri(target.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Signature", signature)
                    .header("X-Webhook-Event", COMPLETED_EVENT)
                    .header("X-Webhook-Job-Id", jobId)
                    .body(payload)
                    .exchange((request, response) -> {
                        try {
                            String responseBody = new String(
                                    response.getBody().readNBytes(MAX_RESPONSE_BODY_CHARS), StandardCharsets.UTF_8);
                            int statusCode = response.getStatusCode().value();
                            return new DeliveryResult(statusCode, responseBody,
                                    statusCode >= 200 && statusCode < 300, null);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
        } catch (RuntimeException | GeneralSecurityException exception) {
            return new DeliveryResult(null, null, false,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    static String signatureHeader(byte[] payload, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_RESPONSE_BODY_CHARS) {
            return value;
        }
        return value.substring(0, MAX_RESPONSE_BODY_CHARS);
    }

    private record WebhookTarget(String configId, String url, String secret,
                                 int maxRetries, int retryIntervalSeconds) {
    }

    private record DeliveryResult(Integer statusCode, String responseBody,
                                  boolean success, String errorMessage) {
    }
}