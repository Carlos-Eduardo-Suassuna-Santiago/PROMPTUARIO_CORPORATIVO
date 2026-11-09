package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.messaging.IamEventPublisher;
import br.edu.ifrn.promptuario.iam.messaging.events.UserDeactivatedEvent;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.repository.RefreshTokenRepository;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void deactivatesUserRevokesAllRefreshTokensAndPublishesDomainEvent() {
        User user = new User("patient@promptuario.health", "hash", "Paciente", Role.PATIENT);
        user.setId("patient-123");
        UserRepository users = mock(UserRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        IamEventPublisher eventPublisher = mock(IamEventPublisher.class);
        when(users.findById("patient-123")).thenReturn(Optional.of(user));
        UserService service = new UserService(users, refreshTokens, new BCryptPasswordEncoder(4), eventPublisher);

        service.deactivateUser("patient-123", "Solicitação administrativa", "admin-123");

        assertThat(user.isActive()).isFalse();
        assertThat(user.getDeactivatedAt()).isNotNull();
        assertThat(user.getDeactivationReason()).isEqualTo("Solicitação administrativa");
        verify(refreshTokens).revokeAllByUserId("patient-123");
        verify(eventPublisher).publishUserDeactivated(eq(new UserDeactivatedEvent(
                "patient-123", "Solicitação administrativa", "admin-123", user.getDeactivatedAt()
        )));
    }
}
