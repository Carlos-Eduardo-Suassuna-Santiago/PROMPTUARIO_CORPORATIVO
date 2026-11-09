package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponseDTO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        UserResponseDTO user
) {
    public TokenResponseDTO(String accessToken, String refreshToken, long expiresIn, UserResponseDTO user) {
        this(accessToken, refreshToken, "bearer", expiresIn, user);
    }
}
