package br.edu.ifrn.promptuario.modules.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Immutable
@Entity
@Table(name = "medical_record_history")
public class MedicalRecordHistory {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MedicalRecord record;

    @NotNull
    @Size(max = 36)
    @Column(name = "changed_by", length = 36, nullable = false)
    private String changedBy;

    @NotNull
    @Size(max = 50)
    @Column(name = "change_type", length = 50, nullable = false)
    private String changeType;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> snapshot = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MedicalRecordHistory() {
    }

    public MedicalRecordHistory(
            MedicalRecord record,
            String changedBy,
            String changeType,
            Map<String, Object> snapshot) {
        this.record = record;
        this.changedBy = changedBy;
        this.changeType = changeType;
        this.snapshot = new LinkedHashMap<>(snapshot);
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
