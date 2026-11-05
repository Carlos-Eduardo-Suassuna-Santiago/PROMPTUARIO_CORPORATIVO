package br.edu.ifrn.promptuario.modules.patient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "medication_history")
public class MedicationHistory {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ContinuousMedication medication;

    @NotBlank
    @Size(max = 255)
    @Column(length = 255, nullable = false)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String dosage;

    @NotBlank
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String frequency;

    @Size(max = 255)
    @Column(name = "prescribing_doctor", length = 255)
    private String prescribingDoctor;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Size(max = 255)
    @Column(name = "end_reason", length = 255)
    private String endReason;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int version;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 20, nullable = false)
    private MedicationChangeType changeType;

    @Size(max = 255)
    @Column(name = "changed_by", length = 255)
    private String changedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MedicationHistory() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
