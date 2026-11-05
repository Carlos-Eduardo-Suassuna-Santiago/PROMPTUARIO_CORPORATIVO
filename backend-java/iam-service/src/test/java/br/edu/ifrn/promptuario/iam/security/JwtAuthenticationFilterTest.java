package br.edu.ifrn.promptuario.iam.security;

import br.edu.ifrn.promptuario.iam.config.JwtProperties;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.service.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidAccessTokenWithRoleAuthority() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(jwtProperties());
        String token = provider.generateAccessToken(user());
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, blacklistService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-123");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_DOCTOR");
    }

    @Test
    void rejectsRevokedOrRefreshToken() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(jwtProperties());
        String revokedToken = provider.generateAccessToken(user());
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        when(blacklistService.isBlacklisted(revokedToken)).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, blacklistService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + revokedToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
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
