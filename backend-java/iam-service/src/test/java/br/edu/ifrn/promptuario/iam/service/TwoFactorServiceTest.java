package br.edu.ifrn.promptuario.iam.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorServiceTest {

    @Test
    void generatesTotpSecretQrUriAndValidatesCurrentCode() {
        TwoFactorService service = new TwoFactorService();
        String secret = service.generateSecretKey();
        int code = new GoogleAuthenticator().getTotpPassword(secret);

        assertThat(secret).isNotBlank();
        assertThat(service.getQrCodeUrl("user@promptuario.health", secret))
                .startsWith("otpauth://totp/Promptuario:user@promptuario.health?secret=" + secret)
                .contains("issuer=Promptuario");
        assertThat(service.verifyCode(secret, code)).isTrue();
        assertThat(service.verifyCode(secret, 1_000_000)).isFalse();
    }
}
