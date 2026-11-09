package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.DocumentType;
import java.time.Instant;

public record DocumentUploadResponseDto(String id, String patientId, DocumentType documentType, String fileName,
                                        String s3Key, long fileSize, String mimeType, String fileHash,
                                        String description, String uploadedBy, boolean active, Instant createdAt) {
}
