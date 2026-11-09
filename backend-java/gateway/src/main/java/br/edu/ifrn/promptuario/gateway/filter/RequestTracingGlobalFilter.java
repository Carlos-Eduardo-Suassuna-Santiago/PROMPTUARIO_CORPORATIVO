package br.edu.ifrn.promptuario.gateway.filter;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestTracingGlobalFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID = "X-Correlation-Id";
    private static final String REQUEST_ID = "X-Request-Id";
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders incomingHeaders = exchange.getRequest().getHeaders();
        String requestId = nonBlank(incomingHeaders.getFirst(REQUEST_ID));
        String correlationId = nonBlank(incomingHeaders.getFirst(CORRELATION_ID));
        String traceId = correlationId != null
                ? correlationId
                : requestId != null ? requestId : UUID.randomUUID().toString();
        String resolvedRequestId = requestId != null ? requestId : traceId;
        String clientIp = clientIp(exchange);

        var request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(CORRELATION_ID, traceId);
                    headers.set(REQUEST_ID, resolvedRequestId);
                    headers.set(FORWARDED_FOR, clientIp);
                })
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(request).build();
        tracedExchange.getResponse().beforeCommit(() -> {
            HttpHeaders responseHeaders = tracedExchange.getResponse().getHeaders();
            responseHeaders.set(CORRELATION_ID, traceId);
            responseHeaders.set(REQUEST_ID, resolvedRequestId);
            return Mono.empty();
        });

        return chain.filter(tracedExchange);
    }

    private String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwardedFor = nonBlank(exchange.getRequest().getHeaders().getFirst(FORWARDED_FOR));
        if (forwardedFor != null) {
            String firstAddress = forwardedFor.split(",", 2)[0].trim();
            if (!firstAddress.isBlank()) {
                return firstAddress;
            }
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return "unknown";
        }
        return remoteAddress.getAddress() == null
                ? remoteAddress.getHostString()
                : remoteAddress.getAddress().getHostAddress();
    }
}