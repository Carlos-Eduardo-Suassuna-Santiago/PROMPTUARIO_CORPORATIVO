package br.edu.ifrn.promptuario.shared.messaging;

import br.edu.ifrn.promptuario.shared.event.MedicalRecordCreatedEvent;
import br.edu.ifrn.promptuario.shared.event.PrescriptionGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonUtils;
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class EventPublisherTest {

    private final ObjectMapper objectMapper = JacksonUtils.enhancedObjectMapper();
    private final Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

    @Test
    void publishesEquivalentMessagesOnceToCanonicalAndLegacyDestinations() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        List<Publication> publications = capturePublications(rabbitTemplate);
        EventPublisher publisher = new EventPublisher(rabbitTemplate, oneAttempt(), true);
        MedicalRecordCreatedEvent event = event();

        publisher.publish("domain.events", "medical_record.created", event);

        assertThat(publications).hasSize(2);
        assertThat(publications).extracting(Publication::destination).containsExactly(
                "domain.events/medical_record.created",
                "promptuario.clinical/clinical.medical_record.created");
        assertThat(new HashSet<>(publications.stream().map(Publication::destination).toList())).hasSize(2);

        Message canonical = publications.get(0).toMessage(converter);
        Message legacy = publications.get(1).toMessage(converter);
        assertThat(objectMapper.readTree(canonical.getBody())).isEqualTo(objectMapper.readTree(legacy.getBody()));
        assertEnvelope(canonical, event);
        assertEnvelope(legacy, event);
    }

    @Test
    void featureFlagDisablesOnlyTheLegacyPublication() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        List<Publication> publications = capturePublications(rabbitTemplate);
        EventPublisher publisher = new EventPublisher(rabbitTemplate, oneAttempt(), false);

        publisher.publish("domain.events", "medical_record.created", event());

        assertThat(publications).extracting(Publication::destination)
                .containsExactly("domain.events/medical_record.created");
    }

    @Test
    void publishesPrescriptionToItsCanonicalAndLegacyRoutingKeys() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        List<Publication> publications = capturePublications(rabbitTemplate);
        EventPublisher publisher = new EventPublisher(rabbitTemplate, oneAttempt(), true);
        PrescriptionGeneratedEvent event = new PrescriptionGeneratedEvent(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-01-15T13:30:45Z"),
                "PrescriptionGenerated",
                "corr-prescription-001",
                "prescription-001",
                "record-001",
                "patient-001",
                "doctor-001",
                List.of(Map.of("name", "Dipirona")),
                "prescriptions/patient-001/prescription-001.pdf");

        publisher.publish("domain.events", "prescription.generated", event);

        assertThat(publications).extracting(Publication::destination).containsExactly(
                "domain.events/prescription.generated",
                "promptuario.clinical/clinical.prescription.generated");
    }

    private List<Publication> capturePublications(RabbitTemplate rabbitTemplate) {
        List<Publication> publications = new ArrayList<>();
        doAnswer(invocation -> {
            publications.add(new Publication(
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3)));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(), any(MessagePostProcessor.class));
        return publications;
    }

    private void assertEnvelope(Message message, MedicalRecordCreatedEvent event) {
        MessageProperties properties = message.getMessageProperties();
        assertThat(properties.getMessageId()).isEqualTo(event.eventId().toString());
        assertThat(properties.getCorrelationId()).isEqualTo(event.correlationId());
        assertThat(properties.getHeaders()).containsEntry("eventId", event.eventId().toString());
        assertThat(properties.getHeaders()).containsEntry("eventType", event.eventType());
        assertThat(properties.getHeaders()).containsEntry("timestamp", event.timestamp().toString());
        assertThat(properties.getHeaders()).containsEntry("correlationId", event.correlationId());
    }

    private RetryTemplate oneAttempt() {
        return RetryTemplate.builder().maxAttempts(1).noBackoff().build();
    }

    private MedicalRecordCreatedEvent event() {
        return new MedicalRecordCreatedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-01-15T12:30:45Z"),
                "MedicalRecordCreated",
                "corr-medical-001",
                "record-001",
                "appointment-001",
                "patient-001",
                "doctor-001",
                "Febre e cefaleia",
                List.of("R50.9", "R51.9"),
                1,
                2);
    }

    private record Publication(String exchange, String routingKey, Object event,
                               MessagePostProcessor messagePostProcessor) {
        String destination() {
            return exchange + "/" + routingKey;
        }

        Message toMessage(Jackson2JsonMessageConverter converter) throws Exception {
            Message message = converter.toMessage(event, new MessageProperties());
            return messagePostProcessor.postProcessMessage(message);
        }
    }
}
