package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.config.JwtProperties;
import br.edu.ifrn.promptuario.iam.dto.RefreshRequestDTO;
import br.edu.ifrn.promptuario.iam.model.RefreshToken;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.repository.PasswordResetRepository;
import br.edu.ifrn.promptuario.iam.repository.RefreshTokenRepository;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import br.edu.ifrn.promptuario.iam.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void rotatesValidRefreshTokenAndRevokesThePreviousRecord() throws Exception {
        User user = user();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());
        String rawRefreshToken = tokenProvider.generateRefreshToken(user);
        RefreshToken storedToken = new RefreshToken(user, sha256(rawRefreshToken), Instant.now().plusSeconds(300));

        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        when(refreshTokens.findByTokenHashAndRevokedFalse(storedToken.getTokenHash()))
                .thenReturn(Optional.of(storedToken));
        AuthService service = new AuthService(
                mock(UserRepository.class),
                refreshTokens,
                mock(PasswordResetRepository.class),
                tokenProvider,
                mock(TokenBlacklistService.class),
                new BCryptPasswordEncoder(4)
        );

        var response = service.refresh(new RefreshRequestDTO(rawRefreshToken));

        assertThat(storedToken.isRevoked()).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo(rawRefreshToken);
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
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
