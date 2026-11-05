package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, String> {

    List<TimeSlot> findBySchedule_Id(String scheduleId);

    List<TimeSlot> findBySchedule_DoctorIdAndSlotDateAndAvailableTrue(
            String doctorId,
            LocalDate slotDate);
}
