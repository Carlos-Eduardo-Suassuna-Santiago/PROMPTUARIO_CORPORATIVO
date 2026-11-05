package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.security.JwtTokenProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist:";
    private static final String REVOKED_VALUE = "revoked";

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenBlacklistService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void blacklistToken(String token) {
        Instant expiration = jwtTokenProvider.extractExpiration(token);
        Duration ttl = Duration.between(Instant.now(), expiration);

        if (!ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, REVOKED_VALUE, ttl);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
