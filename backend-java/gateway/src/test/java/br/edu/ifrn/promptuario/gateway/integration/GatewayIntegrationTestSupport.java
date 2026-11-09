package br.edu.ifrn.promptuario.gateway.integration;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.GenericContainer;

abstract class GatewayIntegrationTestSupport {

    protected static final String JWT_SECRET = "gateway-integration-test-signing-secret-at-least-32-bytes";
    protected static final MockWebServer IAM = startMockWebServer();
    protected static final MockWebServer PATIENT_CLINICAL = startMockWebServer();
    protected static final MockWebServer AI = startMockWebServer();
    protected static final MockWebServer REPORTING = startMockWebServer();
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        REDIS.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown(IAM);
            shutdown(PATIENT_CLINICAL);
            shutdown(AI);
            shutdown(REPORTING);
            REDIS.stop();
        }, "gateway-integration-test-dependencies-shutdown"));
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("app.security.jwt.secret", () -> JWT_SECRET);
        registry.add("app.services.iam", () -> serviceUrl(IAM));
        registry.add("app.services.patient-clinical", () -> serviceUrl(PATIENT_CLINICAL));
        registry.add("app.services.ai", () -> serviceUrl(AI));
        registry.add("app.services.reporting", () -> serviceUrl(REPORTING));
    }

    @BeforeEach
    void resetDownstreamMocks() throws InterruptedException {
        reset(IAM);
        reset(PATIENT_CLINICAL);
        reset(AI);
        reset(REPORTING);
    }

    protected String signedToken(String userId, String role) {
        try {
            com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("role", role)
                    .claim("email", userId + "@example.test")
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                    .build();
            com.nimbusds.jwt.SignedJWT jwt = new com.nimbusds.jwt.SignedJWT(
                    new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),
                    claims);
            jwt.sign(new com.nimbusds.jose.crypto.MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (com.nimbusds.jose.JOSEException exception) {
            throw new IllegalStateException("Could not sign integration JWT", exception);
        }
    }

    private static MockWebServer startMockWebServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String serviceUrl(MockWebServer server) {
        return server.url("/").toString().replaceAll("/$", "");
    }

    private static void reset(MockWebServer server) throws InterruptedException {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"status\":\"ok\"}");
            }
        });
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // Drain requests from the previous test.
        }
    }

    private static void shutdown(MockWebServer server) {
        try {
            server.shutdown();
        } catch (IOException ignored) {
            // Shutdown hook must continue cleaning up remaining test dependencies.
        }
    }
}