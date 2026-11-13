package br.edu.ifrn.promptuario.shared.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.JacksonUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalEventContractTest {

    private final ObjectMapper objectMapper = JacksonUtils.enhancedObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void medicalRecordJsonMatchesThePythonContractFixture() throws IOException {
        MedicalRecordCreatedEvent event = new MedicalRecordCreatedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-01-15T12:30:45Z"),
                "MedicalRecordCreated",
                "corr-medical-001",
                "record-001",
                "appointment-001",
                "patient-001",
                "doctor-001",
                "Febre e cefaleia",
                List.of("R50.9", "R51.9"),
                1,
                2);

        JsonNode producedJson = objectMapper.valueToTree(event);
        assertThat(producedJson).isEqualTo(readFixture("medical-record-created.json"));
    }

    @Test
    void prescriptionJsonMatchesThePythonContractFixture() throws IOException {
        PrescriptionGeneratedEvent event = new PrescriptionGeneratedEvent(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-01-15T13:30:45Z"),
                "PrescriptionGenerated",
                "corr-prescription-001",
                "prescription-001",
                "record-001",
                "patient-001",
                "doctor-001",
                List.of(Map.of(
                        "name", "Dipirona",
                        "dosage", "500 mg",
                        "frequency", "8/8h")),
                "prescriptions/patient-001/prescription-001.pdf");

        JsonNode producedJson = objectMapper.valueToTree(event);
        assertThat(producedJson).isEqualTo(readFixture("prescription-generated.json"));
    }

    @Test
    void JavaRecordsAlsoAcceptLegacySnakeCaseAliases() throws IOException {
        JsonNode medicalFixture = readFixture("medical-record-created.json");
        String legacyJson = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("event_id", medicalFixture.get("eventId").asText()),
                Map.entry("timestamp", medicalFixture.get("timestamp").asText()),
                Map.entry("event_type", medicalFixture.get("eventType").asText()),
                Map.entry("correlation_id", medicalFixture.get("correlationId").asText()),
                Map.entry("record_id", medicalFixture.get("recordId").asText()),
                Map.entry("appointment_id", medicalFixture.get("appointmentId").asText()),
                Map.entry("patient_id", medicalFixture.get("patientId").asText()),
                Map.entry("doctor_id", medicalFixture.get("doctorId").asText()),
                Map.entry("chief_complaint", medicalFixture.get("chiefComplaint").asText()),
                Map.entry("diagnosis_codes", List.of("R50.9", "R51.9")),
                Map.entry("prescriptions_count", 1),
                Map.entry("exam_requests_count", 2)));

        MedicalRecordCreatedEvent parsed = objectMapper.readValue(legacyJson, MedicalRecordCreatedEvent.class);

        assertThat(parsed.eventId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(parsed.recordId()).isEqualTo("record-001");
        assertThat(parsed.patientId()).isEqualTo("patient-001");
        assertThat(parsed.correlationId()).isEqualTo("corr-medical-001");
    }

    private JsonNode readFixture(String name) throws IOException {
        try (var stream = getClass().getResourceAsStream("/contracts/" + name)) {
            if (stream == null) throw new IOException("Missing contract fixture: " + name);
            return objectMapper.readTree(stream);
        }
    }
}
