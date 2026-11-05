package br.edu.ifrn.promptuario.iam.messaging;

import br.edu.ifrn.promptuario.iam.messaging.events.UserCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IamEventPublisherTest {

    @Test
    void publishesJsonEventWithTraceabilityHeaders() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        IamEventPublisher publisher = new IamEventPublisher(rabbitTemplate, objectMapper, "iam.events");

        publisher.publishUserCreated(new UserCreatedEvent(
                "user-123", "user@promptuario.health", "Usuário", "PATIENT", null, Instant.now()
        ));

        org.mockito.ArgumentCaptor<Message> message = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("iam.events"), eq("user.created"), message.capture());
        assertThat(message.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getValue().getMessageProperties().getHeaders())
                .containsEntry("eventType", "UserCreatedEvent")
                .containsKeys("eventId", "occurredAt", "correlationId");
    }
}
