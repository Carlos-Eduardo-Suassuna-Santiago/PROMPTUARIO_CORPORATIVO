package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserDeactivatedEvent(@JsonAlias("event_id") String eventId,
                                   Instant timestamp,
                                   @JsonAlias("event_type") String eventType,
                                   @JsonAlias("correlation_id") String correlationId,
                                   @JsonAlias("user_id") String userId, String reason) {
    public UserDeactivatedEvent(String correlationId, String userId, String reason) {
        this(UUID.randomUUID().toString(), Instant.now(), "user.deactivated", correlationId, userId, reason);
    }
}
