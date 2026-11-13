package br.edu.ifrn.promptuario.shared.event;

import java.time.Instant;

/** Common envelope required by every event published by this service. */
public interface DomainEvent {
    Object eventId();

    Instant timestamp();

    String eventType();

    String correlationId();
}
