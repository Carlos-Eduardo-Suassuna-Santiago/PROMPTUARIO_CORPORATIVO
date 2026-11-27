package br.edu.ifrn.promptuario.reporting.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    private final String serviceName;

    public HealthController(@Value("${app.service-name:reporting-service}") String serviceName) {
        this.serviceName = serviceName;
    }

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", serviceName);
    }
}
