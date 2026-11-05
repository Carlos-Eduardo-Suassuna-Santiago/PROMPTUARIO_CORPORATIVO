package br.edu.ifrn.promptuario.iam.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void createsBcryptEncoderWithCostTwelve() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        String encoded = encoder.encode("senha-segura");

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoded).matches("^\\$2[aby]\\$12\\$.*");
        assertThat(encoder.matches("senha-segura", encoded)).isTrue();
    }
}
