package br.edu.ifrn.promptuario.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutingIntegrationTest extends GatewayIntegrationTestSupport {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void healthzReturnsGatewayStatus() {
        webTestClient.get()
                .uri("/healthz")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.service").isEqualTo("gateway");
    }

    @Test
    void healthzServicesChecksAllServicesAndLegacyAliases() {
        int iamBefore = IAM.getRequestCount();
        int patientBefore = PATIENT_CLINICAL.getRequestCount();
        int aiBefore = AI.getRequestCount();
        int reportingBefore = REPORTING.getRequestCount();

        webTestClient.get()
                .uri("/healthz/services")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.services.iam").isEqualTo("ok")
                .jsonPath("$.services.patient_clinical").isEqualTo("ok")
                .jsonPath("$.services.patient").isEqualTo("ok")
                .jsonPath("$.services.clinical").isEqualTo("ok")
                .jsonPath("$.services.ai").isEqualTo("ok")
                .jsonPath("$.services.reporting").isEqualTo("ok");

        assertThat(IAM.getRequestCount() - iamBefore).isEqualTo(1);
        assertThat(PATIENT_CLINICAL.getRequestCount() - patientBefore).isEqualTo(1);
        assertThat(AI.getRequestCount() - aiBefore).isEqualTo(1);
        assertThat(REPORTING.getRequestCount() - reportingBefore).isEqualTo(1);
    }

    @Test
    void publicIamLoginIsForwardedWithoutAuthorization() throws Exception {
        long requestsBefore = IAM.getRequestCount();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", "patient@example.test", "password", "test-password"))
                .exchange()
                .expectStatus().isOk();

        assertThat(IAM.getRequestCount()).isEqualTo(requestsBefore + 1);
        RecordedRequest forwarded = IAM.takeRequest(1, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getPath()).isEqualTo("/api/v1/auth/login");
        assertThat(forwarded.getHeader("Authorization")).isNull();
    }

    @Test
    void protectedPatientRouteRejectsMissingToken() {
        int requestsBefore = PATIENT_CLINICAL.getRequestCount();

        webTestClient.get()
                .uri("/api/v1/patients")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(PATIENT_CLINICAL.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    void validatedJwtClaimsAreForwardedToPatientService() throws Exception {
        String userId = "gateway-user-42";
        webTestClient.get()
                .uri("/api/v1/patients")
                .headers(headers -> headers.setBearerAuth(signedToken(userId, "DOCTOR")))
                .exchange()
                .expectStatus().isOk();

        RecordedRequest forwarded = PATIENT_CLINICAL.takeRequest(1, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeader("X-User-Id")).isEqualTo(userId);
        assertThat(forwarded.getHeader("X-User-Role")).isEqualTo("DOCTOR");
    }
}