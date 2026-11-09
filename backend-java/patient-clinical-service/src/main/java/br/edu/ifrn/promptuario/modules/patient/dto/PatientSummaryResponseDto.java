package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import java.time.LocalDate;
import java.util.List;

public record PatientSummaryResponseDto(String id, String userId, String fullName, LocalDate dateOfBirth,
                                        String bloodType, String phone, List<AllergyResponseDto> allergies,
                                        List<MedicationResponseDto> medications, java.time.Instant lastAppointmentAt) {
    public static PatientSummaryResponseDto fromEntity(Patient e) {
        return new PatientSummaryResponseDto(e.getId(), e.getUserId(), e.getFullName(), e.getDateOfBirth(),
                e.getBloodType(), e.getPhone(), e.getAllergies().stream().map(AllergyResponseDto::fromEntity).toList(),
                e.getMedications().stream().filter(m -> m.isActive()).map(MedicationResponseDto::fromEntity).toList(), null);
    }
}
