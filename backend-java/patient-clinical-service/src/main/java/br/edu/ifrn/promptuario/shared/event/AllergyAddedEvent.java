package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record AllergyAddedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                String patientId, String allergyId, String substance, String severity) {
    public AllergyAddedEvent(String correlationId, String patientId, String allergyId, String substance, String severity) {
        this(UUID.randomUUID(), Instant.now(), "patient.allergy.added", correlationId, patientId, allergyId, substance, severity);
    }
}
