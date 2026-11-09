package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.GenderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientUpdateDto(
        @Size(min = 2, max = 255) String fullName,
        @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{11}$", message = "CPF inválido") String cpf,
        LocalDate dateOfBirth, GenderType gender, @Size(max = 5) String bloodType,
        String phone, @Email String email, String street, String city,
        @Size(max = 2) String state, String zipCode, String emergencyName,
        String emergencyPhone, String emergencyRelation, String notes) {
}
