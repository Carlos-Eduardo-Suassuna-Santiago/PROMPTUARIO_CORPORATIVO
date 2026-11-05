package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRegisterDTO(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password,
        @JsonProperty("full_name")
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(min = 2, max = 255, message = "Nome completo deve ter entre 2 e 255 caracteres")
        String fullName,
        @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{11}$", message = "CPF inválido")
        String cpf,
        @JsonProperty("date_of_birth") LocalDate dateOfBirth,
        String gender,
        String phone
) {
}
