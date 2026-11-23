package br.edu.ifrn.promptuario.iam.config;

import br.edu.ifrn.promptuario.iam.service.IamAuditRequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebMvcConfig implements WebMvcConfigurer {
    private final IamAuditRequestInterceptor interceptor;
    public AuditWebMvcConfig(IamAuditRequestInterceptor interceptor) { this.interceptor = interceptor; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor); }
}
