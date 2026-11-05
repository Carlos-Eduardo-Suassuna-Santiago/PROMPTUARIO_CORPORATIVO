package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.DocumentType;
import br.edu.ifrn.promptuario.modules.patient.model.PatientDocument;
import java.time.Instant;

public record DocumentResponseDto(String id, String patientId, DocumentType documentType, String fileName,
                                  long fileSize, String mimeType, String fileHash, String description,
                                  String uploadedBy, boolean active, Instant createdAt, Instant updatedAt,
                                  Instant deletedAt) {
    public static DocumentResponseDto fromEntity(PatientDocument e) {
        return new DocumentResponseDto(e.getId(), e.getPatient().getId(), e.getDocumentType(), e.getFileName(),
                e.getFileSize(), e.getMimeType(), e.getFileHash(), e.getDescription(), e.getUploadedBy(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getDeletedAt());
    }
}
