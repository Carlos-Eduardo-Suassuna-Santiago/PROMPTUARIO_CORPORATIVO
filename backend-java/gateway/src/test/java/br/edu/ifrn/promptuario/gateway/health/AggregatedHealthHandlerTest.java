package br.edu.ifrn.promptuario.gateway.health;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifrn.promptuario.gateway.model.HealthStatusResponse;
import br.edu.ifrn.promptuario.gateway.properties.ServiceEndpointsProperties;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AggregatedHealthHandlerTest {

    @Test
    void returnsLocalGatewayHealth() {
        AggregatedHealthHandler handler = new AggregatedHealthHandler(
                WebClient.builder().build(),
                endpoints());

        StepVerifier.create(handler.health())
                .expectNext(Map.of("status", "ok", "service", "gateway"))
                .verifyComplete();
    }

    @Test
    void aggregatesStatusesAndAddsPatientClinicalAliases() {
        WebClient webClient = webClientFor(host -> switch (host) {
            case "patient-clinical" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "reporting" -> HttpStatus.CREATED;
            default -> HttpStatus.OK;
        });
        AggregatedHealthHandler handler = new AggregatedHealthHandler(webClient, endpoints());

        StepVerifier.create(handler.servicesHealth())
                .assertNext(response -> {
                    HealthStatusResponse body = response.getBody();
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(body.status()).isEqualTo("degraded");
                    assertThat(body.services())
                            .containsEntry("iam", "ok")
                            .containsEntry("patient_clinical", "degraded")
                            .containsEntry("patient", "degraded")
                            .containsEntry("clinical", "degraded")
                            .containsEntry("ai", "ok")
                            .containsEntry("reporting", "degraded");
                    assertThatThrownByMutation(body);
                })
                .verifyComplete();
    }

    @Test
    void mapsServiceExceptionToUnreachableAndOverallDegraded() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> request.url().getHost().equals("ai")
                        ? Mono.error(new TimeoutException("health timeout"))
                        : Mono.just(ClientResponse.create(HttpStatus.OK).build()))
                .build();
        AggregatedHealthHandler handler = new AggregatedHealthHandler(webClient, endpoints());

        StepVerifier.create(handler.servicesHealth())
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().status()).isEqualTo("degraded");
                    assertThat(response.getBody().services()).containsEntry("ai", "unreachable");
                })
                .verifyComplete();
    }

    private WebClient webClientFor(java.util.function.Function<String, HttpStatus> statusForHost) {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(statusForHost.apply(request.url().getHost())).build());
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private ServiceEndpointsProperties endpoints() {
        return new ServiceEndpointsProperties(
                URI.create("http://iam:8000"),
                URI.create("http://patient-clinical:8000"),
                URI.create("http://ai:8000"),
                URI.create("http://reporting:8000"),
                URI.create("http://rabbitmq:15672"));
    }

    private void assertThatThrownByMutation(HealthStatusResponse response) {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> response.services().put("unexpected", "ok"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}