package br.edu.ifrn.promptuario.modules.clinical.service;

import br.edu.ifrn.promptuario.config.PdfWorkerConfig;
import br.edu.ifrn.promptuario.modules.clinical.event.PrescriptionPdfRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

import static br.edu.ifrn.promptuario.config.RabbitMQConfig.DOMAIN_EVENTS_EXCHANGE;

@Component
public class PrescriptionPdfJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionPdfJobPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RetryTemplate retryTemplate;

    public PrescriptionPdfJobPublisher(RabbitTemplate rabbitTemplate, RetryTemplate retryTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.retryTemplate = retryTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(PrescriptionPdfRequestedEvent event) {
        retryTemplate.execute(context -> {
            rabbitTemplate.convertAndSend(
                    DOMAIN_EVENTS_EXCHANGE,
                    PdfWorkerConfig.PDF_ROUTING_KEY,
                    Map.of("prescriptionId", event.prescriptionId()));
            return null;
        });
        log.info("prescription_pdf_job_published exchange={} routingKey={} prescriptionId={}",
                DOMAIN_EVENTS_EXCHANGE, PdfWorkerConfig.PDF_ROUTING_KEY, event.prescriptionId());
    }
}
