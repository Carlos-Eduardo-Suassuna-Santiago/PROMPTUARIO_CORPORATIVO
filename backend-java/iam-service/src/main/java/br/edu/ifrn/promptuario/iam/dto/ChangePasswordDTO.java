package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDTO(
        @JsonProperty("current_password")
        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,
        @JsonProperty("new_password")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres")
        String newPassword
) {
}
