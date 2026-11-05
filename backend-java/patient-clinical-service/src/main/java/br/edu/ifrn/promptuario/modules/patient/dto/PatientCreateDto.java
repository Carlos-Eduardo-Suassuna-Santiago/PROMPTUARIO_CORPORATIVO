package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.GenderType;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientCreateDto(
        @NotBlank @Size(min = 2, max = 255) String fullName,
        @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{11}$", message = "CPF inválido") String cpf,
        LocalDate dateOfBirth,
        GenderType gender,
        @Size(max = 5) String bloodType,
        String phone,
        @Email String email,
        String street, String city, @Size(max = 2) String state, String zipCode,
        String emergencyName, String emergencyPhone, String emergencyRelation,
        String notes,
        String userId) {

    public Patient toEntity() {
        Patient patient = new Patient();
        patient.setUserId(userId);
        patient.setFullName(fullName);
        patient.setCpf(cpf);
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setBloodType(bloodType);
        patient.setPhone(phone);
        patient.setEmail(email);
        patient.setStreet(street);
        patient.setCity(city);
        patient.setState(state);
        patient.setZipCode(zipCode);
        patient.setEmergencyName(emergencyName);
        patient.setEmergencyPhone(emergencyPhone);
        patient.setEmergencyRelation(emergencyRelation);
        patient.setNotes(notes);
        return patient;
    }
}
