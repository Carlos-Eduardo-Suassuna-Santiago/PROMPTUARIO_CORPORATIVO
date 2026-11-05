package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.PrescriptionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionHistoryRepository extends JpaRepository<PrescriptionHistory, String> {

    Page<PrescriptionHistory> findByPrescription_IdOrderByCreatedAtDesc(
            String prescriptionId,
            Pageable pageable);
}
