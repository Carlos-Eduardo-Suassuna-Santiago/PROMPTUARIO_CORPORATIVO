package br.edu.ifrn.promptuario.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateUserDTO(
        @NotBlank(message = "Motivo da desativação é obrigatório")
        @Size(min = 5, max = 255, message = "Motivo da desativação deve ter entre 5 e 255 caracteres")
        String reason
) {
}
