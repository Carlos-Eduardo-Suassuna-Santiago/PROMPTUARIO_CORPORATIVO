package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, String> {

    List<ReportSchedule> findByActiveTrue();
}