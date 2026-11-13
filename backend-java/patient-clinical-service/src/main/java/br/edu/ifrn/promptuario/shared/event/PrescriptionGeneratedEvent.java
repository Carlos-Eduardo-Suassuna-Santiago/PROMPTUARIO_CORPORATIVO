package br.edu.ifrn.promptuario.shared.event;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrescriptionGeneratedEvent(@JsonAlias("event_id") UUID eventId,
                                         Instant timestamp,
                                         @JsonAlias("event_type") String eventType,
                                         @JsonAlias("correlation_id") String correlationId,
                                         @JsonAlias("prescription_id") String prescriptionId,
                                         @JsonAlias("record_id") String recordId,
                                         @JsonAlias("patient_id") String patientId,
                                         @JsonAlias("doctor_id") String doctorId,
                                         java.util.List<java.util.Map<String, Object>> medications,
                                         @JsonAlias("pdf_s3_key") String pdfS3Key) implements DomainEvent {
    public PrescriptionGeneratedEvent(String correlationId, String prescriptionId, String recordId, String patientId) {
        this(UUID.randomUUID(), Instant.now(), "PrescriptionGenerated", correlationId, prescriptionId, recordId,
                patientId, null, java.util.List.of(), null);
    }
}
