package br.edu.ifrn.promptuario.config;

import br.edu.ifrn.promptuario.shared.audit.AuditRequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebMvcConfig implements WebMvcConfigurer {
    private final AuditRequestInterceptor interceptor;
    public AuditWebMvcConfig(AuditRequestInterceptor interceptor) { this.interceptor = interceptor; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor); }
}
