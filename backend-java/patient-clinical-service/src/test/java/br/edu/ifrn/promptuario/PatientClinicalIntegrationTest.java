package br.edu.ifrn.promptuario;

import br.edu.ifrn.promptuario.config.PdfWorkerConfig;
import br.edu.ifrn.promptuario.modules.clinical.model.Prescription;
import br.edu.ifrn.promptuario.modules.clinical.repository.PrescriptionRepository;
import br.edu.ifrn.promptuario.shared.event.MedicalRecordCreatedEvent;
import br.edu.ifrn.promptuario.shared.messaging.EventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.edu.ifrn.promptuario.config.RabbitMQConfig.DOMAIN_EVENTS_EXCHANGE;
import static br.edu.ifrn.promptuario.config.RabbitMQConfig.LEGACY_CLINICAL_EXCHANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PatientClinicalIntegrationTest {

    private static final String RABBIT_USER = "promptuario";
    private static final String RABBIT_PASSWORD = "promptuario_pass";
    private static final String JWT_SECRET = "integration-test-secret-key-with-at-least-32-bytes";
    private static final String PRESCRIPTIONS_BUCKET = "prescriptions";
    private static final String DOCTOR_ID = UUID.randomUUID().toString();
    private static final String ADMIN_ID = UUID.randomUUID().toString();
    private static final String CPF = "12345678901";
    private static final Instant APPOINTMENT_TIME = Instant.now().plus(7, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.SECONDS);

    private static final String RECORD_CANONICAL_QUEUE = "test.record.canonical." + UUID.randomUUID();
    private static final String RECORD_LEGACY_QUEUE = "test.record.legacy." + UUID.randomUUID();
    private static final String PRESCRIPTION_CANONICAL_QUEUE = "test.prescription.canonical." + UUID.randomUUID();
    private static final String PRESCRIPTION_LEGACY_QUEUE = "test.prescription.legacy." + UUID.randomUUID();

    private static final Network NETWORK = Network.newNetwork();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("patient_clinical_db")
            .withUsername("clinical")
            .withPassword("clinical_pass")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withAdminUser(RABBIT_USER)
            .withAdminPassword(RABBIT_PASSWORD)
            .withNetwork(NETWORK)
            .withNetworkAliases("rabbitmq");

    @Container
    static final LocalStackContainer S3 = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4.0"))
            .withServices(LocalStackContainer.Service.S3)
            .withNetwork(NETWORK)
            .withNetworkAliases("localstack");

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:7.0"))
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "ai")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "ai_pass")
            .withNetwork(NETWORK)
            .withNetworkAliases("mongo")
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(NETWORK)
            .withNetworkAliases("redis")
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin rabbitAdmin;

    @Autowired
    PrescriptionRepository prescriptionRepository;

    @Autowired
    S3Client s3Client;

    private static GenericContainer<?> aiPython;
    private static String patientId;
    private static String appointmentId;
    private static String recordId;
    private static String prescriptionId;
    private static Message legacyRecordEvent;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> RABBIT_USER);
        registry.add("spring.rabbitmq.password", () -> RABBIT_PASSWORD);
        registry.add("app.messaging.legacy-publish-enabled", () -> "true");

        registry.add("app.storage.s3.endpoint",
                () -> S3.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("app.storage.s3.access-key", S3::getAccessKey);
        registry.add("app.storage.s3.secret-key", S3::getSecretKey);
        registry.add("app.storage.s3.region", S3::getRegion);
        registry.add("app.storage.s3.bucket-prescriptions", () -> PRESCRIPTIONS_BUCKET);
        registry.add("app.security.jwt.secret", () -> JWT_SECRET);
    }

    @BeforeAll
    static void startPythonAi() {
        Path repositoryRoot = locateRepositoryRoot();
        Assumptions.assumeTrue(
                Files.isRegularFile(repositoryRoot.resolve("backend/ai-service/Dockerfile")),
                "Teste E2E ignorado: fontes do AI Python não foram encontrados no checkout");

        ImageFromDockerfile image = new ImageFromDockerfile("promptuario-ai-e2e:latest", false)
                .withFileFromPath("Dockerfile", repositoryRoot.resolve("backend/ai-service/Dockerfile"))
                .withFileFromPath("ai-service", repositoryRoot.resolve("backend/ai-service"))
                .withFileFromPath("shared", repositoryRoot.resolve("backend/shared"));

        aiPython = new GenericContainer<>(image)
                .withNetwork(NETWORK)
                .withNetworkAliases("ai-python")
                .withExposedPorts(8000)
                .withEnv("MONGODB_URL", "mongodb://ai:ai_pass@mongo:27017/ai_db?authSource=admin")
                .withEnv("REDIS_URL", "redis://redis:6379/2")
                .withEnv("RABBITMQ_URL", "amqp://" + RABBIT_USER + ":" + RABBIT_PASSWORD + "@rabbitmq:5672/")
                .withEnv("JWT_SECRET_KEY", JWT_SECRET)
                .withEnv("LLM_API_KEY", "")
                .withEnv("OTEL_SDK_DISABLED", "true")
                .waitingFor(Wait.forHttp("/healthz")
                        .forPort(8000)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)));
        aiPython.start();
    }

    @BeforeEach
    void declareCaptureQueues() {
        declareCaptureQueue(RECORD_CANONICAL_QUEUE, DOMAIN_EVENTS_EXCHANGE, "medical_record.created");
        declareCaptureQueue(RECORD_LEGACY_QUEUE, LEGACY_CLINICAL_EXCHANGE, "clinical.medical_record.created");
        declareCaptureQueue(PRESCRIPTION_CANONICAL_QUEUE, DOMAIN_EVENTS_EXCHANGE, "prescription.generated");
        declareCaptureQueue(PRESCRIPTION_LEGACY_QUEUE, LEGACY_CLINICAL_EXCHANGE, "clinical.prescription.generated");
    }

    @AfterAll
    static void stopPythonAi() {
        if (aiPython != null) {
            aiPython.stop();
        }
        NETWORK.close();
    }

    @Test
    @Order(1)
    void completesClinicalFlowWithDualEventsAiAndPdf() throws Exception {
        ResponseEntity<JsonNode> health = http.getForEntity("/healthz", JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).isNotNull();
        assertThat(health.getBody().path("status").asText()).isEqualTo("ok");

        ResponseEntity<JsonNode> patient = post("/api/v1/patients", Map.of(
                "fullName", "Maria da Silva",
                "cpf", CPF,
                "dateOfBirth", "1985-04-12",
                "gender", "F",
                "email", "maria.integration@example.com",
                "userId", UUID.randomUUID().toString()), token(ADMIN_ID, "ADMIN"));
        assertCreated(patient);
        patientId = patient.getBody().path("id").asText();

        ResponseEntity<JsonNode> appointment = post("/api/v1/appointments", Map.of(
                "patientId", patientId,
                "doctorId", DOCTOR_ID,
                "scheduledAt", APPOINTMENT_TIME.toString(),
                "appointmentType", "CONSULTATION",
                "specialty", "Clínica médica"), token(DOCTOR_ID, "DOCTOR"));
        assertCreated(appointment);
        appointmentId = appointment.getBody().path("id").asText();

        ResponseEntity<JsonNode> record = post("/api/v1/records", Map.of(
                "appointmentId", appointmentId,
                "patientId", patientId,
                "chiefComplaint", "Dor de cabeça persistente",
                "anamnesis", "Sintomas há três dias",
                "physicalExam", "Sem sinais de alarme",
                "diagnosis", "Cefaleia a esclarecer",
                "diagnosisCodes", new String[]{"R51.9"},
                "treatmentPlan", "Analgesia e acompanhamento"), token(DOCTOR_ID, "DOCTOR"));
        assertCreated(record);
        recordId = record.getBody().path("id").asText();

        Message canonicalRecord = receiveRequired(RECORD_CANONICAL_QUEUE, Duration.ofSeconds(15));
        legacyRecordEvent = receiveRequired(RECORD_LEGACY_QUEUE, Duration.ofSeconds(15));
        assertEquivalentDualPublication(canonicalRecord, legacyRecordEvent, "MedicalRecordCreated");
        assertThat(rabbitTemplate.receive(RECORD_CANONICAL_QUEUE, 500)).isNull();
        assertThat(rabbitTemplate.receive(RECORD_LEGACY_QUEUE, 500)).isNull();

        awaitCondition("AI Python consumir o prontuário", Duration.ofSeconds(30),
                () -> mongoJobCount(recordId, "SYMPTOM_ANALYSIS") == 1);
        publishDuplicate(legacyRecordEvent, "clinical.medical_record.created");
        publishDuplicate(legacyRecordEvent, "clinical.medical_record.created");
        Thread.sleep(1500);
        assertThat(mongoJobCount(recordId, "SYMPTOM_ANALYSIS"))
                .as("o mesmo eventId deve ser processado somente uma vez")
                .isEqualTo(1);

        ResponseEntity<JsonNode> prescription = createPrescription();
        assertCreated(prescription);
        prescriptionId = prescription.getBody().path("id").asText();

        awaitCondition("worker gerar e persistir o PDF", Duration.ofSeconds(30), () ->
                prescriptionRepository.findById(prescriptionId)
                        .map(value -> value.getPdfS3Key() != null && value.getPdfGeneratedAt() != null)
                        .orElse(false));

        Prescription generated = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertThat(generated.getPdfS3Key()).isEqualTo("prescriptions/" + patientId + "/" + prescriptionId + ".pdf");
        assertThat(generated.getPdfGeneratedAt()).isNotNull();
        assertThat(s3Client.headObject(HeadObjectRequest.builder()
                .bucket(PRESCRIPTIONS_BUCKET)
                .key(generated.getPdfS3Key())
                .build()).contentLength()).isPositive();

        Message canonicalPrescription = receiveRequired(PRESCRIPTION_CANONICAL_QUEUE, Duration.ofSeconds(15));
        Message legacyPrescription = receiveRequired(PRESCRIPTION_LEGACY_QUEUE, Duration.ofSeconds(15));
        assertEquivalentDualPublication(canonicalPrescription, legacyPrescription, "PrescriptionGenerated");
        assertThat(rabbitTemplate.receive(PRESCRIPTION_CANONICAL_QUEUE, 500)).isNull();
        assertThat(rabbitTemplate.receive(PRESCRIPTION_LEGACY_QUEUE, 500)).isNull();

        awaitCondition("AI Python consumir a prescrição", Duration.ofSeconds(30),
                () -> mongoJobCount(recordId, "DRUG_INTERACTION_CHECK") == 1);
    }

    @Test
    @Order(2)
    void rejectsDuplicateCpf() {
        ResponseEntity<JsonNode> response = post("/api/v1/patients", Map.of(
                "fullName", "Outra Pessoa",
                "cpf", CPF,
                "userId", UUID.randomUUID().toString()), token(ADMIN_ID, "ADMIN"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(3)
    void rejectsAppointmentForMissingPatient() {
        ResponseEntity<JsonNode> response = post("/api/v1/appointments", Map.of(
                "patientId", UUID.randomUUID().toString(),
                "doctorId", UUID.randomUUID().toString(),
                "scheduledAt", APPOINTMENT_TIME.plus(1, ChronoUnit.DAYS).toString()),
                token(DOCTOR_ID, "DOCTOR"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(4)
    void rejectsScheduleConflict() {
        ResponseEntity<JsonNode> response = post("/api/v1/appointments", Map.of(
                "patientId", patientId,
                "doctorId", DOCTOR_ID,
                "scheduledAt", APPOINTMENT_TIME.toString()), token(DOCTOR_ID, "DOCTOR"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("message").asText()).contains("Horário indisponível");
    }

    @Test
    @Order(5)
    void sendsWorkerFailureToDlqWhenBucketIsMissing() throws Exception {
        rabbitAdmin.purgeQueue(PdfWorkerConfig.PDF_DLQ, true);
        emptyAndDeletePrescriptionsBucket();
        try {
            ResponseEntity<JsonNode> response = createPrescription();
            assertCreated(response);
            String failedPrescriptionId = response.getBody().path("id").asText();

            Message deadLetter = receiveRequired(PdfWorkerConfig.PDF_DLQ, Duration.ofSeconds(20));
            JsonNode body = objectMapper.readTree(deadLetter.getBody());
            assertThat(body.path("prescriptionId").asText()).isEqualTo(failedPrescriptionId);

            Prescription failed = prescriptionRepository.findById(failedPrescriptionId).orElseThrow();
            assertThat(failed.getPdfS3Key()).isNull();
            assertThat(failed.getPdfGeneratedAt()).isNull();
        } finally {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(PRESCRIPTIONS_BUCKET).build());
        }
    }

    @Test
    @Order(6)
    void rejectsInvalidHttpAndRabbitPayloads() {
        ResponseEntity<JsonNode> invalidHttp = post("/api/v1/patients", Map.of(), token(ADMIN_ID, "ADMIN"));
        assertThat(invalidHttp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        rabbitAdmin.purgeQueue(PdfWorkerConfig.PDF_DLQ, true);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MediaType.APPLICATION_JSON_VALUE);
        rabbitTemplate.send(DOMAIN_EVENTS_EXCHANGE, PdfWorkerConfig.PDF_ROUTING_KEY,
                new Message("{invalid-json".getBytes(StandardCharsets.UTF_8), properties));

        Message deadLetter = receiveRequired(PdfWorkerConfig.PDF_DLQ, Duration.ofSeconds(20));
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).isEqualTo("{invalid-json");
    }

    @Test
    @Order(7)
    void retriesAndReportsRabbitMqUnavailable() throws Exception {
        int unavailablePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unavailablePort = socket.getLocalPort();
        }

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("127.0.0.1", unavailablePort);
        connectionFactory.setConnectionTimeout(250);
        RabbitTemplate unavailableRabbit = new RabbitTemplate(connectionFactory);
        unavailableRabbit.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        RetryTemplate retry = RetryTemplate.builder()
                .maxAttempts(2)
                .fixedBackoff(10)
                .retryOn(AmqpException.class)
                .build();
        EventPublisher publisher = new EventPublisher(unavailableRabbit, retry, false);
        MedicalRecordCreatedEvent event = new MedicalRecordCreatedEvent(
                UUID.randomUUID(), Instant.now(), "MedicalRecordCreated", UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                DOCTOR_ID, "Teste de indisponibilidade", java.util.List.of(), 0, 0);

        try {
            assertThatThrownBy(() -> publisher.publish(DOMAIN_EVENTS_EXCHANGE, "medical_record.created", event))
                    .isInstanceOf(AmqpException.class);
        } finally {
            connectionFactory.destroy();
        }
    }

    private ResponseEntity<JsonNode> createPrescription() {
        Map<String, Object> medication = new LinkedHashMap<>();
        medication.put("name", "Dipirona");
        medication.put("dosage", "500 mg");
        medication.put("frequency", "A cada 8 horas");
        medication.put("durationDays", 3);
        medication.put("instructions", "Tomar após alimentação");
        return post("/api/v1/records/" + recordId + "/prescriptions", Map.of(
                "medications", new Object[]{medication},
                "instructions", "Suspender em caso de reação adversa",
                "validDays", 30), token(DOCTOR_ID, "DOCTOR"));
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private void assertCreated(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode())
                .withFailMessage("Esperado HTTP 201, recebido %s com corpo %s", response.getStatusCode(), response.getBody())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("id").asText()).isNotBlank();
    }

    private void declareCaptureQueue(String queueName, String exchange, String routingKey) {
        rabbitAdmin.declareQueue(new Queue(queueName, false, false, false));
        rabbitAdmin.declareBinding(new Binding(
                queueName, Binding.DestinationType.QUEUE, exchange, routingKey, null));
    }

    private Message receiveRequired(String queue, Duration timeout) {
        Message message = rabbitTemplate.receive(queue, timeout.toMillis());
        assertThat(message)
                .withFailMessage("Nenhuma mensagem recebida em %s dentro de %s", queue, timeout)
                .isNotNull();
        return message;
    }

    private void assertEquivalentDualPublication(Message canonical, Message legacy, String eventType) throws Exception {
        JsonNode canonicalJson = objectMapper.readTree(canonical.getBody());
        JsonNode legacyJson = objectMapper.readTree(legacy.getBody());
        assertThat(legacyJson).isEqualTo(canonicalJson);

        String eventId = canonicalJson.path("eventId").asText();
        String timestamp = canonicalJson.path("timestamp").asText();
        String correlationId = canonicalJson.path("correlationId").asText();
        assertThat(eventId).isNotBlank();
        assertThat(timestamp).isNotBlank();
        assertThat(correlationId).isNotBlank();
        assertThat(canonicalJson.path("eventType").asText()).isEqualTo(eventType);

        assertHeaders(canonical, eventId, eventType, timestamp, correlationId);
        assertHeaders(legacy, eventId, eventType, timestamp, correlationId);
    }

    private void assertHeaders(Message message, String eventId, String eventType,
                               String timestamp, String correlationId) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        assertThat(headers.get("eventId")).hasToString(eventId);
        assertThat(headers.get("eventType")).hasToString(eventType);
        assertThat(headers.get("timestamp")).hasToString(timestamp);
        assertThat(headers.get("correlationId")).hasToString(correlationId);
    }

    private void publishDuplicate(Message original, String routingKey) {
        Message duplicate = MessageBuilder.fromClonedMessage(original).build();
        rabbitTemplate.send(LEGACY_CLINICAL_EXCHANGE, routingKey, duplicate);
    }

    private long mongoJobCount(String targetRecordId, String analysisType) throws Exception {
        String script = "db.analysis_jobs.countDocuments({record_id: '" + targetRecordId
                + "', analysis_type: '" + analysisType + "'})";
        org.testcontainers.containers.Container.ExecResult result = MONGO.execInContainer(
                "mongosh", "--quiet", "--username", "ai", "--password", "ai_pass",
                "--authenticationDatabase", "admin", "ai_db", "--eval", script);
        assertThat(result.getExitCode())
                .withFailMessage("mongosh falhou: %s", result.getStderr())
                .isZero();
        Matcher matcher = Pattern.compile("(?m)^\\s*(\\d+)\\s*$").matcher(result.getStdout());
        long count = -1;
        while (matcher.find()) {
            count = Long.parseLong(matcher.group(1));
        }
        assertThat(count).withFailMessage("Contagem MongoDB inválida: %s", result.getStdout()).isNotNegative();
        return count;
    }

    private void emptyAndDeletePrescriptionsBucket() {
        s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(PRESCRIPTIONS_BUCKET).build())
                .contents()
                .forEach(object -> s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(PRESCRIPTIONS_BUCKET)
                        .key(object.key())
                        .build()));
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(PRESCRIPTIONS_BUCKET).build());
    }

    private void awaitCondition(String description, Duration timeout, CheckedCondition condition) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        Throwable lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                if (condition.evaluate()) {
                    return;
                }
            } catch (Throwable failure) {
                lastFailure = failure;
            }
            Thread.sleep(250);
        }
        AssertionError timeoutFailure = new AssertionError("Tempo excedido aguardando: " + description);
        if (lastFailure != null) {
            timeoutFailure.initCause(lastFailure);
        }
        throw timeoutFailure;
    }

    private static Path locateRepositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("backend"))
                    && Files.isDirectory(candidate.resolve("backend-java"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private String token(String subject, String role) {
        try {
            String header = base64Url(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            Instant now = Instant.now();
            String payload = base64Url(objectMapper.writeValueAsBytes(Map.of(
                    "sub", subject,
                    "role", role,
                    "iat", now.getEpochSecond(),
                    "exp", now.plus(1, ChronoUnit.HOURS).getEpochSecond())));
            String unsigned = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return unsigned + "." + base64Url(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível criar o JWT de teste", exception);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean evaluate() throws Exception;
    }
}
