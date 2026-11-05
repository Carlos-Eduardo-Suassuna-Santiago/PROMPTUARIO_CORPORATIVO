package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.MedicalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalCertificateRepository extends JpaRepository<MedicalCertificate, String> {

    List<MedicalCertificate> findByRecord_IdOrderByCreatedAtDesc(String recordId);
}
