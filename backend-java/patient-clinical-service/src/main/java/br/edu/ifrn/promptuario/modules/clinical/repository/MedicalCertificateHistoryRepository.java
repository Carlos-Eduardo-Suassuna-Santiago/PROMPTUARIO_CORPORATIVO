package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.MedicalCertificateHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalCertificateHistoryRepository extends JpaRepository<MedicalCertificateHistory, String> {

    Page<MedicalCertificateHistory> findByCertificate_IdOrderByCreatedAtDesc(
            String certificateId,
            Pageable pageable);
}
