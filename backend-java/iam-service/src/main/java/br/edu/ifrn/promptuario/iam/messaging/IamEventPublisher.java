package br.edu.ifrn.promptuario.iam.messaging;

import br.edu.ifrn.promptuario.iam.messaging.events.UserCreatedEvent;
import br.edu.ifrn.promptuario.iam.messaging.events.UserDeactivatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class IamEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;

    public IamEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${app.rabbitmq.exchange:iam.events}") String exchange
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
    }

    public void publishUserCreated(UserCreatedEvent event) {
        publish("user.created", event);
    }

    public void publishUserDeactivated(UserDeactivatedEvent event) {
        publish("user.deactivated", event);
    }

    private void publish(String routingKey, Object event) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setHeader("eventType", event.getClass().getSimpleName());
            properties.setHeader("eventId", UUID.randomUUID().toString());
            properties.setHeader("occurredAt", Instant.now().toString());
            properties.setHeader("correlationId", correlationId());

            Message message = new Message(objectMapper.writeValueAsBytes(event), properties);
            rabbitTemplate.send(exchange, routingKey, message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar o evento IAM", ex);
        }
    }

    private String correlationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
    }
}
