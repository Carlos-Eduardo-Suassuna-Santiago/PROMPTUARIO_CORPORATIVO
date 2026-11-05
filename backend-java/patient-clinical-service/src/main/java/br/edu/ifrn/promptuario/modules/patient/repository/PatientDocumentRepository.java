package br.edu.ifrn.promptuario.modules.patient.repository;

import br.edu.ifrn.promptuario.modules.patient.model.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientDocumentRepository extends JpaRepository<PatientDocument, String> {

    List<PatientDocument> findByPatient_IdAndActiveTrueOrderByCreatedAtDesc(String patientId);
}
