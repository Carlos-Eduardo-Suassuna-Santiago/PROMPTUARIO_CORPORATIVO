package br.edu.ifrn.promptuario.shared.event;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserCreatedEvent(@JsonAlias("event_id") String eventId,
                               @JsonAlias("timestamp") Instant timestamp,
                               @JsonAlias("event_type") String eventType,
                               @JsonAlias("correlation_id") String correlationId,
                               @JsonAlias("user_id") String userId, String email,
                               @JsonAlias("full_name") String fullName, String role, String cpf, String phone) {
    public UserCreatedEvent(String correlationId, String userId, String email, String fullName, String role,
                            String cpf, String phone) {
        this(UUID.randomUUID().toString(), Instant.now(), "user.created", correlationId, userId, email, fullName, role, cpf, phone);
    }
}
