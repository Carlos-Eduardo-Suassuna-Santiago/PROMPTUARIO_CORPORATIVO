package br.edu.ifrn.promptuario.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import br.edu.ifrn.promptuario.gateway.config.PolicyAwareRedisRateLimiter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RateLimitErrorResponseGlobalFilterTest {

    @Test
    void writesFastApiCompatibleJsonWhenRateLimited() {
        RateLimitErrorResponseGlobalFilter filter = new RateLimitErrorResponseGlobalFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/patients"));
        GatewayFilterChain chain = currentExchange -> {
            currentExchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            currentExchange.getResponse().getHeaders().set("Retry-After", "3");
                currentExchange.getResponse().getHeaders().set(
                    PolicyAwareRedisRateLimiter.RATE_LIMIT_REJECTED_HEADER,
                    "true");
            return currentExchange.getResponse().setComplete();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("3");
        assertThat(exchange.getResponse().getHeaders())
            .doesNotContainKey(PolicyAwareRedisRateLimiter.RATE_LIMIT_REJECTED_HEADER);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString())
                .isEqualTo("application/json");
        StepVerifier.create(((MockServerHttpResponse) exchange.getResponse()).getBodyAsString())
                .expectNext("{\"detail\":\"Taxa de requisições excedida. Tente novamente em breve.\"}")
                .verifyComplete();
    }

    @Test
    void doesNotReplaceDownstreamTooManyRequests() {
        RateLimitErrorResponseGlobalFilter filter = new RateLimitErrorResponseGlobalFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/patients"));
        GatewayFilterChain chain = currentExchange -> {
            currentExchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return currentExchange.getResponse().setComplete();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isNull();
        StepVerifier.create(((MockServerHttpResponse) exchange.getResponse()).getBodyAsString())
            .expectNext("")
                .verifyComplete();
    }
}