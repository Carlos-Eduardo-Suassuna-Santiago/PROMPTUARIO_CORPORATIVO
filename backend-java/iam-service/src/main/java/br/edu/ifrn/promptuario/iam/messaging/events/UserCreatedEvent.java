package br.edu.ifrn.promptuario.iam.messaging.events;

import java.time.Instant;

public record UserCreatedEvent(
        String userId,
        String email,
        String fullName,
        String role,
        String cpf,
        Instant createdAt
) {
}
