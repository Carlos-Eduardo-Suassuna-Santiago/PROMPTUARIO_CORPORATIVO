package br.edu.ifrn.promptuario.modules.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "prescriptions")
public class Prescription {

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
    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @NotNull
    @Size(max = 36)
    @Column(name = "doctor_id", length = 36, nullable = false)
    private String doctorId;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> medications = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String instructions;

    @Min(1)
    @Column(name = "valid_days", nullable = false)
    private int validDays = 30;

    @Size(max = 500)
    @Column(name = "pdf_s3_key", length = 500)
    private String pdfS3Key;

    @Column(name = "pdf_generated_at")
    private Instant pdfGeneratedAt;

    @Size(max = 64)
    @Column(name = "signature_hash", length = 64)
    private String signatureHash;

    @Size(max = 36)
    @Column(name = "signed_by", length = 36)
    private String signedBy;

    @Column(name = "signed_at")
    private Instant signedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Prescription() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
