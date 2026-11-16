package br.edu.ifrn.promptuario.reporting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Immutable
@Entity
@Table(name = "report_audit_logs")
public class ReportAuditLog {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Size(max = 40)
    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @NotBlank
    @Size(max = 30)
    @Column(name = "entity_type", length = 30, nullable = false)
    private String entityType;

    @Size(max = 36)
    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Size(max = 500)
    @Column(length = 500)
    private String description = "";

    @NotBlank
    @Size(max = 36)
    @Column(name = "performed_by", length = 36, nullable = false)
    private String performedBy;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_metadata", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventMetadata = new LinkedHashMap<>();

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReportAuditLog() {
    }

    public ReportAuditLog(String eventType, String entityType, String entityId,
                         String description, String performedBy,
                         Map<String, Object> eventMetadata, String ipAddress) {
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description == null ? "" : description;
        this.performedBy = performedBy;
        this.eventMetadata = eventMetadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(eventMetadata);
        this.ipAddress = ipAddress;
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}