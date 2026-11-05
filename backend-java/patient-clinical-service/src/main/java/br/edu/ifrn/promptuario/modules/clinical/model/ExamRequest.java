package br.edu.ifrn.promptuario.modules.clinical.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "exam_requests")
public class ExamRequest {

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
    @Size(max = 255)
    @Column(name = "exam_type", length = 255, nullable = false)
    private String examType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ExamUrgency urgency = ExamUrgency.ROUTINE;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(columnDefinition = "text")
    private String result;

    @Column(name = "result_date")
    private Instant resultDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamRequestHistory> history = new ArrayList<>();

    public ExamRequest() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
