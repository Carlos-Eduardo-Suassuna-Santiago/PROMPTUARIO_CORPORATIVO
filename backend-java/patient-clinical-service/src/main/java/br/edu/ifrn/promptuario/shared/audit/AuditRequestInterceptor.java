package br.edu.ifrn.promptuario.shared.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class AuditRequestInterceptor implements HandlerInterceptor {
    private final AuditService auditService;
    public AuditRequestInterceptor(AuditService auditService) { this.auditService = auditService; }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/healthz") || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger") || path.startsWith("/api/v1/audit")) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) return;
        try {
            auditService.record(path, "HTTP_" + request.getMethod(), null, authentication.getName(),
                    null, Map.of("status", response.getStatus()));
        } catch (RuntimeException ignored) {
            // Auditoria nunca deve alterar a resposta da operação original.
        }
    }
}
