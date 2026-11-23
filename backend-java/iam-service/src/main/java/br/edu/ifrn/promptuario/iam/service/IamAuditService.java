package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.model.AuditLog;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.repository.AuditLogRepository;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
public class IamAuditService {
    private final AuditLogRepository repository;
    private final UserRepository userRepository;
    public IamAuditService(AuditLogRepository repository, UserRepository userRepository) { this.repository = repository; this.userRepository = userRepository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String table, String operation, User target, Map<String, Object> oldValues,
                       Map<String, Object> newValues) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorId = auth == null ? null : auth.getName();
        String actorRole = auth == null ? null : auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse(null);
        String actorEmail = target == null ? null : target.getEmail();
        HttpServletRequest request = currentRequest();
        String ip = request == null ? null : first(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
        if (ip != null && ip.contains(",")) ip = ip.split(",", 2)[0].trim();
        repository.save(new AuditLog(table, operation, target == null ? null : target.getId(), actorId,
                actorRole, actorEmail, oldValues, newValues, ip,
                request == null ? null : request.getHeader("X-Request-Id"),
                request == null ? null : request.getHeader("X-Correlation-Id")));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequest(String path, String method, int status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return;
        User target = userRepository.findById(auth.getName()).orElse(null);
        record(path, "HTTP_" + method, target, null, Map.of("status", status));
    }

    private static String first(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v; return null; }
    private static HttpServletRequest currentRequest() { var a = RequestContextHolder.getRequestAttributes(); return a instanceof ServletRequestAttributes s ? s.getRequest() : null; }
}
