package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.Appointment;
import br.edu.ifrn.promptuario.modules.clinical.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    @Query(value = """
            select a.* from appointments a
            left join patients p on p.id = a.patient_id
            where (coalesce(cast(:patientId as varchar), '') = '' or a.patient_id = cast(:patientId as varchar))
              and (coalesce(cast(:doctorId as varchar), '') = '' or a.doctor_id = cast(:doctorId as varchar))
              and (coalesce(cast(:status as varchar), '') = '' or a.status = cast(:status as varchar))
              and (cast(:rangeStart as timestamptz) is null or a.scheduled_at >= cast(:rangeStart as timestamptz))
              and (cast(:rangeEnd as timestamptz) is null or a.scheduled_at <= cast(:rangeEnd as timestamptz))
              and (coalesce(cast(:patientName as varchar), '') = '' or lower(p.full_name) like lower(concat('%', cast(:patientName as varchar), '%')))
            """, nativeQuery = true,
            countQuery = """
            select count(*) from appointments a
            left join patients p on p.id = a.patient_id
            where (coalesce(cast(:patientId as varchar), '') = '' or a.patient_id = cast(:patientId as varchar))
              and (coalesce(cast(:doctorId as varchar), '') = '' or a.doctor_id = cast(:doctorId as varchar))
              and (coalesce(cast(:status as varchar), '') = '' or a.status = cast(:status as varchar))
              and (cast(:rangeStart as timestamptz) is null or a.scheduled_at >= cast(:rangeStart as timestamptz))
              and (cast(:rangeEnd as timestamptz) is null or a.scheduled_at <= cast(:rangeEnd as timestamptz))
              and (coalesce(cast(:patientName as varchar), '') = '' or lower(p.full_name) like lower(concat('%', cast(:patientName as varchar), '%')))
            """)
    Page<Appointment> searchAppointments(
            @Param("patientId") String patientId,
            @Param("doctorId") String doctorId,
            @Param("status") String status,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("patientName") String patientName,
            Pageable pageable);

    boolean existsByDoctorIdAndScheduledAtAndStatusNot(
            String doctorId,
            Instant scheduledAt,
            AppointmentStatus status);

    List<Appointment> findByPatient_IdAndScheduledAtAfterAndStatusIn(
            String patientId, Instant scheduledAt, Collection<AppointmentStatus> statuses);
}
