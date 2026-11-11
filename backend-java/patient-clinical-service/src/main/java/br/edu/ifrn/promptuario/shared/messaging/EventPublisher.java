package br.edu.ifrn.promptuario.shared.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    public void publish(String exchange, String routingKey, Object event) {
        String correlationId = correlationId(event);
        String eventId = eventId(event);
        String timestamp = timestamp(event);
        String eventType = eventType(event);
        MessagePostProcessor headers = message -> {
            message.getMessageProperties().setHeader("correlationId", correlationId);
            message.getMessageProperties().setHeader("eventId", eventId);
            message.getMessageProperties().setHeader("timestamp", timestamp);
            message.getMessageProperties().setHeader("eventType", eventType);
            if (eventId != null) message.getMessageProperties().setMessageId(eventId);
            return message;
        };
        try {
            retryTemplate.execute(context -> {
                rabbitTemplate.convertAndSend(exchange, routingKey, event, headers);
                return null;
            });
            log.info("event_published publication=canonical exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                    exchange, routingKey, eventId, eventType, correlationId);
        } catch (RuntimeException ex) {
            log.error("event_publish_failed publication=canonical exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                    exchange, routingKey, eventId, eventType, correlationId, ex);
            throw ex;
        }

        if (legacyPublishEnabled) publishLegacyIfNeeded(routingKey, event, headers, eventId, eventType, correlationId);
    }

    private void publishLegacyIfNeeded(String routingKey, Object event, MessagePostProcessor headers,
                                       String eventId, String eventType, String correlationId) {
        String legacyRoutingKey = switch (routingKey) {
            case MEDICAL_RECORD_ROUTING_KEY -> LEGACY_MEDICAL_RECORD_ROUTING_KEY;
            case PRESCRIPTION_ROUTING_KEY -> LEGACY_PRESCRIPTION_ROUTING_KEY;
            default -> null;
        };
        if (legacyRoutingKey == null) return;
        try {
            retryTemplate.execute(context -> {
                rabbitTemplate.convertAndSend(LEGACY_CLINICAL_EXCHANGE, legacyRoutingKey, event, headers);
                return null;
            });
            log.info("event_published publication=legacy exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                    LEGACY_CLINICAL_EXCHANGE, legacyRoutingKey, eventId, eventType, correlationId);
        } catch (RuntimeException ex) {
            log.error("event_publish_failed publication=legacy exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                    LEGACY_CLINICAL_EXCHANGE, legacyRoutingKey, eventId, eventType, correlationId, ex);
        }
    }

    private static String correlationId(Object event) {
        return recordValue(event, "correlationId");
    }

    private static String eventId(Object event) {
        return recordValue(event, "eventId");
    }

    private static String timestamp(Object event) {
        return recordValue(event, "timestamp");
    }

    private static String eventType(Object event) {
        return recordValue(event, "eventType");
    }

    private static String recordValue(Object event, String componentName) {
        if (event == null || !event.getClass().isRecord()) return null;
        try {
            for (var component : event.getClass().getRecordComponents()) {
                if (component.getName().equals(componentName)) {
                    Object value = component.getAccessor().invoke(event);
                    return value == null ? null : value.toString();
                }
            }
        } catch (ReflectiveOperationException ex) {
            log.debug("Could not inspect event correlation id", ex);
        }
        return null;
    }
}
