package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJobRepository extends JpaRepository<ReportJob, String> {

    Page<ReportJob> findByRequestedByOrderByCreatedAtDesc(String requestedBy, Pageable pageable);

    Page<ReportJob> findByStatus(ReportStatus status, Pageable pageable);
}