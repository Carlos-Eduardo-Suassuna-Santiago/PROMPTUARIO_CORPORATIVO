package br.edu.ifrn.promptuario.shared.audit;

import br.edu.ifrn.promptuario.shared.model.AuditLog;
import br.edu.ifrn.promptuario.shared.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public AuditLog record(String tableName, String operation, String recordId, String userId,
                           Map<String, Object> oldValues, Map<String, Object> newValues) {
        return repository.save(AuditLog.create("patient-clinical-service", tableName, operation,
                recordId, userId, oldValues, newValues));
    }
}
