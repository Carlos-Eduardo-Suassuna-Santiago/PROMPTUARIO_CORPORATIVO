package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.config.JwtProperties;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void storesRevokedTokenWithRemainingJwtLifetime() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);

        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());
        String token = tokenProvider.generateAccessToken(user());
        TokenBlacklistService blacklistService = new TokenBlacklistService(redisTemplate, tokenProvider);

        blacklistService.blacklistToken(token);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(operations).set(org.mockito.ArgumentMatchers.eq("blacklist:" + token),
                org.mockito.ArgumentMatchers.eq("revoked"), ttl.capture());
        assertThat(ttl.getValue()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void checksBlacklistWithConstantTimeKeyLookup() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());
        TokenBlacklistService blacklistService = new TokenBlacklistService(redisTemplate, tokenProvider);
        when(redisTemplate.hasKey("blacklist:token")).thenReturn(true);

        assertThat(blacklistService.isBlacklisted("token")).isTrue();
        verify(redisTemplate).hasKey("blacklist:token");
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
