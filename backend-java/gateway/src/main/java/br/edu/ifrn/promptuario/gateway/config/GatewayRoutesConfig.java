package br.edu.ifrn.promptuario.gateway.config;

import br.edu.ifrn.promptuario.gateway.properties.ServiceEndpointsProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            ServiceEndpointsProperties services) {
        return builder.routes()
                .route("iam-metrics", route -> route
                        .path("/metrics")
                        .uri(services.iam().toString()))
                .route("iam-public", route -> route
                        .path(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/register-patient",
                                "/api/v1/auth/oauth/**")
                        .uri(services.iam().toString()))
                .route("iam-protected", route -> route
                        .path("/api/v1/auth/**", "/api/v1/users/**")
                        .uri(services.iam().toString()))
                .route("patient-clinical", route -> route
                        .path(
                                "/api/v1/patients/**",
                                "/api/v1/appointments/**",
                                "/api/v1/schedules/**",
                                "/api/v1/records/**")
                        .uri(services.patientClinical().toString()))
                .route("ai-debug-public", route -> route
                        .path("/api/v1/ai/debug")
                        .uri(services.ai().toString()))
                .route("ai-protected", route -> route
                        .path("/api/v1/ai/**")
                        .uri(services.ai().toString()))
                .route("reporting", route -> route
                        .path("/api/v1/reports/**", "/api/v1/admin/**", "/api/v1/audit/**")
                        .uri(services.reporting().toString()))
                .route("rabbitmq-ui", route -> route
                        .path("/rabbitmq/**")
                        .filters(filters -> filters
                                .rewritePath("/rabbitmq/(?<segment>.*)", "/${segment}")
                                .removeRequestHeader(HttpHeaders.AUTHORIZATION))
                        .uri(services.rabbitmqManagement().toString()))
                .build();
    }
}