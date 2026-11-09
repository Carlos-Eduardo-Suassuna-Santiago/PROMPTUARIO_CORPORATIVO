package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
public record MedicalRecordCreatedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                        String recordId, String appointmentId, String patientId) {
    public MedicalRecordCreatedEvent(String correlationId, String recordId, String appointmentId, String patientId) {
        this(UUID.randomUUID(), Instant.now(), "medical_record.created", correlationId, recordId, appointmentId, patientId);
    }
}
