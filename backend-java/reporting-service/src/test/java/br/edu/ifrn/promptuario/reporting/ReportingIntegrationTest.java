package br.edu.ifrn.promptuario.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReportingIntegrationTest {

    private static final String JWT_SECRET = "integration-test-signing-secret-must-have-at-least-32-bytes";
    private static final String MINIO_TEST_IMAGE = System.getProperty(
            "minio.test.image", "quay.io/minio/minio:RELEASE.2024-10-13T13-34-11Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("reporting_db")
            .withUsername("reporting")
            .withPassword("reporting_pass");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse(MINIO_TEST_IMAGE))
            .withEnv("MINIO_ROOT_USER", "promptuario")
            .withEnv("MINIO_ROOT_PASSWORD", "promptuario_pass")
            .withCommand("server", "/data", "--console-address", ":9001")
            .withExposedPorts(9000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private S3Client s3Client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("app.storage.s3.endpoint", ReportingIntegrationTest::minioEndpoint);
        registry.add("app.storage.s3.public-endpoint", ReportingIntegrationTest::minioEndpoint);
        registry.add("app.security.jwt.secret", () -> JWT_SECRET);
    }

    @Test
    void exportsCsvPersistsItInMinioAndDownloadsThroughPresignedUrl() throws Exception {
        String bearerToken = jwtFor("admin-integration-test", "ADMIN");
        String body = """
                {"report_type":"CONSULTATIONS","output_format":"CSV", "parameters":
                  {"from_date":"2026-01-01", "to_date":"2026-01-01"}}
                """;

        String creationResponse = mockMvc.perform(post("/api/v1/reports/export")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(creationResponse).path("id").asText();
        assertThat(jobId).isNotBlank();

        JsonNode completedJob = awaitCompletedJob(jobId, bearerToken);
        String s3Key = completedJob.path("s3_key").asText();
        assertThat(s3Key).endsWith(".csv");
        s3Client.headObject(HeadObjectRequest.builder().bucket("reports").key(s3Key).build());

        String location = mockMvc.perform(get("/api/v1/reports/export/{jobId}/download", jobId)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).isNotBlank();

        HttpResponse<String> download = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(HttpRequest.newBuilder(URI.create(location)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).contains("Data");
    }

    private JsonNode awaitCompletedJob(String jobId, String bearerToken) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        JsonNode response = null;
        while (Instant.now().isBefore(deadline)) {
            String content = mockMvc.perform(get("/api/v1/reports/export/{jobId}", jobId)
                            .header("Authorization", "Bearer " + bearerToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            response = objectMapper.readTree(content);
            String reportStatus = response.path("status").asText();
            if ("COMPLETED".equals(reportStatus)) {
                return response;
            }
            assertThat(reportStatus).isNotEqualTo("FAILED");
            Thread.sleep(250);
        }
        throw new AssertionError("Report job did not complete within 30 seconds: " + response);
    }

    private String jwtFor(String subject, String role) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("role", role)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }

    private static String minioEndpoint() {
        return "http://%s:%d".formatted(MINIO.getHost(), MINIO.getMappedPort(9000));
    }
}