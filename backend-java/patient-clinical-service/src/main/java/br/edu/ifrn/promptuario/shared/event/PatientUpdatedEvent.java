package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record PatientUpdatedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                  String patientId, String userId, String fullName) {
    public PatientUpdatedEvent(String correlationId, String patientId, String userId, String fullName) {
        this(UUID.randomUUID(), Instant.now(), "patient.updated", correlationId, patientId, userId, fullName);
    }
}
