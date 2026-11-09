package br.edu.ifrn.promptuario.gateway.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record HealthStatusResponse(String status, Map<String, String> services) {

    public HealthStatusResponse {
        services = Collections.unmodifiableMap(new LinkedHashMap<>(services));
    }
}