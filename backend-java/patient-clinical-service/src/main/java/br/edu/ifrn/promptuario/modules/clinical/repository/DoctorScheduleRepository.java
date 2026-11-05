package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, String> {

    List<DoctorSchedule> findByDoctorId(String doctorId);

    List<DoctorSchedule> findByDoctorIdAndActiveTrue(String doctorId);
}
