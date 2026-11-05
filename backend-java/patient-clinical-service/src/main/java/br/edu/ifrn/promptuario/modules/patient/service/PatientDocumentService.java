package br.edu.ifrn.promptuario.modules.patient.service;

import br.edu.ifrn.promptuario.modules.patient.dto.DocumentResponseDto;
import br.edu.ifrn.promptuario.modules.patient.model.DocumentType;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import br.edu.ifrn.promptuario.modules.patient.model.PatientDocument;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientDocumentRepository;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientRepository;
import br.edu.ifrn.promptuario.shared.storage.S3StorageService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

@Service
public class PatientDocumentService {

    private static final Logger log = LoggerFactory.getLogger(PatientDocumentService.class);
    private static final String DOCUMENTS_BUCKET = "patient-documents";
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final PatientRepository patientRepository;
    private final PatientDocumentRepository documentRepository;
    private final S3StorageService storageService;
    private final String documentsBucket;
    private final long maxFileSize;

    public PatientDocumentService(
            PatientRepository patientRepository,
            PatientDocumentRepository documentRepository,
            S3StorageService storageService,
            @Value("${app.storage.s3.max-file-size-bytes:10485760}") long maxFileSize,
            @Value("${app.storage.s3.bucket-patient-documents:patient-documents}") String documentsBucket) {
        this.patientRepository = patientRepository;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        this.documentsBucket = documentsBucket;
    }

    @Transactional
    public DocumentResponseDto uploadPatientDocument(
            String patientId,
            MultipartFile file,
            String title,
            String category,
            String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Document exceeds the maximum allowed size");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Only PDF, PNG and JPEG documents are supported");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String key = "patients/%s/%s_%s".formatted(patientId, UUID.randomUUID(), originalName);

        try {
            byte[] content = file.getBytes();
            storageService.uploadFile(documentsBucket, key, content, contentType);

            PatientDocument document = new PatientDocument();
            document.setPatient(patient);
            document.setDocumentType(toDocumentType(category));
            document.setFileName(originalName);
            document.setS3Key(key);
            document.setFileSize(file.getSize());
            document.setMimeType(contentType);
            document.setFileHash(sha256(content));
            document.setDescription(buildDescription(title, category));
            document.setUploadedBy(uploadedBy);
            return DocumentResponseDto.fromEntity(documentRepository.save(document));
        } catch (IOException ex) {
            log.error("Could not read patient document: patientId={}, filename={}", patientId, originalName, ex);
            throw new IllegalStateException("Could not read document", ex);
        } catch (RuntimeException ex) {
            log.error("Could not persist patient document: patientId={}, key={}", patientId, key, ex);
            try {
                storageService.deleteFile(documentsBucket, key);
            } catch (RuntimeException cleanupError) {
                log.warn("Could not clean up uploaded object: bucket={}, key={}", DOCUMENTS_BUCKET, key, cleanupError);
            }
            throw ex;
        }
    }

    private static String normalizeContentType(String contentType) {
        return contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType.toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedContentType(String contentType) {
        return MediaType.APPLICATION_PDF_VALUE.equals(contentType)
                || MediaType.IMAGE_PNG_VALUE.equals(contentType)
                || MediaType.IMAGE_JPEG_VALUE.equals(contentType);
    }

    private static String sanitizeFilename(String filename) {
        String value = filename == null ? "document" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.isBlank() ? "document" : value;
    }

    private static DocumentType toDocumentType(String category) {
        if (category == null || category.isBlank()) return DocumentType.OTHER;
        try {
            return DocumentType.valueOf(category.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DocumentType.OTHER;
        }
    }

    private static String buildDescription(String title, String category) {
        if (title == null || title.isBlank()) return category;
        return category == null || category.isBlank() ? title : title + " [" + category + "]";
    }

    private static String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append("%02x".formatted(value));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
