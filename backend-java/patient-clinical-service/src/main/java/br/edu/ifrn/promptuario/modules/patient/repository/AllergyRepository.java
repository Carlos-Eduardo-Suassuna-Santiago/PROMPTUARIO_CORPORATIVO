package br.edu.ifrn.promptuario.modules.patient.repository;

import br.edu.ifrn.promptuario.modules.patient.model.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllergyRepository extends JpaRepository<Allergy, String> {

    List<Allergy> findByPatient_Id(String patientId);
}
