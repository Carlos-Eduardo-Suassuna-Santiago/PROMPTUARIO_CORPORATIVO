package br.edu.ifrn.promptuario.modules.patient.repository;

import br.edu.ifrn.promptuario.modules.patient.model.MedicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationHistoryRepository extends JpaRepository<MedicationHistory, String> {

    List<MedicationHistory> findByPatient_IdOrderByCreatedAtDesc(String patientId);
}
