package br.edu.ifrn.promptuario.modules.clinical.service;

import br.edu.ifrn.promptuario.config.PdfWorkerConfig;
import br.edu.ifrn.promptuario.modules.clinical.model.MedicalCertificate;
import br.edu.ifrn.promptuario.modules.clinical.repository.MedicalCertificateRepository;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientRepository;
import br.edu.ifrn.promptuario.shared.storage.S3StorageService;
import com.fasterxml.jackson.databind.JsonNode; import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException; import jakarta.transaction.Transactional;
import org.springframework.amqp.core.Message; import org.springframework.amqp.rabbit.annotation.RabbitListener; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine; import org.thymeleaf.context.Context; import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.ByteArrayOutputStream; import java.io.IOException; import java.time.Instant; import java.time.LocalDate; import java.util.HashMap; import java.util.Map;

@Service
public class CertificatePdfWorker {
    private final ObjectMapper objectMapper; private final TemplateEngine templateEngine; private final MedicalCertificateRepository certificates; private final PatientRepository patients; private final S3StorageService storage; private final String bucket;
    public CertificatePdfWorker(ObjectMapper objectMapper,TemplateEngine templateEngine,MedicalCertificateRepository certificates,PatientRepository patients,S3StorageService storage,@Value("${app.storage.s3.bucket-prescriptions:prescriptions}") String bucket){this.objectMapper=objectMapper;this.templateEngine=templateEngine;this.certificates=certificates;this.patients=patients;this.storage=storage;this.bucket=bucket;}
    @RabbitListener(queues=PdfWorkerConfig.CERTIFICATE_PDF_QUEUE,containerFactory="pdfRabbitListenerContainerFactory") @Transactional
    public void generate(Message message){generateCertificatePdf(extractId(message));}
    @Transactional public void generateCertificatePdf(String id){MedicalCertificate certificate=certificates.findById(id).orElseThrow(()->new IllegalArgumentException("Certificate not found: "+id));Patient patient=patients.findById(certificate.getPatientId()).orElseThrow(()->new IllegalArgumentException("Patient not found: "+certificate.getPatientId()));Map<String,Object> model=new HashMap<>();model.put("certificate",certificate);model.put("patient",patient);model.put("issuedAt",LocalDate.now());Context context=new Context();context.setVariables(model);byte[] pdf=renderPdf(templateEngine.process("certificate-template",context));String key="certificates/%s/%s.pdf".formatted(patient.getId(),certificate.getId());storage.uploadFile(bucket,key,pdf,"application/pdf");certificate.setPdfS3Key(key);certificate.setPdfGeneratedAt(Instant.now());certificates.save(certificate);}
    private String extractId(Message message){try{JsonNode json=objectMapper.readTree(message.getBody());String id=json.path("certificateId").asText(json.path("certificate_id").asText(null));if(id==null||id.isBlank())throw new IllegalArgumentException("Message does not contain certificateId");return id;}catch(IOException ex){throw new IllegalArgumentException("Invalid certificate PDF message",ex);}}
    private byte[] renderPdf(String html){try(ByteArrayOutputStream out=new ByteArrayOutputStream()){ITextRenderer renderer=new ITextRenderer();renderer.setDocumentFromString(html);renderer.layout();renderer.createPDF(out);return out.toByteArray();}catch(DocumentException|IOException ex){throw new IllegalStateException("Could not render certificate PDF",ex);}}
}
