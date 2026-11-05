package br.edu.ifrn.promptuario.iam.repository;

import br.edu.ifrn.promptuario.iam.model.OAuthAccount;
import br.edu.ifrn.promptuario.iam.model.PasswordResetToken;
import br.edu.ifrn.promptuario.iam.model.RefreshToken;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class IamPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("iam_db")
            .withUsername("iam")
            .withPassword("iam_pass");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void persistsEntitiesAndExecutesRepositoryQueries() {
        User doctor = new User("DOCTOR@PROMPTUARIO.HEALTH", "hash", "Dra. Ana", Role.DOCTOR);
        doctor.setCpf("123.456.789-00");
        User persistedUser = userRepository.saveAndFlush(doctor);

        RefreshToken refreshToken = refreshTokenRepository.saveAndFlush(
                new RefreshToken(persistedUser, "refresh-token-hash", Instant.now().plusSeconds(3_600))
        );
        PasswordResetToken resetToken = passwordResetRepository.saveAndFlush(
                new PasswordResetToken(persistedUser, "reset-token-hash", Instant.now().plusSeconds(3_600))
        );
        OAuthAccount oauthAccount = oAuthAccountRepository.saveAndFlush(
                new OAuthAccount(persistedUser, "google", "google-user-123", "doctor@promptuario.health")
        );

        assertThat(persistedUser.getId()).hasSize(36);
        assertThat(persistedUser.getEmail()).isEqualTo("doctor@promptuario.health");
        assertThat(userRepository.findByEmailIgnoreCase("DOCTOR@PROMPTUARIO.HEALTH")).contains(persistedUser);
        assertThat(userRepository.findByCpf("123.456.789-00")).contains(persistedUser);
        assertThat(userRepository.findByRoleAndIsActiveTrue(Role.DOCTOR)).contains(persistedUser);
        assertThat(refreshTokenRepository.findByTokenHashAndRevokedFalse(refreshToken.getTokenHash()))
                .contains(refreshToken);
        assertThat(passwordResetRepository.findByTokenHashAndUsedFalse(resetToken.getTokenHash())).contains(resetToken);
        assertThat(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-user-123"))
                .contains(oauthAccount);
        assertThat(oAuthAccountRepository.findByUserId(persistedUser.getId())).contains(oauthAccount);

        assertThat(refreshTokenRepository.revokeAllByUserId(persistedUser.getId())).isEqualTo(1);
        assertThat(refreshTokenRepository.findByTokenHashAndRevokedFalse(refreshToken.getTokenHash())).isEmpty();
    }
}
