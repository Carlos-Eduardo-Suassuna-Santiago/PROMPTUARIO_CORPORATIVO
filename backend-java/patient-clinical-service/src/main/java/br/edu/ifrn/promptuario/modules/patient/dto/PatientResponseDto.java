package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.GenderType;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PatientResponseDto(String id, String userId, String fullName, String cpf, LocalDate dateOfBirth,
                                 GenderType gender, String bloodType, String phone, String email, String street,
                                 String city, String state, String zipCode, String emergencyName,
                                 String emergencyPhone, String emergencyRelation, String notes, boolean active,
                                 boolean anonymized, Instant createdAt, Instant updatedAt,
                                 List<AllergyResponseDto> allergies, List<MedicationResponseDto> medications) {
    public static PatientResponseDto fromEntity(Patient e) {
        return new PatientResponseDto(e.getId(), e.getUserId(), e.getFullName(), e.getCpf(), e.getDateOfBirth(),
                e.getGender(), e.getBloodType(), e.getPhone(), e.getEmail(), e.getStreet(), e.getCity(), e.getState(),
                e.getZipCode(), e.getEmergencyName(), e.getEmergencyPhone(), e.getEmergencyRelation(), e.getNotes(),
                e.isActive(), e.isAnonymized(), e.getCreatedAt(), e.getUpdatedAt(),
                e.getAllergies().stream().map(AllergyResponseDto::fromEntity).toList(),
                e.getMedications().stream().map(MedicationResponseDto::fromEntity).toList());
    }
}
