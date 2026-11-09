package br.edu.ifrn.promptuario.iam.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
public class TwoFactorService {

    private static final String ISSUER = "Promptuario";

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public String generateSecretKey() {
        return googleAuthenticator.createCredentials().getKey();
    }

    public String getQrCodeUrl(String email, String secret) {
        if (email == null || email.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("E-mail e segredo TOTP são obrigatórios");
        }
        String label = UriUtils.encodePathSegment(ISSUER + ":" + email.trim(), StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + UriUtils.encodeQueryParam(secret, StandardCharsets.UTF_8)
                + "&issuer=" + ISSUER;
    }

    public boolean verifyCode(String secret, int code) {
        return secret != null && !secret.isBlank() && code >= 0 && code <= 999_999
                && googleAuthenticator.authorize(secret, code);
    }
}
