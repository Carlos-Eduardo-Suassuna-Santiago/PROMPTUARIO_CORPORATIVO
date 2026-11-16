package br.edu.ifrn.promptuario.reporting.messaging;

import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportStatus;
import br.edu.ifrn.promptuario.reporting.repository.ReportJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReportJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReportJobPublisher.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RabbitTemplate rabbitTemplate;
    private final ReportJobRepository reportJobRepository;
    private final String queueName;

    public ReportJobPublisher(
            RabbitTemplate rabbitTemplate,
            ReportJobRepository reportJobRepository,
            @Value("${app.messaging.job-queue:reporting.job.queue}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.reportJobRepository = reportJobRepository;
        this.queueName = queueName;
    }

    @Async("reportTaskExecutor")
    public void publish(String jobId) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(queueName, jobId);
                log.info("Published report job: jobId={}, queue={}, attempt={}", jobId, queueName, attempt);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("Could not publish report job: jobId={}, queue={}, attempt={}/{}",
                        jobId, queueName, attempt, MAX_ATTEMPTS, exception);
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(250L * (1L << (attempt - 1)));
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        lastFailure = new IllegalStateException("RabbitMQ publication retry interrupted",
                                interruptedException);
                        break;
                    }
                }
            }
        }

        String dispatchFailureReason = lastFailure == null ? "RabbitMQ publication failed" : lastFailure.getMessage();
        reportJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(ReportStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(dispatchFailureReason == null ? "RabbitMQ publication failed"
                    : dispatchFailureReason);
            reportJobRepository.save(job);
        });
        log.error("Report job could not be dispatched after {} attempts: jobId={}", MAX_ATTEMPTS, jobId,
                lastFailure);
    }
}