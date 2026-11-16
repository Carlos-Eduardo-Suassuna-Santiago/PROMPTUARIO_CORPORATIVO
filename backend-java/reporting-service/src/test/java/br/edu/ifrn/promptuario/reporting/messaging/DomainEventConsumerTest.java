package br.edu.ifrn.promptuario.reporting.messaging;

import br.edu.ifrn.promptuario.reporting.repository.DailyStatsRepository;
import br.edu.ifrn.promptuario.reporting.repository.ProcessedDomainEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainEventConsumerTest {

    @Test
    void incrementsAppointmentMetricUsingScheduledDateAndDeduplicatesEvent() {
        DailyStatsRepository dailyStats = mock(DailyStatsRepository.class);
        ProcessedDomainEventRepository inbox = mock(ProcessedDomainEventRepository.class);
        when(inbox.insertIfAbsent("evt-appointment-1", "appointment.created")).thenReturn(1, 0);
        DomainEventConsumer consumer = new DomainEventConsumer(new ObjectMapper(), dailyStats, inbox);
        Message message = message("appointment.created", """
                {"eventId":"evt-appointment-1","eventType":"AppointmentCreated",
                 "timestamp":"2026-11-15T20:00:00Z","scheduledAt":"2026-11-17T01:30:00Z"}
                """);

        consumer.consume(message);
        consumer.consume(message);

        verify(dailyStats).incrementMetricAtomically(anyString(), eq("2026-11-17"), eq("CONSULTATIONS"), eq(null));
    }

    @Test
    void incrementsDoctorConsultationsForDoctorIdAndUtcEventDate() {
        DailyStatsRepository dailyStats = mock(DailyStatsRepository.class);
        ProcessedDomainEventRepository inbox = mock(ProcessedDomainEventRepository.class);
        when(inbox.insertIfAbsent("evt-record-1", "medical_record.created")).thenReturn(1);
        DomainEventConsumer consumer = new DomainEventConsumer(new ObjectMapper(), dailyStats, inbox);
        Message message = message("medical_record.created", """
                {"eventId":"evt-record-1","timestamp":"2026-11-15T23:30:00-03:00","doctorId":"doctor-9"}
                """);

        consumer.consume(message);

        verify(dailyStats).incrementMetricAtomically(anyString(), eq("2026-11-16"),
                eq("DOCTOR_CONSULTATIONS"), eq("doctor-9"));
    }

    @Test
    void ignoresUnsupportedEventsWithoutClaimingThem() {
        DailyStatsRepository dailyStats = mock(DailyStatsRepository.class);
        ProcessedDomainEventRepository inbox = mock(ProcessedDomainEventRepository.class);
        DomainEventConsumer consumer = new DomainEventConsumer(new ObjectMapper(), dailyStats, inbox);

        consumer.consume(message("appointment.updated", "{\"eventId\":\"evt-ignored\"}"));

        verify(inbox, never()).insertIfAbsent(anyString(), anyString());
        verify(dailyStats, never()).incrementMetricAtomically(anyString(), anyString(), anyString(), anyString());
    }

    private Message message(String routingKey, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
