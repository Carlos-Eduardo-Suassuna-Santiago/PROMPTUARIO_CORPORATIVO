package br.edu.ifrn.promptuario.iam;

import br.edu.ifrn.promptuario.iam.messaging.IamEventPublisher;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import br.edu.ifrn.promptuario.iam.security.JwtTokenProvider;
import br.edu.ifrn.promptuario.iam.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("iam_db").withUsername("iam").withPassword("iam_pass");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TokenBlacklistService tokenBlacklistService;
    @MockBean private IamEventPublisher eventPublisher;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test
    void authenticatesSeedAdministratorAndReturnsValidJwt() throws Exception {
        String accessToken = login("admin@promptuario.health", "admin123");
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
    }

    @Test
    void registersPatientAndPersistsIt() throws Exception {
        String email = patientEmail();
        mockMvc.perform(post("/api/v1/auth/register-patient").contentType(MediaType.APPLICATION_JSON)
                        .content(patientPayload(email)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email));
        assertThat(userRepository.findByEmailIgnoreCase(email)).isPresent();
    }

    @Test
    void logoutAddsAccessTokenToRedisBlacklist() throws Exception {
        String email = patientEmail();
        register(email);
        String accessToken = login(email, "SenhaSegura123");
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"refresh_token\":\"ignored\"}"))
                .andExpect(status().isNoContent());
        assertThat(tokenBlacklistService.isBlacklisted(accessToken)).isTrue();
    }

    @Test
    void rejectsProtectedEndpointAfterLogout() throws Exception {
        String email = patientEmail();
        register(email);
        String token = login(email, "SenhaSegura123");
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"refresh_token\":\"ignored\"}"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUserCannotLoginAgain() throws Exception {
        String email = patientEmail();
        register(email);
        String patientId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        String adminToken = login("admin@promptuario.health", "admin123");
        mockMvc.perform(delete("/api/v1/users/{id}", patientId).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Solicitação administrativa\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isForbidden());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register-patient").contentType(MediaType.APPLICATION_JSON).content(patientPayload(email)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
                .replaceAll(".*\\\"access_token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String patientEmail() { return "patient-" + UUID.randomUUID() + "@promptuario.health"; }
    private String patientPayload(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"SenhaSegura123\",\"full_name\":\"Paciente de Teste\"}";
    }
}
