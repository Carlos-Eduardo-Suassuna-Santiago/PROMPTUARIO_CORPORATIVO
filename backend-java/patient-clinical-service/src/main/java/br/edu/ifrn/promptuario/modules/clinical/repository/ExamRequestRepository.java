package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.ExamRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRequestRepository extends JpaRepository<ExamRequest, String> {

    List<ExamRequest> findByRecord_IdOrderByCreatedAtDesc(String recordId);

    List<ExamRequest> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
