package br.edu.ifrn.promptuario.shared.event;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedicalRecordCreatedEvent(@JsonAlias("event_id") UUID eventId,
                                        Instant timestamp,
                                        @JsonAlias("event_type") String eventType,
                                        @JsonAlias("correlation_id") String correlationId,
                                        @JsonAlias("record_id") String recordId,
                                        @JsonAlias("appointment_id") String appointmentId,
                                        @JsonAlias("patient_id") String patientId,
                                        @JsonAlias("doctor_id") String doctorId,
                                        @JsonAlias("chief_complaint") String chiefComplaint,
                                        @JsonAlias("diagnosis_codes") java.util.List<String> diagnosisCodes,
                                        @JsonAlias("prescriptions_count") int prescriptionsCount,
                                        @JsonAlias("exam_requests_count") int examRequestsCount) implements DomainEvent {
    public MedicalRecordCreatedEvent(String correlationId, String recordId, String appointmentId, String patientId) {
        this(UUID.randomUUID(), Instant.now(), "MedicalRecordCreated", correlationId, recordId, appointmentId,
                patientId, null, null, java.util.List.of(), 0, 0);
    }
}
