package br.edu.ifrn.promptuario.gateway.filter;

import java.nio.charset.StandardCharsets;
import br.edu.ifrn.promptuario.gateway.config.PolicyAwareRedisRateLimiter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitErrorResponseGlobalFilter implements GlobalFilter, Ordered {

    private static final String RATE_LIMIT_BODY =
            "{\"detail\":\"Taxa de requisições excedida. Tente novamente em breve.\"}";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponseDecorator response = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> setComplete() {
                if (getStatusCode() != HttpStatus.TOO_MANY_REQUESTS
                        || !"true".equals(getHeaders().getFirst(
                                PolicyAwareRedisRateLimiter.RATE_LIMIT_REJECTED_HEADER))) {
                    return super.setComplete();
                }

                getHeaders().remove(PolicyAwareRedisRateLimiter.RATE_LIMIT_REJECTED_HEADER);
                getHeaders().setContentType(MediaType.APPLICATION_JSON);
                if (!getHeaders().containsKey("Retry-After")) {
                    getHeaders().set("Retry-After", "1");
                }
                byte[] body = RATE_LIMIT_BODY.getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = bufferFactory().wrap(body);
                return super.writeWith(Mono.just(buffer));
            }
        };

        return chain.filter(exchange.mutate().response(response).build());
    }
}