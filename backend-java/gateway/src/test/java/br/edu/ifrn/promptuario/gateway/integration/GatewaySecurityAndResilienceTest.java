package br.edu.ifrn.promptuario.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityAndResilienceTest extends GatewayIntegrationTestSupport {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Test
    void blacklistedJwtReturns401WithoutCallingDownstream() {
        String token = signedToken("blacklisted-" + UUID.randomUUID(), "PATIENT");
        StepVerifier.create(redisTemplate.opsForValue().set("blacklist:" + token, "1"))
                .expectNext(true)
                .verifyComplete();
        int requestsBefore = PATIENT_CLINICAL.getRequestCount();

        webTestClient.get()
                .uri("/api/v1/patients")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Token revogado");

        assertThat(PATIENT_CLINICAL.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    void anonymousRequestsOverLimitReturn429AndRetryAfter() {
        String clientIp = "198.51.100." + (10 + Math.floorMod(UUID.randomUUID().hashCode(), 200));
        int rateLimited = 0;

        for (int requestNumber = 0; requestNumber < 35; requestNumber++) {
            EntityExchangeResult<byte[]> result = webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .header("X-Forwarded-For", clientIp)
                    .bodyValue("{}")
                    .exchange()
                    .expectBody()
                    .returnResult();

            if (result.getStatus().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                rateLimited++;
                assertThat(result.getResponseHeaders().getFirst("Retry-After")).isNotBlank();
                assertThat(new String(result.getResponseBody(), StandardCharsets.UTF_8))
                        .contains("Taxa de requisições excedida");
            } else {
                assertThat(result.getStatus().is2xxSuccessful()).isTrue();
            }
        }

        assertThat(rateLimited).isPositive();
    }

    @Test
    void downstreamServerErrorsOpenCircuitAndReturn503Fallback() throws Exception {
        REPORTING.setDispatcher(new okhttp3.mockwebserver.QueueDispatcher());
        String token = signedToken("reporting-circuit-" + UUID.randomUUID(), "ADMIN");

        int requestsBefore = REPORTING.getRequestCount();
        for (int attempt = 0; attempt < 3; attempt++) {
            REPORTING.enqueue(new MockResponse().setResponseCode(503).setBody("downstream unavailable"));
            webTestClient.get()
                    .uri("/api/v1/reports/resilience-check")
                    .headers(headers -> headers.setBearerAuth(token))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        assertThat(REPORTING.getRequestCount()).isEqualTo(requestsBefore + 3);
        EntityExchangeResult<byte[]> fallback = webTestClient.get()
                .uri("/api/v1/reports/resilience-check")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().valueEquals("X-Circuit-Breaker", "open")
                .expectBody()
                .jsonPath("$.service").isEqualTo("reporting")
                .returnResult();

        RecordedRequest forwarded = REPORTING.takeRequest(1, TimeUnit.SECONDS);
        assertThat(fallback.getResponseBody()).isNotNull();
        assertThat(forwarded).isNotNull();
        assertThat(REPORTING.getRequestCount()).isEqualTo(requestsBefore + 3);
    }
}