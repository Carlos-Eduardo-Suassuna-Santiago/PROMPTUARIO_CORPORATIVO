package br.edu.ifrn.promptuario.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@Tag(name = "Health")
public class HealthController {
    @GetMapping("/healthz")
    @Operation(summary = "Service health check")
    public Map<String,String> health(){return Map.of("status","ok","service","patient-clinical-service","version","1.0.0-java");}
}
