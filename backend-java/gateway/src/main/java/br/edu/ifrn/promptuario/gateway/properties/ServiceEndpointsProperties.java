package br.edu.ifrn.promptuario.gateway.properties;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services")
public record ServiceEndpointsProperties(
        URI iam,
        URI patientClinical,
        URI ai,
        URI reporting,
        URI rabbitmqManagement) {
}