package br.edu.ifrn.promptuario.shared.event;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
public record PrescriptionGeneratedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                         String prescriptionId, String recordId, String patientId, String doctorId,
                                         java.util.List<java.util.Map<String, Object>> medications,
                                         String pdfS3Key) {
    @JsonProperty("event_id") public UUID legacyEventId() { return eventId; }
    @JsonProperty("event_type") public String legacyEventType() { return eventType; }
    @JsonProperty("correlation_id") public String legacyCorrelationId() { return correlationId; }
    @JsonProperty("prescription_id") public String legacyPrescriptionId() { return prescriptionId; }
    @JsonProperty("record_id") public String legacyRecordId() { return recordId; }
    @JsonProperty("patient_id") public String legacyPatientId() { return patientId; }
    @JsonProperty("doctor_id") public String legacyDoctorId() { return doctorId; }
    @JsonProperty("pdf_s3_key") public String legacyPdfS3Key() { return pdfS3Key; }

    public PrescriptionGeneratedEvent(String correlationId, String prescriptionId, String recordId, String patientId) {
        this(UUID.randomUUID(), Instant.now(), "PrescriptionGenerated", correlationId, prescriptionId, recordId,
                patientId, null, java.util.List.of(), null);
    }
}
