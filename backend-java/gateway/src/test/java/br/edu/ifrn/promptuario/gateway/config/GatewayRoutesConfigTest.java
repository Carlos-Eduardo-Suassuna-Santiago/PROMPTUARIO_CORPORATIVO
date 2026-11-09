package br.edu.ifrn.promptuario.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class GatewayRoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

        @Autowired
        private WebEndpointsSupplier webEndpointsSupplier;

        @Autowired
        private WebEndpointProperties webEndpointProperties;

        @Autowired
        private ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory;

    @Test
    void registersRoutesWithExpectedServiceDestinations() {
        StepVerifier.create(routeLocator.getRoutes().collectMap(Route::getId, Route::getUri))
                .assertNext(routes -> {
                    assertThat(routes).containsKeys(
                            "iam-metrics",
                            "iam-public",
                            "iam-protected",
                            "patient-clinical",
                            "ai-debug-public",
                            "ai-protected",
                            "reporting",
                            "rabbitmq-ui");
                    assertThat(routes.get("iam-public")).isEqualTo(URI.create("http://iam-service:8000"));
                    assertThat(routes.get("patient-clinical"))
                            .isEqualTo(URI.create("http://patient-clinical-service:8000"));
                    assertThat(routes.get("ai-protected")).isEqualTo(URI.create("http://ai-service:8000"));
                    assertThat(routes.get("reporting"))
                            .isEqualTo(URI.create("http://reporting-service:8000"));
                    assertThat(routes.get("rabbitmq-ui"))
                            .isEqualTo(URI.create("http://guest:guest@rabbitmq:15672"));
                })
                .verifyComplete();
    }

        @Test
        void routePredicatesMatchRepresentativeApiPaths() {
                StepVerifier.create(routeLocator.getRoutes().collectMap(Route::getId))
                                .assertNext(routes -> {
                                        assertMatches(routes.get("iam-public"), "/api/v1/auth/login");
                                        assertMatches(routes.get("iam-protected"), "/api/v1/users/me");
                                        assertMatches(routes.get("patient-clinical"), "/api/v1/records/123");
                                        assertMatches(routes.get("ai-debug-public"), "/api/v1/ai/debug");
                                        assertMatches(routes.get("ai-protected"), "/api/v1/ai/analyze");
                                        assertMatches(routes.get("reporting"), "/api/v1/audit/events");
                                        assertMatches(routes.get("rabbitmq-ui"), "/rabbitmq/");
                                })
                                .verifyComplete();
        }

        @Test
        void appliesRateLimiterToApiRoutes() {
                StepVerifier.create(routeLocator.getRoutes().collectMap(Route::getId))
                                .assertNext(routes -> {
                                            assertThat(routes.get("iam-public").getFilters()).hasSize(2);
                                            assertThat(routes.get("iam-protected").getFilters()).hasSize(2);
                                            assertThat(routes.get("patient-clinical").getFilters()).hasSize(2);
                                            assertThat(routes.get("ai-debug-public").getFilters()).hasSize(2);
                                            assertThat(routes.get("ai-protected").getFilters()).hasSize(2);
                                            assertThat(routes.get("reporting").getFilters()).hasSize(2);
                                            assertThat(routes.get("iam-metrics").getFilters()).hasSize(1);
                                        assertThat(routes.get("rabbitmq-ui").getFilters()).hasSize(2);
                                })
                                .verifyComplete();
        }

        @Test
        void exposesConfiguredActuatorEndpoints() {
                assertThat(webEndpointProperties.getExposure().getInclude())
                        .contains("health", "info", "prometheus", "gateway");
                        assertThat(webEndpointsSupplier.getEndpoints())
                                .extracting(endpoint -> endpoint.getEndpointId().toString())
                                .contains("health", "info", "prometheus");
        }

    @Test
    void configuresCircuitBreakerThresholdsAndServiceTimeouts() {
        assertCircuitBreaker("patientClinicalCircuitBreaker", 30);
        assertCircuitBreaker("reportingCircuitBreaker", 30);
        assertCircuitBreaker("iamCircuitBreaker", 30);
        assertCircuitBreaker("aiCircuitBreaker", 60);
    }

    private void assertCircuitBreaker(String name, long timeoutSeconds) {
        StepVerifier.create(circuitBreakerFactory.create(name)
                        .run(Mono.just("probe"), Mono::error))
                .expectNext("probe")
                .verifyComplete();
        CircuitBreaker circuitBreaker = circuitBreakerFactory.getCircuitBreakerRegistry()
                .find(name)
                .orElseThrow();
        TimeLimiter timeLimiter = circuitBreakerFactory.getTimeLimiterRegistry()
                .find(name)
                .orElseThrow();

        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(3);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
                .isEqualTo(2);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
                .isEqualTo(30_000L);
        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration())
                .isEqualTo(java.time.Duration.ofSeconds(timeoutSeconds));
    }

        private void assertMatches(Route route, String path) {
                ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
                StepVerifier.create(route.getPredicate().apply(exchange))
                                .expectNext(true)
                                .verifyComplete();
        }
}