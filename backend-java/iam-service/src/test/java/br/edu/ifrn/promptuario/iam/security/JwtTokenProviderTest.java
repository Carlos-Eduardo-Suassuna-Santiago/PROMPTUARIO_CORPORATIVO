package br.edu.ifrn.promptuario.iam.security;

import br.edu.ifrn.promptuario.iam.config.JwtProperties;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());

    @Test
    void generatesAccessTokenCompatibleWithSharedClaims() throws Exception {
        User user = user();

        String token = tokenProvider.generateAccessToken(user);
        SignedJWT parsed = SignedJWT.parse(token);

        assertThat(parsed.getHeader().getAlgorithm().getName()).isEqualTo("HS256");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("user-123");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("role")).isEqualTo("DOCTOR");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("email")).isEqualTo("doctor@promptuario.health");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("type")).isEqualTo("access");
        assertThat(parsed.getJWTClaimsSet().getIssueTime()).isNotNull();
        assertThat(parsed.getJWTClaimsSet().getExpirationTime()).isNotNull();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.extractUserId(token)).isEqualTo("user-123");
        assertThat(tokenProvider.extractRole(token)).isEqualTo("DOCTOR");
        assertThat(tokenProvider.isAccessToken(token)).isTrue();
    }

    @Test
    void generatesRefreshTokenWithUniqueJtiAndRejectsAlteredSignatures() throws Exception {
        String firstToken = tokenProvider.generateRefreshToken(user());
        String secondToken = tokenProvider.generateRefreshToken(user());

        assertThat(SignedJWT.parse(firstToken).getJWTClaimsSet().getStringClaim("type")).isEqualTo("refresh");
        assertThat(SignedJWT.parse(firstToken).getJWTClaimsSet().getJWTID()).isNotBlank()
                .isNotEqualTo(SignedJWT.parse(secondToken).getJWTClaimsSet().getJWTID());
        assertThat(tokenProvider.isAccessToken(firstToken)).isFalse();
        assertThat(tokenProvider.validateToken(firstToken + "invalid")).isFalse();
    }

    private static JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("12345678901234567890123456789012");
        properties.setAlgorithm("HS256");
        properties.setAccessTokenExpirationMinutes(30);
        properties.setRefreshTokenExpirationDays(7);
        return properties;
    }

    private static User user() {
        User user = new User("doctor@promptuario.health", "hash", "Dra. Ana", Role.DOCTOR);
        user.setId("user-123");
        return user;
    }
}
