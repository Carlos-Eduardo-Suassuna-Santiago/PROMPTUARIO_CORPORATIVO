package br.edu.ifrn.promptuario.gateway.fallback;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Instant;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class ServiceFallbackController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFallbackController.class);
    private static final String DETAIL = "Serviço temporariamente indisponível";

    @RequestMapping(path = "/patient-clinical", produces = "application/json")
    public Mono<ResponseEntity<FallbackResponse>> patientClinical(ServerWebExchange exchange) {
        return unavailable("patient-clinical", exchange);
    }

    @RequestMapping(path = "/ai", produces = "application/json")
    public Mono<ResponseEntity<FallbackResponse>> ai(ServerWebExchange exchange) {
        return unavailable("ai", exchange);
    }

    @RequestMapping(path = "/reporting", produces = "application/json")
    public Mono<ResponseEntity<FallbackResponse>> reporting(ServerWebExchange exchange) {
        return unavailable("reporting", exchange);
    }

    @RequestMapping(path = "/iam", produces = "application/json")
    public Mono<ResponseEntity<FallbackResponse>> iam(ServerWebExchange exchange) {
        return unavailable("iam", exchange);
    }

    @RequestMapping(path = "/rabbitmq", produces = "application/json")
    public Mono<ResponseEntity<FallbackResponse>> rabbitmq(ServerWebExchange exchange) {
        return unavailable(
                "rabbitmq-management",
                "Interface de gerenciamento do RabbitMQ temporariamente indisponível",
                HttpStatus.BAD_GATEWAY,
                exchange);
    }

    private Mono<ResponseEntity<FallbackResponse>> unavailable(String service, ServerWebExchange exchange) {
        return unavailable(service, DETAIL, HttpStatus.SERVICE_UNAVAILABLE, exchange);
    }

    private Mono<ResponseEntity<FallbackResponse>> unavailable(
            String service,
            String detail,
            HttpStatus status,
            ServerWebExchange exchange) {
        Throwable originalException = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        Throwable cause = unwrap(originalException);
        String exceptionType = cause == null ? "unknown" : cause.getClass().getSimpleName();
        String exceptionMessage = cause == null ? "" : cause.getMessage();
        String reason = "downstream_failure";
        if (cause instanceof CallNotPermittedException) {
            reason = "circuit_open";
        } else if (cause instanceof TimeoutException
            || cause instanceof org.springframework.cloud.gateway.support.TimeoutException) {
            reason = "timeout";
        }
        LOGGER.warn(
            "circuit_breaker_fallback service={} reason={} exception_type={} exception_message={}",
            service,
            reason,
            exceptionType,
            exceptionMessage);

        FallbackResponse body = new FallbackResponse(detail, service, Instant.now());
        return Mono.just(ResponseEntity.status(status)
                .header("X-Circuit-Breaker", "open")
                .body(body));
    }

    private Throwable unwrap(Throwable exception) {
        if (exception == null) {
            return null;
        }
        Throwable current = Exceptions.unwrap(exception);
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    public record FallbackResponse(String detail, String service, Instant timestamp) {
    }
}