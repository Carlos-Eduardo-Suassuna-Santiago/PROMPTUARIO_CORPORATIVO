package br.edu.ifrn.promptuario.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.security.Principal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.mockito.ArgumentCaptor;

class JwtBlacklistGlobalFilterTest {

    @Test
    void rejectsBlacklistedTokenWithoutCallingDownstream() {
        ReactiveStringRedisTemplate redisTemplate = org.mockito.Mockito.mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey("blacklist:revoked-token")).thenReturn(Mono.just(true));
        JwtBlacklistGlobalFilter filter = new JwtBlacklistGlobalFilter(redisTemplate);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer revoked-token"));
        AtomicReference<Boolean> downstreamCalled = new AtomicReference<>(false);
        GatewayFilterChain chain = request -> {
            downstreamCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(downstreamCalled.get()).isFalse();
        StepVerifier.create(((MockServerHttpResponse) exchange.getResponse()).getBodyAsString())
                .expectNext("{\"detail\":\"Token revogado\"}")
                .verifyComplete();
        verify(redisTemplate).hasKey("blacklist:revoked-token");
    }

    @Test
    void addsIdentityHeadersForValidatedActiveToken() {
        ReactiveStringRedisTemplate redisTemplate = org.mockito.Mockito.mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey("blacklist:active-token")).thenReturn(Mono.just(false));
        JwtBlacklistGlobalFilter filter = new JwtBlacklistGlobalFilter(redisTemplate);
        Jwt jwt = Jwt.withTokenValue("active-token")
                .header("alg", "HS256")
                .subject("user-123")
                .claim("role", "DOCTOR")
                .claim("email", "doctor@example.test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        ServerWebExchange exchange = org.mockito.Mockito.mock(ServerWebExchange.class);
        ServerWebExchange.Builder exchangeBuilder = org.mockito.Mockito.mock(ServerWebExchange.Builder.class);
        ServerWebExchange forwardedExchange = org.mockito.Mockito.mock(ServerWebExchange.class);
        ServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer active-token")
            .build();
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getPrincipal()).thenReturn(Mono.<Principal>just(authentication));
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(org.mockito.ArgumentMatchers.any(ServerHttpRequest.class)))
            .thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(forwardedExchange);
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();
        GatewayFilterChain chain = requestExchange -> {
            downstreamExchange.set(requestExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerHttpRequest> requestCaptor = ArgumentCaptor.forClass(ServerHttpRequest.class);
        verify(exchangeBuilder).request(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getHeaders())
                .containsEntry("X-User-Id", java.util.List.of("user-123"))
                .containsEntry("X-User-Role", java.util.List.of("DOCTOR"))
                .containsEntry("X-User-Email", java.util.List.of("doctor@example.test"))
                .containsEntry("X-Gateway", java.util.List.of("promptuario-gateway/1.0"));
        assertThat(downstreamExchange.get()).isSameAs(forwardedExchange);
    }
}