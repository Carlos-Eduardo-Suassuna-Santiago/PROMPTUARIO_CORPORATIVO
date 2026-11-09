package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record PatientCreatedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                  String patientId, String userId, String fullName) {
    public PatientCreatedEvent(String correlationId, String patientId, String userId, String fullName) {
        this(UUID.randomUUID(), Instant.now(), "patient.created", correlationId, patientId, userId, fullName);
    }
}
