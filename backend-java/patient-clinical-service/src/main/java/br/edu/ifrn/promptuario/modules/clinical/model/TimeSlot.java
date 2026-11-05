package br.edu.ifrn.promptuario.modules.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DoctorSchedule schedule;

    @NotNull
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @NotBlank
    @Size(max = 5)
    @Column(name = "start_time", length = 5, nullable = false)
    private String startTime;

    @NotBlank
    @Size(max = 5)
    @Column(name = "end_time", length = 5, nullable = false)
    private String endTime;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TimeSlot() {
    }

    @jakarta.persistence.PrePersist
    private void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
