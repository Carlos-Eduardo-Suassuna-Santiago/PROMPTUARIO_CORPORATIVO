package br.edu.ifrn.promptuario.iam.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IamAuditRequestInterceptor implements HandlerInterceptor {
    private final IamAuditService auditService;
    public IamAuditRequestInterceptor(IamAuditService auditService) { this.auditService = auditService; }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/healthz") || path.startsWith("/api/v1/audit")) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return;
        try { auditService.recordRequest(path, request.getMethod(), response.getStatus()); }
        catch (RuntimeException ignored) { }
    }
}
