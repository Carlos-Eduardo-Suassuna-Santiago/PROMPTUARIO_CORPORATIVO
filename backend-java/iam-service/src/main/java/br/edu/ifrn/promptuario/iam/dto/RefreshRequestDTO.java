package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
        @JsonProperty("refresh_token")
        @NotBlank(message = "Refresh token é obrigatório")
        String refreshToken
) {
}
