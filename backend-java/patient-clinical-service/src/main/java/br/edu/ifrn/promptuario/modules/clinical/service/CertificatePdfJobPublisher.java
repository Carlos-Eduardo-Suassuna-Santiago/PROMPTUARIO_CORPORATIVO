package br.edu.ifrn.promptuario.modules.clinical.service;

import br.edu.ifrn.promptuario.config.PdfWorkerConfig;
import br.edu.ifrn.promptuario.modules.clinical.event.CertificatePdfRequestedEvent;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.Map;
import static br.edu.ifrn.promptuario.config.RabbitMQConfig.DOMAIN_EVENTS_EXCHANGE;

@Component
public class CertificatePdfJobPublisher {
    private static final Logger log=LoggerFactory.getLogger(CertificatePdfJobPublisher.class);
    private final RabbitTemplate rabbitTemplate; private final RetryTemplate retryTemplate;
    public CertificatePdfJobPublisher(RabbitTemplate rabbitTemplate,RetryTemplate retryTemplate){this.rabbitTemplate=rabbitTemplate;this.retryTemplate=retryTemplate;}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void publish(CertificatePdfRequestedEvent event){retryTemplate.execute(ctx->{rabbitTemplate.convertAndSend(DOMAIN_EVENTS_EXCHANGE,PdfWorkerConfig.CERTIFICATE_PDF_ROUTING_KEY,Map.of("certificateId",event.certificateId()));return null;});log.info("certificate_pdf_job_published certificateId={}",event.certificateId());}
}
