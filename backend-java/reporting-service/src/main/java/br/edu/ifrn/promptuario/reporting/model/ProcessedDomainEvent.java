package br.edu.ifrn.promptuario.reporting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_domain_events")
public class ProcessedDomainEvent {

    @Id
    @Column(name = "event_id", length = 200, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    protected ProcessedDomainEvent() {
    }
}