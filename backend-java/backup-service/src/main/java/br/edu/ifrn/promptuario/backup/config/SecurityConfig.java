package br.edu.ifrn.promptuario.backup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Configuração de segurança do {@code backup-service}.
 *
 * <p>Libera endpoints de saúde e métricas (sem autenticação) e protege a API
 * operacional de backups sob o prefixo {@code /api/v1/backups/**}, exigindo
 * o papel {@code ADMIN} via Spring Security OAuth2 Resource Server (JWT).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Define as regras de autorização do serviço.
     *
     * <ul>
     *   <li>{@code GET /healthz}, {@code GET /actuator/health}, {@code GET /actuator/prometheus}:
     *       livres (sem autenticação).</li>
     *   <li>Qualquer outro endpoint: autenticado, com {@code ROLE_ADMIN} obrigatório para
     *       {@code /api/v1/backups/**}.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/healthz").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/backups/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/backups/execute").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/backups/restore").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    /**
     * Converte as autoridades (roles) do JWT para o formato {@code ROLE_<escopo>},
     * de modo que {@code hasRole("ADMIN")} funcione com o claim {@code scope} ou
     * {@code roles} do token emitido pelo servidor de autorização.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
