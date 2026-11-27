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

import java.util.Objects;


@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final RetryTemplate retryTemplate;
    public EventPublisher(RabbitTemplate rabbitTemplate,
                          RetryTemplate retryTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.retryTemplate = retryTemplate;
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

        try {
            retryTemplate.execute(context -> {
                rabbitTemplate.convertAndSend(exchange, routingKey, event, headers);
                return null;
            });
            log.info("event_published exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                    exchange, routingKey, eventId, eventType, correlationId);
        } catch (RuntimeException ex) {
            log.error("event_publish_failed exchange={} routingKey={} eventId={} eventType={} correlationId={}",
                    exchange, routingKey, eventId, eventType, correlationId, ex);
            throw ex;
        }
    }
}
