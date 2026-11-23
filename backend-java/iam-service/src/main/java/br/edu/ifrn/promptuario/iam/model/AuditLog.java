package br.edu.ifrn.promptuario.iam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();
    @Column(name = "service_name", length = 50, nullable = false) private String serviceName;
    @Column(name = "table_name", length = 100, nullable = false) private String tableName;
    @Column(length = 30, nullable = false) private String operation;
    @Column(name = "record_id", length = 36) private String recordId;
    @Column(name = "user_id", length = 36) private String userId;
    @Column(name = "user_role", length = 20) private String userRole;
    @Column(name = "user_email", length = 255) private String userEmail;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "old_values", columnDefinition = "jsonb") private Map<String, Object> oldValues;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "new_values", columnDefinition = "jsonb") private Map<String, Object> newValues;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "request_id", length = 36) private String requestId;
    @Column(name = "correlation_id", length = 36) private String correlationId;
    @Column(name = "timestamp", nullable = false) private Instant timestamp = Instant.now();

    protected AuditLog() { }
    public AuditLog(String tableName, String operation, String recordId, String userId,
                    String userRole, String userEmail, Map<String, Object> oldValues,
                    Map<String, Object> newValues, String ipAddress, String requestId,
                    String correlationId) {
        this.serviceName = "iam-service"; this.tableName = tableName; this.operation = operation;
        this.recordId = recordId; this.userId = userId; this.userRole = userRole; this.userEmail = userEmail;
        this.oldValues = oldValues; this.newValues = newValues; this.ipAddress = ipAddress;
        this.requestId = requestId; this.correlationId = correlationId;
    }
}
