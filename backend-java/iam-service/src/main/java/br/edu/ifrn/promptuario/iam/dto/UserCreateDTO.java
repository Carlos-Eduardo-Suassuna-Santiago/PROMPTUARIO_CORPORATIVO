package br.edu.ifrn.promptuario.iam.dto;

import br.edu.ifrn.promptuario.iam.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
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
        @NotNull(message = "Papel é obrigatório")
        Role role,
        String cpf
) {
}
