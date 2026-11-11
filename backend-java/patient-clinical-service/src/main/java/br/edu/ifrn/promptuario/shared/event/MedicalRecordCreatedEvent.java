package br.edu.ifrn.promptuario.shared.event;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
public record MedicalRecordCreatedEvent(UUID eventId, Instant timestamp, String eventType, String correlationId,
                                        String recordId, String appointmentId, String patientId, String doctorId,
                                        String chiefComplaint, java.util.List<String> diagnosisCodes,
                                        int prescriptionsCount, int examRequestsCount) {
    @JsonProperty("event_id") public UUID legacyEventId() { return eventId; }
    @JsonProperty("event_type") public String legacyEventType() { return eventType; }
    @JsonProperty("correlation_id") public String legacyCorrelationId() { return correlationId; }
    @JsonProperty("record_id") public String legacyRecordId() { return recordId; }
    @JsonProperty("appointment_id") public String legacyAppointmentId() { return appointmentId; }
    @JsonProperty("patient_id") public String legacyPatientId() { return patientId; }
    @JsonProperty("doctor_id") public String legacyDoctorId() { return doctorId; }
    @JsonProperty("chief_complaint") public String legacyChiefComplaint() { return chiefComplaint; }
    @JsonProperty("diagnosis_codes") public java.util.List<String> legacyDiagnosisCodes() { return diagnosisCodes; }
    @JsonProperty("prescriptions_count") public int legacyPrescriptionsCount() { return prescriptionsCount; }
    @JsonProperty("exam_requests_count") public int legacyExamRequestsCount() { return examRequestsCount; }

    public MedicalRecordCreatedEvent(String correlationId, String recordId, String appointmentId, String patientId) {
        this(UUID.randomUUID(), Instant.now(), "MedicalRecordCreated", correlationId, recordId, appointmentId,
                patientId, null, null, java.util.List.of(), 0, 0);
    }
}
