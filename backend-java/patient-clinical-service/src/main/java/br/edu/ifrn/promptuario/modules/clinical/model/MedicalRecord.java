package br.edu.ifrn.promptuario.modules.clinical.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
import org.hibernate.annotations.UpdateTimestamp;
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
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Appointment appointment;

    @NotNull
    @Size(max = 36)
    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @NotNull
    @Size(max = 36)
    @Column(name = "doctor_id", length = 36, nullable = false)
    private String doctorId;

    @NotBlank
    @Column(name = "chief_complaint", columnDefinition = "text", nullable = false)
    private String chiefComplaint;

    @Column(columnDefinition = "text")
    private String anamnesis;

    @Column(name = "physical_exam", columnDefinition = "text")
    private String physicalExam;

    @Column(columnDefinition = "text")
    private String diagnosis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_codes", columnDefinition = "jsonb")
    private List<String> diagnosisCodes = new ArrayList<>();

    @Column(name = "treatment_plan", columnDefinition = "text")
    private String treatmentPlan;

    @Column(columnDefinition = "text")
    private String observations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rich_notes", columnDefinition = "jsonb")
    private Map<String, Object> richNotes = new LinkedHashMap<>();

    @Size(max = 64)
    @Column(name = "signature_hash", length = 64)
    private String signatureHash;

    @Size(max = 36)
    @Column(name = "signed_by", length = 36)
    private String signedBy;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Size(max = 36)
    @Column(name = "ai_analysis_id", length = 36)
    private String aiAnalysisId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> prescriptions = new ArrayList<>();

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamRequest> examRequests = new ArrayList<>();

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalCertificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalRecordHistory> history = new ArrayList<>();

    public MedicalRecord() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
