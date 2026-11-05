package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, String> {

    List<Prescription> findByRecord_IdOrderByCreatedAtDesc(String recordId);

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
