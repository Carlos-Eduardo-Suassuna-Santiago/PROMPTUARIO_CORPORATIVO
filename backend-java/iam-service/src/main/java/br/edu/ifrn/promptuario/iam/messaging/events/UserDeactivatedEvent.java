package br.edu.ifrn.promptuario.iam.messaging.events;

import java.time.Instant;

public record UserDeactivatedEvent(
        String userId,
        String reason,
        String deactivatedBy,
        Instant deactivatedAt
) {
}
