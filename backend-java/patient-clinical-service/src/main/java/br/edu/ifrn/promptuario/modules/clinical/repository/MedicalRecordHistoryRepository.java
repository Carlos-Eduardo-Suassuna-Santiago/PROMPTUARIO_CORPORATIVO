package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.MedicalRecordHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordHistoryRepository extends JpaRepository<MedicalRecordHistory, String> {

    Page<MedicalRecordHistory> findByRecord_IdOrderByCreatedAtDesc(String recordId, Pageable pageable);
}
