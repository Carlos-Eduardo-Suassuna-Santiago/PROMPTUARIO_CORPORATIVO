package br.edu.ifrn.promptuario.modules.clinical.service;

import br.edu.ifrn.promptuario.config.PdfWorkerConfig;
import br.edu.ifrn.promptuario.modules.clinical.model.Prescription;
import br.edu.ifrn.promptuario.modules.clinical.repository.PrescriptionRepository;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientRepository;
import br.edu.ifrn.promptuario.shared.event.PrescriptionGeneratedEvent;
import br.edu.ifrn.promptuario.shared.messaging.EventPublisher;
import br.edu.ifrn.promptuario.shared.storage.S3StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PrescriptionPdfWorker {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionPdfWorker.class);

    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final S3StorageService storageService;
    private final EventPublisher eventPublisher;
    private final String prescriptionsBucket;

    public PrescriptionPdfWorker(ObjectMapper objectMapper,
                                  TemplateEngine templateEngine,
                                  PrescriptionRepository prescriptionRepository,
                                  PatientRepository patientRepository,
                                  S3StorageService storageService,
                                  EventPublisher eventPublisher,
                                  @Value("${app.storage.s3.bucket-prescriptions:prescriptions}") String prescriptionsBucket) {
        this.objectMapper = objectMapper;
        this.templateEngine = templateEngine;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.prescriptionsBucket = prescriptionsBucket;
    }

    @RabbitListener(queues = PdfWorkerConfig.PDF_QUEUE,
            containerFactory = "pdfRabbitListenerContainerFactory")
    @Transactional
    public void generate(Message message) {
        String prescriptionId = extractPrescriptionId(message);
        generatePrescriptionPdf(prescriptionId);
    }

    @Transactional
    public void generatePrescriptionPdf(String prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + prescriptionId));
        Patient patient = patientRepository.findById(prescription.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + prescription.getPatientId()));

        Map<String, Object> model = new HashMap<>();
        model.put("prescription", prescription);
        model.put("patient", patient);
        model.put("medications", prescription.getMedications());
        model.put("issuedAt", LocalDate.now());
        model.put("validUntil", LocalDate.now().plusDays(prescription.getValidDays()));
        model.put("doctorName", prescription.getDoctorId());
        model.put("doctorCrm", prescription.getDoctorId());

        Context context = new Context();
        context.setVariables(model);
        String html = templateEngine.process("prescription-template", context);
        byte[] pdf = renderPdf(html);
        String key = "prescriptions/%s/%s.pdf".formatted(patient.getId(), prescription.getId());

        storageService.uploadFile(prescriptionsBucket, key, pdf, "application/pdf");
        prescription.setPdfS3Key(key);
        prescription.setPdfGeneratedAt(Instant.now());
        prescriptionRepository.save(prescription);

        eventPublisher.publish("domain.events", "prescription.generated",
                new PrescriptionGeneratedEvent(UUID.randomUUID(), Instant.now(), "PrescriptionGenerated",
                        UUID.randomUUID().toString(), prescription.getId(), prescription.getRecord().getId(),
                        patient.getId(), prescription.getDoctorId(), prescription.getMedications(), key));
        log.info("Prescription PDF generated: prescriptionId={}, key={}", prescription.getId(), key);
    }

    private String extractPrescriptionId(Message message) {
        try {
            JsonNode json = objectMapper.readTree(message.getBody());
            String id = json.path("prescriptionId").asText(json.path("prescription_id").asText(null));
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Message does not contain prescriptionId");
            return id;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid PDF generation message", ex);
        }
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Could not render prescription PDF", ex);
        }
    }
}
