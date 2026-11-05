package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record PrescriptionGeneratedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                         String prescriptionId, String recordId, String patientId) {
    public PrescriptionGeneratedEvent(String correlationId, String prescriptionId, String recordId, String patientId) {
        this(UUID.randomUUID(), Instant.now(), "prescription.generated", correlationId, prescriptionId, recordId, patientId);
    }
}
