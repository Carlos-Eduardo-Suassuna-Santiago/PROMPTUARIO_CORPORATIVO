package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.ContinuousMedication;
import java.time.Instant;
import java.time.LocalDate;

public record MedicationResponseDto(String id, String patientId, String name, String dosage, String frequency,
                                    String route, String prescribingDoctor, LocalDate startedAt, LocalDate endedAt,
                                    String endReason, boolean active, int version, String notes,
                                    Instant createdAt, Instant updatedAt) {
    public static MedicationResponseDto fromEntity(ContinuousMedication e) {
        return new MedicationResponseDto(e.getId(), e.getPatient().getId(), e.getName(), e.getDosage(), e.getFrequency(),
                e.getRoute(), e.getPrescribingDoctor(), e.getStartedAt(), e.getEndedAt(), e.getEndReason(), e.isActive(),
                e.getVersion(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
