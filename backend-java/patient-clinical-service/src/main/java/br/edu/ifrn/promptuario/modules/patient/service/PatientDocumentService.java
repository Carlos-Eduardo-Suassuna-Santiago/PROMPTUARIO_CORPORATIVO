package br.edu.ifrn.promptuario.modules.patient.service;

import br.edu.ifrn.promptuario.modules.patient.dto.DocumentDto;
import br.edu.ifrn.promptuario.modules.patient.dto.DocumentUploadResponseDto;
import br.edu.ifrn.promptuario.modules.patient.model.PatientDocument;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientDocumentRepository;
import br.edu.ifrn.promptuario.shared.audit.AuditService;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PatientDocumentService {
    private final PatientDocumentRepository repository; private final PatientService patients; private final AuditService audit;
    public PatientDocumentService(PatientDocumentRepository repository, PatientService patients, AuditService audit) { this.repository = repository; this.patients = patients; this.audit = audit; }
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResponseDto addDocument(String patientId, DocumentDto dto, String s3Key, String uploadedBy) {
        PatientDocument d = new PatientDocument(); d.setPatient(patients.getPatientById(patientId)); d.setDocumentType(dto.documentType()); d.setFileName(dto.fileName()); d.setFileSize(dto.fileSize()); d.setMimeType(dto.mimeType()); d.setFileHash(dto.fileHash()); d.setDescription(dto.description()); d.setS3Key(s3Key); d.setUploadedBy(uploadedBy);
        PatientDocument saved = repository.save(d); audit.record("patient_documents", "INSERT", saved.getId(), uploadedBy, null, Map.of("patientId",patientId,"s3Key",s3Key)); return response(saved);
    }
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResponseDto uploadDocument(String patientId, DocumentDto dto, String s3Key, String uploadedBy) {
        return addDocument(patientId, dto, s3Key, uploadedBy);
    }
    @Transactional(readOnly = true)
    public List<PatientDocument> listDocuments(String patientId) { patients.getPatientById(patientId); return repository.findByPatient_IdAndActiveTrueOrderByCreatedAtDesc(patientId); }
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String patientId, String documentId, String userId) { PatientDocument d = repository.findById(documentId).filter(x -> patientId.equals(x.getPatient().getId())).orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado")); d.setActive(false); d.setDeletedAt(Instant.now()); repository.save(d); audit.record("patient_documents", "DELETE", documentId, userId, Map.of("active",true), Map.of("active",false)); }
    private DocumentUploadResponseDto response(PatientDocument d) { return new DocumentUploadResponseDto(d.getId(),d.getPatient().getId(),d.getDocumentType(),d.getFileName(),d.getS3Key(),d.getFileSize(),d.getMimeType(),d.getFileHash(),d.getDescription(),d.getUploadedBy(),d.isActive(),d.getCreatedAt()); }
}
