package br.edu.ifrn.promptuario.gateway.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class ServiceFallbackControllerTest {

    @Test
    void returnsServiceUnavailableAndLogsTimeoutCause() {
        ServiceFallbackController controller = new ServiceFallbackController();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/fallback/ai"));
        exchange.getAttributes().put(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR,
                new TimeoutException("downstream timeout"));

        StepVerifier.create(controller.ai(exchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getHeaders().getFirst("X-Circuit-Breaker")).isEqualTo("open");
                    assertThat(response.getBody().detail()).isEqualTo("Serviço temporariamente indisponível");
                    assertThat(response.getBody().service()).isEqualTo("ai");
                    assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(Instant.now());
                })
                .verifyComplete();
    }

    @Test
    void returnsBadGatewayForRabbitManagementFailure() {
        ServiceFallbackController controller = new ServiceFallbackController();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/fallback/rabbitmq"));

        StepVerifier.create(controller.rabbitmq(exchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(response.getBody().service()).isEqualTo("rabbitmq-management");
                    assertThat(response.getBody().detail())
                            .isEqualTo("Interface de gerenciamento do RabbitMQ temporariamente indisponível");
                })
                .verifyComplete();
    }
}