package br.edu.ifrn.promptuario.shared.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String exchange, String routingKey, Object event) {
        String correlationId = correlationId(event);
        MessagePostProcessor headers = message -> {
            message.getMessageProperties().setHeader("correlationId", correlationId);
            message.getMessageProperties().setHeader("timestamp", Instant.now().toString());
            message.getMessageProperties().setHeader("eventType", routingKey);
            String eventId = eventId(event);
            if (eventId != null) message.getMessageProperties().setMessageId(eventId);
            return message;
        };
        rabbitTemplate.convertAndSend(exchange, routingKey, event, headers);
        log.debug("Domain event published: exchange={}, routingKey={}, correlationId={}", exchange, routingKey, correlationId);
    }

    private static String correlationId(Object event) {
        return recordValue(event, "correlationId");
    }

    private static String eventId(Object event) {
        return recordValue(event, "eventId");
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
