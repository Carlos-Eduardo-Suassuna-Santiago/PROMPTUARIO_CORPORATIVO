package br.edu.ifrn.promptuario.modules.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "medical_certificates")
public class MedicalCertificate {

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

    @NotBlank
    @Column(columnDefinition = "text", nullable = false)
    private String reason;

    @Min(0)
    @Column(name = "days_off", nullable = false)
    private int daysOff;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Size(max = 255)
    @Column(name = "pdf_s3_key", length = 255)
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

    public MedicalCertificate() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
