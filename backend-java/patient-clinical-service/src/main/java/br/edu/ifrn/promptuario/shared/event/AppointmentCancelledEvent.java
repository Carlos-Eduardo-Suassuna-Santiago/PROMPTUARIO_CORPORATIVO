package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record AppointmentCancelledEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                        String appointmentId, String patientId, String reason) implements DomainEvent {
    public AppointmentCancelledEvent(String correlationId, String appointmentId, String patientId, String reason) {
        this(UUID.randomUUID(), Instant.now(), "appointment.cancelled", correlationId, appointmentId, patientId, reason);
    }
}
