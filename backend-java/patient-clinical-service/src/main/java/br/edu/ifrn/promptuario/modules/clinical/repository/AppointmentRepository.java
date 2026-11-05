package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.Appointment;
import br.edu.ifrn.promptuario.modules.clinical.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    @Query(value = """
            select a from Appointment a
            where (:patientId is null or a.patient.id = :patientId)
              and (:doctorId is null or a.doctorId = :doctorId)
              and (:status is null or a.status = :status)
              and (:rangeStart is null or a.scheduledAt >= :rangeStart)
              and (:rangeEnd is null or a.scheduledAt <= :rangeEnd)
            """,
            countQuery = """
            select count(a) from Appointment a
            where (:patientId is null or a.patient.id = :patientId)
              and (:doctorId is null or a.doctorId = :doctorId)
              and (:status is null or a.status = :status)
              and (:rangeStart is null or a.scheduledAt >= :rangeStart)
              and (:rangeEnd is null or a.scheduledAt <= :rangeEnd)
            """)
    Page<Appointment> searchAppointments(
            @Param("patientId") String patientId,
            @Param("doctorId") String doctorId,
            @Param("status") AppointmentStatus status,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            Pageable pageable);

    boolean existsByDoctorIdAndScheduledAtAndStatusNot(
            String doctorId,
            Instant scheduledAt,
            AppointmentStatus status);
}
