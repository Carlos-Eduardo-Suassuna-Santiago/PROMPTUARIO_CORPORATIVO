package br.edu.ifrn.promptuario.modules.patient.repository;

import br.edu.ifrn.promptuario.modules.patient.model.Vaccine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccineRepository extends JpaRepository<Vaccine, String> {

    List<Vaccine> findByPatient_Id(String patientId);
}
