package br.edu.ifrn.promptuario.modules.patient.repository;

import br.edu.ifrn.promptuario.modules.patient.model.ContinuousMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContinuousMedicationRepository extends JpaRepository<ContinuousMedication, String> {

    List<ContinuousMedication> findByPatient_Id(String patientId);

    List<ContinuousMedication> findByPatient_IdAndActiveTrue(String patientId);
}
