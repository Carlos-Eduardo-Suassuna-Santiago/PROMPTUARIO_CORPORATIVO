package br.edu.ifrn.promptuario.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestTracingGlobalFilterTest {

    @Test
    void generatesAndPropagatesTraceHeadersAndRemoteAddress() throws Exception {
        RequestTracingGlobalFilter filter = new RequestTracingGlobalFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/patients")
                        .remoteAddress(new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 4321)));
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();
        GatewayFilterChain chain = request -> {
            downstreamExchange.set(request);
            return request.getResponse().setComplete();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String correlationId = downstreamExchange.get().getRequest().getHeaders().getFirst("X-Correlation-Id");
        String requestId = downstreamExchange.get().getRequest().getHeaders().getFirst("X-Request-Id");
        assertThat(correlationId).isNotBlank();
        assertThat(requestId).isEqualTo(correlationId);
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .isEqualTo("192.0.2.10");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isEqualTo(correlationId);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Request-Id")).isEqualTo(requestId);
    }

    @Test
    void usesClientProvidedCorrelationId() {
        RequestTracingGlobalFilter filter = new RequestTracingGlobalFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Correlation-Id", "client-trace-id"));
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();
        GatewayFilterChain chain = request -> {
            downstreamExchange.set(request);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst("X-Correlation-Id"))
                .isEqualTo("client-trace-id");
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst("X-Request-Id"))
                .isEqualTo("client-trace-id");
    }
}