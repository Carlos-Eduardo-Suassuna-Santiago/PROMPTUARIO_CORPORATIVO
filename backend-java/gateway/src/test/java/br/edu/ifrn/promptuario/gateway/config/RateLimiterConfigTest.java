package br.edu.ifrn.promptuario.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RateLimiterConfigTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    void resolvesApiKeyToSha256BeforeOtherIdentity() throws Exception {
        String apiKey = "test-api-key";
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Api-Key", apiKey));
        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8)));

        StepVerifier.create(config.userOrIpKeyResolver().resolve(exchange))
                .expectNext("apikey:" + expectedHash)
                .verifyComplete();
    }

    @Test
    void resolvesAuthenticatedPrincipalByName() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user-123",
                null,
                List.of());

        StepVerifier.create(config.userOrIpKeyResolver().resolve(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .expectNext("user:user-123")
                .verifyComplete();
    }

    @Test
    void resolvesAnonymousClientByFirstForwardedIp() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Forwarded-For", "203.0.113.8, 10.0.0.1"));

        StepVerifier.create(config.userOrIpKeyResolver().resolve(exchange))
                .expectNext("ip:203.0.113.8")
                .verifyComplete();
    }

    @Test
    void choosesAnonymousBucketAndCalculatesRetryAfterFromRemainingTokens() {
        RedisRateLimiter anonymous = mock(RedisRateLimiter.class);
        RedisRateLimiter authenticated = mock(RedisRateLimiter.class);
        RedisRateLimiter apiKey = mock(RedisRateLimiter.class);
        when(anonymous.getRemainingHeader()).thenReturn(RedisRateLimiter.REMAINING_HEADER);
        when(anonymous.isAllowed("anonymous-ip", "ip:203.0.113.8"))
                .thenReturn(Mono.just(new RateLimiter.Response(
                        false,
                        java.util.Map.of(RedisRateLimiter.REMAINING_HEADER, "30"))));
        PolicyAwareRedisRateLimiter rateLimiter = new PolicyAwareRedisRateLimiter(
                anonymous,
                authenticated,
                apiKey);

        StepVerifier.create(rateLimiter.isAllowed("some-route", "ip:203.0.113.8"))
                .assertNext(response -> {
                    assertThat(response.isAllowed()).isFalse();
                    assertThat(response.getHeaders()).containsEntry("Retry-After", "1");
                    assertThat(response.getHeaders())
                            .containsEntry(PolicyAwareRedisRateLimiter.RATE_LIMIT_REJECTED_HEADER, "true");
                })
                .verifyComplete();

        verify(anonymous).isAllowed("anonymous-ip", "ip:203.0.113.8");
    }

    @Test
    void selectsAuthenticatedAndApiKeyBucketsByKeyPrefix() {
        RedisRateLimiter anonymous = mock(RedisRateLimiter.class);
        RedisRateLimiter authenticated = mock(RedisRateLimiter.class);
        RedisRateLimiter apiKey = mock(RedisRateLimiter.class);
        when(authenticated.isAllowed("authenticated-user", "user:user-123"))
                .thenReturn(Mono.just(new RateLimiter.Response(true, java.util.Map.of())));
        when(apiKey.isAllowed("api-key", "apikey:sha256-digest"))
                .thenReturn(Mono.just(new RateLimiter.Response(true, java.util.Map.of())));
        PolicyAwareRedisRateLimiter rateLimiter = new PolicyAwareRedisRateLimiter(
                anonymous,
                authenticated,
                apiKey);

        StepVerifier.create(rateLimiter.isAllowed("iam-protected", "user:user-123"))
                .expectNextMatches(RateLimiter.Response::isAllowed)
                .verifyComplete();
        StepVerifier.create(rateLimiter.isAllowed("patient-clinical", "apikey:sha256-digest"))
                .expectNextMatches(RateLimiter.Response::isAllowed)
                .verifyComplete();

        verify(authenticated).isAllowed("authenticated-user", "user:user-123");
        verify(apiKey).isAllowed("api-key", "apikey:sha256-digest");
    }
}