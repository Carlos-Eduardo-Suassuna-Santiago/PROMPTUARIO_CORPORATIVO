package br.edu.ifrn.promptuario.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtBlacklistGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REVOKED_TOKEN_BODY = "{\"detail\":\"Token revogado\"}";

    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtBlacklistGlobalFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return chain.filter(exchange);
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return chain.filter(exchange);
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        return rejectRevokedToken(exchange);
                    }
                    return exchange.getPrincipal()
                            .ofType(JwtAuthenticationToken.class)
                            .map(authentication -> withIdentityHeaders(exchange, authentication.getTokenAttributes()))
                            .defaultIfEmpty(exchange)
                            .flatMap(chain::filter);
                });
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange, Map<String, Object> claims) {
        var request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set("X-User-Id", claim(claims, "sub"));
                    headers.set("X-User-Role", claim(claims, "role"));
                    headers.set("X-User-Email", claim(claims, "email"));
                    headers.set("X-Gateway", "promptuario-gateway/1.0");
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private String claim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        return value == null ? "" : value.toString();
    }

    private Mono<Void> rejectRevokedToken(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = REVOKED_TOKEN_BODY.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}