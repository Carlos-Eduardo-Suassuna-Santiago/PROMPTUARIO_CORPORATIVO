package br.edu.ifrn.promptuario.shared.audit;

import br.edu.ifrn.promptuario.shared.model.AuditLog;
import br.edu.ifrn.promptuario.shared.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public AuditLog record(String tableName, String operation, String recordId, String userId,
                           Map<String, Object> oldValues, Map<String, Object> newValues) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = authentication != null && authentication.getPrincipal() instanceof Jwt token
                ? token : null;

        String resolvedUserId = firstNonBlank(userId, claim(jwt, "user_id"), claim(jwt, "sub"));
        AuditLog log = AuditLog.create("patient-clinical-service", tableName, operation,
                recordId, resolvedUserId, oldValues, newValues);
        log.setUserEmail(firstNonBlank(claim(jwt, "email"), claim(jwt, "preferred_username")));
        log.setUserRole(firstNonBlank(claim(jwt, "role"), claim(jwt, "roles")));
        log.setIpAddress(requestIp());
        log.setRequestId(requestHeader("X-Request-Id"));
        log.setCorrelationId(requestHeader("X-Correlation-Id"));
        return repository.save(log);
    }

    private static String claim(Jwt jwt, String name) {
        if (jwt == null) return null;
        Object value = jwt.getClaims().get(name);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String requestHeader(String name) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest().getHeader(name);
    }

    private static String requestIp() {
        String forwarded = requestHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest().getRemoteAddr();
    }
}
