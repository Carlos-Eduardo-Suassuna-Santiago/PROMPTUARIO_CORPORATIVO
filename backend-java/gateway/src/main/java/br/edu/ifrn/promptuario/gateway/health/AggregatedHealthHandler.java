package br.edu.ifrn.promptuario.gateway.health;

import br.edu.ifrn.promptuario.gateway.model.HealthStatusResponse;
import br.edu.ifrn.promptuario.gateway.properties.ServiceEndpointsProperties;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class AggregatedHealthHandler {

    private static final Duration SERVICE_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final ServiceEndpointsProperties services;

    public AggregatedHealthHandler(WebClient webClient, ServiceEndpointsProperties services) {
        this.webClient = webClient;
        this.services = services;
    }

    @GetMapping("/healthz")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "ok", "service", "gateway"));
    }

    @GetMapping("/healthz/services")
    public Mono<ResponseEntity<HealthStatusResponse>> servicesHealth() {
        Mono<String> iam = serviceStatus(services.iam());
        Mono<String> patientClinical = serviceStatus(services.patientClinical());
        Mono<String> ai = serviceStatus(services.ai());
        Mono<String> reporting = serviceStatus(services.reporting());

        return Mono.zip(iam, patientClinical, ai, reporting)
                .map(statuses -> aggregate(
                        statuses.getT1(),
                        statuses.getT2(),
                        statuses.getT3(),
                        statuses.getT4()));
    }

    private Mono<String> serviceStatus(URI serviceBaseUri) {
        URI healthUri = serviceBaseUri.resolve("/healthz");
        return webClient.get()
                .uri(healthUri)
                .exchangeToMono(response -> response.releaseBody()
                        .thenReturn(response.statusCode().value() == HttpStatus.OK.value() ? "ok" : "degraded"))
                .timeout(SERVICE_TIMEOUT)
                .onErrorReturn("unreachable");
    }

    private ResponseEntity<HealthStatusResponse> aggregate(
            String iam,
            String patientClinical,
            String ai,
            String reporting) {
        Map<String, String> statuses = new LinkedHashMap<>();
        statuses.put("iam", iam);
        statuses.put("patient_clinical", patientClinical);
        statuses.put("ai", ai);
        statuses.put("reporting", reporting);
        statuses.put("patient", patientClinical);
        statuses.put("clinical", patientClinical);

        String overallStatus = statuses.values().stream().allMatch("ok"::equals) ? "ok" : "degraded";
        return ResponseEntity.ok(new HealthStatusResponse(overallStatus, statuses));
    }
}