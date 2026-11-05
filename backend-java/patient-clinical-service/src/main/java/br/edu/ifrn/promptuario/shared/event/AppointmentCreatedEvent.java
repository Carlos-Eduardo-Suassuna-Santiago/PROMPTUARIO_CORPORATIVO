package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record AppointmentCreatedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                      String appointmentId, String patientId, String doctorId, Instant scheduledAt) {
    public AppointmentCreatedEvent(String correlationId, String appointmentId, String patientId, String doctorId, Instant scheduledAt) {
        this(UUID.randomUUID(), Instant.now(), "appointment.created", correlationId, appointmentId, patientId, doctorId, scheduledAt);
    }
}
