package br.edu.ifrn.promptuario.iam.repository;

import br.edu.ifrn.promptuario.iam.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> { }
