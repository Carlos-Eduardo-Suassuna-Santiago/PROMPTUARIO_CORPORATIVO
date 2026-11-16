package br.edu.ifrn.promptuario.reporting.messaging;

import br.edu.ifrn.promptuario.reporting.repository.DailyStatsRepository;
import br.edu.ifrn.promptuario.reporting.repository.ProcessedDomainEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final DailyStatsRepository dailyStatsRepository;
    private final ProcessedDomainEventRepository processedEvents;

    public DomainEventConsumer(ObjectMapper objectMapper,
                               DailyStatsRepository dailyStatsRepository,
                               ProcessedDomainEventRepository processedEvents) {
        this.objectMapper = objectMapper;
        this.dailyStatsRepository = dailyStatsRepository;
        this.processedEvents = processedEvents;
    }

    @RabbitListener(queues = "${app.messaging.events-queue:reporting.events.queue}")
    @Transactional
    public void consume(Message message) {
        final JsonNode payload;
        try {
            payload = objectMapper.readTree(message.getBody());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid domain event JSON", exception);
        }

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String eventType = resolveEventType(routingKey, firstText(payload, "eventType", "event_type"));
        Metric metric = metricFor(eventType);
        if (metric == null) {
            log.debug("Ignoring unsupported reporting event: routingKey={}, eventType={}", routingKey, eventType);
            return;
        }

        String eventId = firstHeader(message, "eventId", "event_id");
        if (eventId == null) {
            eventId = firstText(payload, "eventId", "event_id");
        }
        if (eventId == null || eventId.isBlank()) {
            eventId = message.getMessageProperties().getMessageId();
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Domain event has no eventId or messageId");
        }
        if (eventId.length() > 200) {
            throw new IllegalArgumentException("Domain event id exceeds 200 characters");
        }

        String entityId = metric.requiresEntityId() ? firstText(payload, "doctorId", "doctor_id") : null;
        if (metric.requiresEntityId() && (entityId == null || entityId.isBlank())) {
            throw new IllegalArgumentException("medical_record.created requires doctorId");
        }
        String statDate = resolveStatDate(payload, metric.dateFields());

        int inserted = processedEvents.insertIfAbsent(eventId, eventType);
        if (inserted == 0) {
            log.debug("Ignoring duplicate domain event: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        dailyStatsRepository.incrementMetricAtomically(UUID.randomUUID().toString(), statDate,
                metric.statType(), entityId);
        log.info("Updated daily metric from domain event: eventId={}, eventType={}, statType={}, statDate={}, entityId={}",
                eventId, eventType, metric.statType(), statDate, entityId);
    }

    private String resolveEventType(String routingKey, String bodyEventType) {
        if (routingKey != null && !routingKey.isBlank()) {
            return routingKey;
        }
        if (bodyEventType == null) {
            return null;
        }
        return switch (bodyEventType) {
            case "PatientCreated" -> "patient.created";
            case "AppointmentCreated" -> "appointment.created";
            case "AppointmentCancelled" -> "appointment.cancelled";
            case "MedicalRecordCreated" -> "medical_record.created";
            case "PrescriptionGenerated" -> "prescription.generated";
            default -> bodyEventType;
        };
    }

    private Metric metricFor(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case "patient.created", "promptuario.patient.created" ->
                    new Metric("NEW_PATIENTS", false, new String[]{"timestamp", "createdAt", "created_at"});
            case "appointment.created", "clinical.appointment.created" ->
                    new Metric("CONSULTATIONS", false,
                            new String[]{"scheduledAt", "scheduled_at", "timestamp"});
            case "appointment.cancelled", "clinical.appointment.cancelled" ->
                    new Metric("CANCELLATIONS", false, new String[]{"timestamp", "cancelledAt", "cancelled_at"});
            case "medical_record.created", "clinical.medical_record.created" ->
                    new Metric("DOCTOR_CONSULTATIONS", true, new String[]{"timestamp", "createdAt", "created_at"});
            case "prescription.generated", "clinical.prescription.generated" ->
                    new Metric("PRESCRIPTIONS", false, new String[]{"timestamp", "generatedAt", "generated_at"});
            default -> null;
        };
    }

    private String resolveStatDate(JsonNode payload, String[] dateFields) {
        for (String dateField : dateFields) {
            String value = firstText(payload, dateField);
            if (value != null && !value.isBlank()) {
                return toUtcDate(value).toString();
            }
        }
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    private LocalDate toUtcDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Continue with full ISO timestamp parsing.
        }
        try {
            return Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Some producers serialize offsets other than Z.
        }
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Domain event date must be ISO-8601: " + value, exception);
        }
    }

    private String firstHeader(Message message, String... names) {
        for (String name : names) {
            Object value = message.getMessageProperties().getHeaders().get(name);
            if (value != null) {
                return value instanceof byte[] bytes ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                        : String.valueOf(value);
            }
        }
        return null;
    }

    private String firstText(JsonNode payload, String... names) {
        for (String name : names) {
            JsonNode value = payload.get(name);
            if (value != null && !value.isNull() && value.isValueNode()) {
                return value.asText();
            }
        }
        return null;
    }

    private record Metric(String statType, boolean requiresEntityId, String[] dateFields) {
    }
}