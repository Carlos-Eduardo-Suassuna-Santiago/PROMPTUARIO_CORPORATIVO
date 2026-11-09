package br.edu.ifrn.promptuario.iam.dto;

import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserResponseDTO(
        String id,
        String email,
        @JsonProperty("full_name") String fullName,
        Role role,
        String cpf,
        @JsonProperty("is_active") boolean isActive,
        @JsonProperty("is_verified") boolean isVerified,
        @JsonProperty("two_factor_enabled") boolean twoFactorEnabled,
        @JsonProperty("created_at") Instant createdAt
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCpf(),
                user.isActive(),
                user.isVerified(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt()
        );
    }
}
