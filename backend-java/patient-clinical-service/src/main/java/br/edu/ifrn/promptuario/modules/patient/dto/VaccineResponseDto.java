package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.Vaccine;
import java.time.Instant;
import java.time.LocalDate;

public record VaccineResponseDto(String id, String patientId, String name, String dose, String manufacturer,
                                 String lotNumber, LocalDate appliedAt, LocalDate nextDoseAt, String notes,
                                 Instant createdAt) {
    public static VaccineResponseDto fromEntity(Vaccine e) {
        return new VaccineResponseDto(e.getId(), e.getPatient().getId(), e.getName(), e.getDose(), e.getManufacturer(),
                e.getLotNumber(), e.getAppliedAt(), e.getNextDoseAt(), e.getNotes(), e.getCreatedAt());
    }
}
