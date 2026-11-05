package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @JsonProperty("full_name")
        @Size(min = 2, max = 255, message = "Nome completo deve ter entre 2 e 255 caracteres")
        String fullName,
        @Email(message = "E-mail inválido")
        String email,
        String cpf
) {
}
