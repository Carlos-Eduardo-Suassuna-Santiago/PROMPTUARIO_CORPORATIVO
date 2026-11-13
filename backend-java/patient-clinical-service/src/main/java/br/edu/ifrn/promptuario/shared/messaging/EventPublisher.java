package br.edu.ifrn.promptuario.shared.messaging;

import br.edu.ifrn.promptuario.shared.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static br.edu.ifrn.promptuario.config.RabbitMQConfig.DOMAIN_EVENTS_EXCHANGE;
import static br.edu.ifrn.promptuario.config.RabbitMQConfig.LEGACY_CLINICAL_EXCHANGE;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private static final String MEDICAL_RECORD_ROUTING_KEY = "medical_record.created";
    private static final String PRESCRIPTION_ROUTING_KEY = "prescription.generated";
    private static final String LEGACY_MEDICAL_RECORD_ROUTING_KEY = "clinical.medical_record.created";
    private static final String LEGACY_PRESCRIPTION_ROUTING_KEY = "clinical.prescription.generated";
    private final RabbitTemplate rabbitTemplate;
    private final RetryTemplate retryTemplate;
    private final boolean legacyPublishEnabled;

    public EventPublisher(RabbitTemplate rabbitTemplate,
                          RetryTemplate retryTemplate,
                          @org.springframework.beans.factory.annotation.Value("${app.messaging.legacy-publish-enabled:true}")
                          boolean legacyPublishEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.retryTemplate = retryTemplate;
        this.legacyPublishEnabled = legacyPublishEnabled;
    }

    @Async("eventPublishingExecutor")
    public void publish(String exchange, String routingKey, DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String correlationId = Objects.requireNonNull(event.correlationId(), "correlationId must not be null");
        String eventId = Objects.requireNonNull(event.eventId(), "eventId must not be null").toString();
        String timestamp = Objects.requireNonNull(event.timestamp(), "timestamp must not be null").toString();
        String eventType = Objects.requireNonNull(event.eventType(), "eventType must not be null");
        MessagePostProcessor headers = message -> {
            message.getMessageProperties().setHeader("correlationId", correlationId);
            message.getMessageProperties().setHeader("eventId", eventId);
            message.getMessageProperties().setHeader("timestamp", timestamp);
            message.getMessageProperties().setHeader("eventType", eventType);
            message.getMessageProperties().setCorrelationId(correlationId);
            message.getMessageProperties().setMessageId(eventId);
            return message;
        };

        Set<Destination> destinations = destinations(exchange, routingKey);
        for (Destination destination : destinations) {
            try {
                retryTemplate.execute(context -> {
                    rabbitTemplate.convertAndSend(destination.exchange(), destination.routingKey(), event, headers);
                    return null;
                });
                log.info("event_published publication={} exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                        destination.publication(), destination.exchange(), destination.routingKey(), eventId, eventType,
                        correlationId);
            } catch (RuntimeException ex) {
                log.error("event_publish_failed publication={} exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                        destination.publication(), destination.exchange(), destination.routingKey(), eventId, eventType,
                        correlationId, ex);
                if (destination.publication().equals("canonical")) throw ex;
            }
        }
    }

    private Set<Destination> destinations(String exchange, String routingKey) {
        Set<Destination> destinations = new LinkedHashSet<>();
        destinations.add(new Destination("canonical", exchange, routingKey));
        if (legacyPublishEnabled && DOMAIN_EVENTS_EXCHANGE.equals(exchange)) {
            switch (routingKey) {
                case MEDICAL_RECORD_ROUTING_KEY -> destinations.add(new Destination(
                        "legacy", LEGACY_CLINICAL_EXCHANGE, LEGACY_MEDICAL_RECORD_ROUTING_KEY));
                case PRESCRIPTION_ROUTING_KEY -> destinations.add(new Destination(
                        "legacy", LEGACY_CLINICAL_EXCHANGE, LEGACY_PRESCRIPTION_ROUTING_KEY));
                default -> { }
            }
        }
        return destinations;
    }

    private record Destination(String publication, String exchange, String routingKey) { }
}
