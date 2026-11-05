package br.edu.ifrn.promptuario.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Immutable
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "ix_audit_logs_service_timestamp", columnList = "service_name, timestamp"),
        @Index(name = "ix_audit_logs_user_id", columnList = "user_id"),
        @Index(name = "ix_audit_logs_operation", columnList = "operation"),
        @Index(name = "ix_audit_logs_correlation_id", columnList = "correlation_id")
})
public class AuditLog {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "service_name", length = 50, nullable = false)
    private String serviceName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "table_name", length = 100, nullable = false)
    private String tableName;

    @NotBlank
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String operation;

    @Size(max = 36)
    @Column(name = "record_id", length = 36)
    private String recordId;

    @Size(max = 36)
    @Column(name = "user_id", length = 36)
    private String userId;

    @Size(max = 20)
    @Column(name = "user_role", length = 20)
    private String userRole;

    @Size(max = 255)
    @Column(name = "user_email", length = 255)
    private String userEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 36)
    @Column(name = "request_id", length = 36)
    private String requestId;

    @Size(max = 36)
    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    protected AuditLog() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
