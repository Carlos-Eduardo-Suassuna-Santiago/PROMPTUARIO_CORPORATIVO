package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.ReportAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAuditLogRepository extends JpaRepository<ReportAuditLog, String> {

    Page<ReportAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId, Pageable pageable);
}